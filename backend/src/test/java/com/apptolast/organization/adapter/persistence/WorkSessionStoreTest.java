package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.support.TestDatabase;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WorkSessionStoreTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;
  static ObjectMapper json;
  UUID project;
  UUID task;
  UUID key;
  PostgresWorkSessionStore store;
  static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-06T10:00:00.123456789Z"), ZoneOffset.UTC);

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
    json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @BeforeEach
  void context() {
    TestDatabase.empty(jdbc);
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    key = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    store = new PostgresWorkSessionStore(jdbc, manager, json);
  }

  StartWorkSession start() {
    return new StartWorkSession(store, CLOCK, () -> Set.of("UTC"));
  }

  @Test
  void s14_replaysOriginalBeforeCurrentBusinessWithoutAvailability() {
    var original = start().start("owner", project, task, key, 25);
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    jdbc.update(
        "UPDATE tasks SET status='completed',completed_at=now(),updated_at=now() WHERE id=?", task);
    var clock = mock(Clock.class);
    var catalog = mock(ZoneCatalog.class);
    var replay = new StartWorkSession(store, clock, catalog).start("owner", project, task, key, 25);
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.session()).isEqualTo(original.session());
    verifyNoInteractions(clock, catalog);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
        .isZero();
  }

  @Test
  void s15_keyCannotChangeDuration() {
    start().start("owner", project, task, key, 25);
    assertThatThrownBy(() -> start().start("owner", project, task, key, 30))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s15_keyCannotChangeTask() {
    start().start("owner", project, task, key, 25);
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        other,
        project);
    assertThatThrownBy(() -> start().start("owner", project, other, key, 25))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s15_keyIsOwnerScopedAcrossProjects() {
    start().start("owner", project, task, key, 25);
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Other','','active',now(),now())",
        otherProject);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        otherProject);
    assertThatThrownBy(() -> start().start("owner", otherProject, otherTask, key, 25))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s12_activeSessionPrecedesCompletedProjectForNewKey() {
    var existing = start().start("owner", project, task, key, 25).session();
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    assertThatThrownBy(() -> start().start("owner", project, task, UUID.randomUUID(), 25))
        .isInstanceOfSatisfying(
            WorkSessionAlreadyActiveException.class,
            error -> assertThat(error.sessionId()).isEqualTo(existing.id()));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s17_sameKeyRaceReturnsOneCreationAndOneReplay() throws Exception {
    var results = race(project, task, key, 25);
    assertThat(results).allMatch(WorkSessionConfirmation.class::isInstance);
    var a = (WorkSessionConfirmation) results.get(0);
    var b = (WorkSessionConfirmation) results.get(1);
    assertThat(List.of(a.replayed(), b.replayed())).containsExactlyInAnyOrder(false, true);
    assertThat(a.session()).isEqualTo(b.session());
    assertCounts(1);
  }

  private void assertCounts(int expected) {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(expected);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(expected);
  }

  private List<Object> race(UUID secondProject, UUID secondTask, UUID secondKey, int secondMinutes)
      throws Exception {
    return race("owner", secondProject, secondTask, secondKey, secondMinutes);
  }

  private List<Object> race(
      String secondOwner, UUID secondProject, UUID secondTask, UUID secondKey, int secondMinutes)
      throws Exception {
    var barrier = new java.util.concurrent.CyclicBarrier(2);
    var attempts = new java.util.concurrent.atomic.AtomicInteger();
    var concurrentJdbc =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public int update(String sql, Object... args) {
            if (sql.startsWith("INSERT INTO work_sessions")) {
              attempts.incrementAndGet();
              try {
                barrier.await(10, java.util.concurrent.TimeUnit.SECONDS);
              } catch (Exception error) {
                throw new AssertionError(error);
              }
            }
            return super.update(sql, args);
          }
        };
    var command =
        new StartWorkSession(
            new PostgresWorkSessionStore(concurrentJdbc, manager, json),
            CLOCK,
            () -> Set.of("UTC"));
    try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first = workers.submit(() -> attempt(command, "owner", project, task, key, 25));
      var second =
          workers.submit(
              () ->
                  attempt(
                      command, secondOwner, secondProject, secondTask, secondKey, secondMinutes));
      var results =
          List.of(
              first.get(15, java.util.concurrent.TimeUnit.SECONDS),
              second.get(15, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(attempts.get()).isEqualTo(2);
      return results;
    }
  }

  private Object attempt(
      StartWorkSession command, String owner, UUID p, UUID t, UUID k, int minutes) {
    try {
      return command.start(owner, p, t, k, minutes);
    } catch (RuntimeException error) {
      return error;
    }
  }

  @Test
  void s17_sameKeyDifferentDurationRaceReturnsConflict() throws Exception {
    var results = race(project, task, key, 30);
    assertThat(results.stream().filter(WorkSessionConfirmation.class::isInstance)).hasSize(1);
    assertThat(results.stream().filter(WorkSessionIdempotencyConflictException.class::isInstance))
        .hasSize(1);
    assertCounts(1);
  }

  @Test
  void s17_distinctProjectsRaceAllowsOnlyOneActiveSession() throws Exception {
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Other','','active',now(),now())",
        otherProject);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        otherProject);
    var results = race(otherProject, otherTask, UUID.randomUUID(), 25);
    assertThat(results.stream().filter(WorkSessionConfirmation.class::isInstance)).hasSize(1);
    assertThat(results.stream().filter(WorkSessionAlreadyActiveException.class::isInstance))
        .hasSize(1);
    var winner =
        (WorkSessionConfirmation)
            results.stream()
                .filter(WorkSessionConfirmation.class::isInstance)
                .findFirst()
                .orElseThrow();
    var conflict =
        (WorkSessionAlreadyActiveException)
            results.stream()
                .filter(WorkSessionAlreadyActiveException.class::isInstance)
                .findFirst()
                .orElseThrow();
    assertThat(conflict.sessionId()).isEqualTo(winner.session().id());
    assertCounts(1);
  }

  @Test
  void s23_readsActiveFromOwnDurableSession() {
    var expected = start().start("owner", project, task, key, 25).session();
    assertThat(store.active("owner")).contains(expected);
    assertCounts(1);
  }

  @Test
  void s21_readsIdentityAfterCompletedContextWithoutOutbox() {
    var expected = start().start("owner", project, task, key, 25).session();
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    jdbc.update("DELETE FROM outbox_events");
    assertThat(store.detail("owner", expected.id())).contains(expected);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s21_readsRequestAfterCompletedContextWithoutOutbox() {
    var expected = start().start("owner", project, task, key, 25).session();
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    jdbc.update("DELETE FROM outbox_events");
    assertThat(store.byRequest("owner", key)).contains(expected);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s24_activeReadUsesReadOnlyTransactionWithoutLocks() {
    var expected = start().start("owner", project, task, key, 25).session();
    var observed = new java.util.concurrent.atomic.AtomicBoolean();
    var reader =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
            assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                .isEqualTo("on");
            assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                .isEqualTo("read committed");
            assertThat(sql).doesNotContain("FOR SHARE", "FOR UPDATE");
            observed.set(true);
            return super.query(sql, mapper, args);
          }
        };
    jdbc.execute(
        "ALTER DATABASE "
            + postgres.getDatabaseName()
            + " SET default_transaction_isolation TO 'repeatable read'");
    try {
      assertThat(new PostgresWorkSessionStore(reader, manager, json).active("owner"))
          .contains(expected);
    } finally {
      jdbc.execute(
          "ALTER DATABASE " + postgres.getDatabaseName() + " RESET default_transaction_isolation");
    }
    assertThat(observed).isTrue();
    assertCounts(1);
  }

  @Test
  void s24_activeReadFailureAtTransactionEndIsStorageUnavailable() {
    var failing = failingReadEnd();
    assertThatThrownBy(() -> new PostgresWorkSessionStore(jdbc, failing, json).active("owner"))
        .isInstanceOf(StorageUnavailableException.class);
    assertCounts(0);
  }

  private org.springframework.transaction.PlatformTransactionManager failingReadEnd() {
    return new org.springframework.transaction.PlatformTransactionManager() {
      public org.springframework.transaction.TransactionStatus getTransaction(
          org.springframework.transaction.TransactionDefinition definition) {
        return manager.getTransaction(definition);
      }

      public void commit(org.springframework.transaction.TransactionStatus status) {
        manager.commit(status);
        throw new org.springframework.transaction.TransactionSystemException(
            "read completion failed");
      }

      public void rollback(org.springframework.transaction.TransactionStatus status) {
        manager.rollback(status);
      }
    };
  }

  @Test
  void s24_identityReadFailureAtTransactionEndIsStorageUnavailable() {
    assertThatThrownBy(
            () ->
                new PostgresWorkSessionStore(jdbc, failingReadEnd(), json)
                    .detail("owner", UUID.randomUUID()))
        .isInstanceOf(StorageUnavailableException.class);
    assertCounts(0);
  }

  @Test
  void s24_requestReadFailureAtTransactionEndIsStorageUnavailable() {
    assertThatThrownBy(
            () ->
                new PostgresWorkSessionStore(jdbc, failingReadEnd(), json).byRequest("owner", key))
        .isInstanceOf(StorageUnavailableException.class);
    assertCounts(0);
  }

  @Test
  void s24_activeSqlFailureIsNotAbsence() {
    withoutSessionTable(
        () ->
            assertThatThrownBy(() -> store.active("owner"))
                .isInstanceOf(StorageUnavailableException.class));
  }

  private void withoutSessionTable(Runnable assertion) {
    jdbc.execute("ALTER TABLE work_sessions RENAME TO unavailable_sessions");
    try {
      assertion.run();
    } finally {
      jdbc.execute("ALTER TABLE unavailable_sessions RENAME TO work_sessions");
    }
    assertCounts(0);
  }

  @Test
  void s24_identitySqlFailureIsNotAbsence() {
    withoutSessionTable(
        () ->
            assertThatThrownBy(() -> store.detail("owner", UUID.randomUUID()))
                .isInstanceOf(StorageUnavailableException.class));
  }

  @Test
  void s24_requestSqlFailureIsNotAbsence() {
    withoutSessionTable(
        () ->
            assertThatThrownBy(() -> store.byRequest("owner", key))
                .isInstanceOf(StorageUnavailableException.class));
  }

  @Test
  void s20_commandUsesReadCommittedAgainstDifferentDatabaseDefault() {
    WorkSessionStarting observed =
        (owner, p, t, k, minutes, operation) ->
            store.commit(
                owner,
                p,
                t,
                k,
                minutes,
                context -> {
                  assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                      .isEqualTo("read committed");
                  assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                      .isEqualTo("off");
                  return operation.apply(context);
                });
    jdbc.execute(
        "ALTER DATABASE "
            + postgres.getDatabaseName()
            + " SET default_transaction_isolation TO 'repeatable read'");
    try {
      new StartWorkSession(observed, CLOCK, () -> Set.of("UTC"))
          .start("owner", project, task, key, 25);
    } finally {
      jdbc.execute(
          "ALTER DATABASE " + postgres.getDatabaseName() + " RESET default_transaction_isolation");
    }
    assertCounts(1);
  }

  @Test
  void s20_failedAvailabilityQueryIsStorageUnavailableNotFallback() {
    jdbc.execute("ALTER TABLE availability_preferences RENAME TO unavailable_preference");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_preference RENAME TO availability_preferences");
    }
    assertCounts(0);
  }

  @Test
  void s20_suppressedSessionInsertRollsBackWithoutInventedConflict() {
    jdbc.execute(
        "CREATE FUNCTION suppress_session_start() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_session_start BEFORE INSERT ON work_sessions FOR EACH ROW EXECUTE FUNCTION suppress_session_start()");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER suppress_session_start ON work_sessions");
      jdbc.execute("DROP FUNCTION suppress_session_start()");
    }
    assertCounts(0);
  }

  @Test
  void s20_suppressedOutboxInsertRollsBackSession() {
    jdbc.execute(
        "CREATE FUNCTION suppress_start_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_start_event BEFORE INSERT ON outbox_events FOR EACH ROW EXECUTE FUNCTION suppress_start_event()");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER suppress_start_event ON outbox_events");
      jdbc.execute("DROP FUNCTION suppress_start_event()");
    }
    assertCounts(0);
  }

  @Test
  void s20_outboxWriteFailureRollsBackSession() {
    jdbc.execute(
        "CREATE FUNCTION reject_start_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced event failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_start_event BEFORE INSERT ON outbox_events FOR EACH ROW EXECUTE FUNCTION reject_start_event()");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER reject_start_event ON outbox_events");
      jdbc.execute("DROP FUNCTION reject_start_event()");
    }
    assertCounts(0);
  }

  @Test
  void s20_commitFailureRollsBackSessionAndEvent() {
    jdbc.execute(
        "CREATE FUNCTION reject_start_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced commit failure'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reject_start_commit AFTER INSERT ON work_sessions DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_start_commit()");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER reject_start_commit ON work_sessions");
      jdbc.execute("DROP FUNCTION reject_start_commit()");
    }
    assertCounts(0);
  }

  @Test
  void s10_foreignContextIsNotFoundBeforeReplay() {
    start().start("owner", project, task, key, 25);
    assertThatThrownBy(() -> start().start("foreign", project, task, key, 25))
        .isInstanceOf(ResourceNotFoundException.class);
    assertCounts(1);
  }

  @Test
  void s22_foreignIdentityIsNotFound() {
    var session = start().start("owner", project, task, key, 25).session();
    assertThatThrownBy(() -> new ReadWorkSessions(store).detail("foreign", session.id()))
        .isInstanceOf(WorkSessionNotFoundException.class);
    assertCounts(1);
  }

  @Test
  void s22_absentIdentityIsNotFound() {
    assertThatThrownBy(() -> new ReadWorkSessions(store).detail("owner", UUID.randomUUID()))
        .isInstanceOf(WorkSessionNotFoundException.class);
    assertCounts(0);
  }

  @Test
  void s22_foreignKeyIsNotFound() {
    start().start("owner", project, task, key, 25);
    assertThatThrownBy(() -> new ReadWorkSessions(store).byRequest("foreign", key))
        .isInstanceOf(WorkSessionNotFoundException.class);
    assertCounts(1);
  }

  @Test
  void s22_absentKeyIsNotFound() {
    assertThatThrownBy(() -> new ReadWorkSessions(store).byRequest("owner", key))
        .isInstanceOf(WorkSessionNotFoundException.class);
    assertCounts(0);
  }

  @Test
  void s19_projectCompletionFirstRejectsWaitingStart() throws Exception {
    completionFirst("projects", ProjectCompletedException.class);
  }

  private void completionFirst(String table, Class<? extends RuntimeException> expected)
      throws Exception {
    try (var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        var connection = jdbc.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try {
        try (var statement = connection.prepareStatement(completionSql(table))) {
          statement.setObject(1, table.equals("projects") ? project : task);
          assertThat(statement.executeUpdate()).isEqualTo(1);
        }
        var pending = workers.submit(() -> start().start("owner", project, task, key, 25));
        awaitLock("SELECT status FROM " + table + "%");
        assertThat(pending.isDone()).isFalse();
        connection.commit();
        assertThatThrownBy(() -> pending.get(10, java.util.concurrent.TimeUnit.SECONDS))
            .hasCauseInstanceOf(expected);
      } finally {
        connection.rollback();
      }
    }
    assertCounts(0);
  }

  private String completionSql(String table) {
    return table.equals("projects")
        ? "UPDATE projects SET status='completed',updated_at=now() WHERE id=?"
        : "UPDATE tasks SET status='completed',completed_at=now(),updated_at=now() WHERE id=?";
  }

  private void awaitLock(String pattern) throws Exception {
    var end = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
    while (System.nanoTime() < end) {
      if (jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE ?",
              Integer.class,
              pattern)
          > 0) return;
      Thread.sleep(10);
    }
    throw new AssertionError("Expected PostgreSQL lock wait: " + pattern);
  }

  @Test
  void s19_taskCompletionFirstRejectsWaitingStart() throws Exception {
    completionFirst("tasks", TaskCompletedException.class);
  }

  @Test
  void s19_startFirstRemainsActiveAfterProjectCompletion() throws Exception {
    startFirst("projects");
  }

  private void startFirst(String table) throws Exception {
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    WorkSessionStarting held =
        (owner, p, t, k, minutes, operation) ->
            store.commit(
                owner,
                p,
                t,
                k,
                minutes,
                context -> {
                  entered.countDown();
                  try {
                    if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                      throw new AssertionError("start release timed out");
                  } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                  }
                  return operation.apply(context);
                });
    var command = new StartWorkSession(held, CLOCK, () -> Set.of("UTC"));
    try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      try {
        var pending = workers.submit(() -> command.start("owner", project, task, key, 25));
        assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var writer =
            workers.submit(
                () -> jdbc.update(completionSql(table), table.equals("projects") ? project : task));
        awaitLock("UPDATE " + table + "%");
        assertThat(writer.isDone()).isFalse();
        release.countDown();
        var confirmed = pending.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(writer.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1);
        assertThat(store.byRequest("owner", key)).contains(confirmed.session());
        assertThat(store.active("owner")).contains(confirmed.session());
      } finally {
        release.countDown();
      }
    }
    assertCounts(1);
  }

  @Test
  void s19_startFirstRemainsActiveAfterTaskCompletion() throws Exception {
    startFirst("tasks");
  }

  @Test
  void s18_differentOwnersCanStartTogetherWithSameKey() throws Exception {
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'foreign','Other','','active',now(),now())",
        otherProject);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        otherProject);
    var results = race("foreign", otherProject, otherTask, key, 25);
    assertThat(results)
        .allSatisfy(
            result ->
                assertThat(result)
                    .isInstanceOfSatisfying(
                        WorkSessionConfirmation.class,
                        confirmation -> assertThat(confirmation.replayed()).isFalse()));
    assertThat(store.active("owner").orElseThrow().projectId()).isEqualTo(project);
    assertThat(store.active("foreign").orElseThrow().projectId()).isEqualTo(otherProject);
    assertCounts(2);
  }

  @Test
  void s1_databaseRejectsTaskFromAnotherProject() {
    var otherProject = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Other','','active',now(),now())",
        otherProject);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'owner',?,?,?,now(),25,now()+interval '25 minutes','UTC','running')",
                    UUID.randomUUID(),
                    otherProject,
                    task,
                    key))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
        .hasMessageContaining("work_sessions_task_project");
    assertCounts(0);
  }

  @Test
  void s20_outboxUniqueFailureRemainsStorageWhenAnotherStartWinsAfterRollback() {
    jdbc.execute(
        "CREATE FUNCTION fail_unique_outbox() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE unique_violation USING MESSAGE='forced outbox collision'; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_unique_outbox BEFORE INSERT ON outbox_events FOR EACH ROW EXECUTE FUNCTION fail_unique_outbox()");
    var winner = new java.util.concurrent.atomic.AtomicReference<WorkSessionConfirmation>();
    var injected = new java.util.concurrent.atomic.AtomicBoolean();
    var afterRollback =
        new org.springframework.transaction.PlatformTransactionManager() {
          public org.springframework.transaction.TransactionStatus getTransaction(
              org.springframework.transaction.TransactionDefinition definition) {
            return manager.getTransaction(definition);
          }

          public void commit(org.springframework.transaction.TransactionStatus status) {
            manager.commit(status);
          }

          public void rollback(org.springframework.transaction.TransactionStatus status) {
            manager.rollback(status);
            if (!injected.compareAndSet(false, true)) return;
            jdbc.execute("DROP TRIGGER fail_unique_outbox ON outbox_events");
            winner.set(start().start("owner", project, task, UUID.randomUUID(), 25));
          }
        };
    try {
      var command =
          new StartWorkSession(
              new PostgresWorkSessionStore(jdbc, afterRollback, json), CLOCK, () -> Set.of("UTC"));
      assertThatThrownBy(() -> command.start("owner", project, task, key, 25))
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(store.byRequest("owner", key)).isEmpty();
      assertThat(store.active("owner")).contains(winner.get().session());
      assertCounts(1);
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS fail_unique_outbox ON outbox_events");
      jdbc.execute("DROP FUNCTION fail_unique_outbox()");
    }
  }

  @Test
  void s5_ideaProjectDoesNotRequirePlanning() {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", "idea", project);
    var result = start().start("owner", project, task, key, 25);
    assertThat(result.replayed()).isFalse();
    assertThat(jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, project))
        .isEqualTo("idea");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
        .isZero();
    assertCounts(1);
  }

  @Test
  void s5_pausedProjectDoesNotRequirePlanning() {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", "paused", project);
    var result = start().start("owner", project, task, key, 25);
    assertThat(result.replayed()).isFalse();
    assertThat(jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, project))
        .isEqualTo("paused");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Integer.class))
        .isZero();
    assertCounts(1);
  }

  @Test
  void s11_completedProjectPrecedesAvailabilityStorageFailure() {
    jdbc.update(completionSql("projects"), project);
    jdbc.execute("ALTER TABLE availability_preferences RENAME TO unavailable_preferences");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(ProjectCompletedException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_preferences RENAME TO availability_preferences");
    }
    assertCounts(0);
  }

  @Test
  void s11_completedTaskPrecedesAvailabilityStorageFailure() {
    jdbc.update(completionSql("tasks"), task);
    jdbc.execute("ALTER TABLE availability_preferences RENAME TO unavailable_preferences");
    try {
      assertThatThrownBy(() -> start().start("owner", project, task, key, 25))
          .isInstanceOf(TaskCompletedException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_preferences RENAME TO availability_preferences");
    }
    assertCounts(0);
  }

  @Test
  void s16_planningAndStartKeysAreIndependent() throws Exception {
    var blockId = UUID.randomUUID();
    var changeId = UUID.randomUUID();
    var created = java.time.Instant.parse("2026-09-01T00:00:00Z");
    var changed = created.plusSeconds(60);
    var local = java.time.LocalDateTime.parse("2026-09-02T10:00");
    var offset = java.time.ZoneOffset.UTC;
    var request =
        new com.apptolast.organization.domain.BlockRequest(
            "Original", local, local.plusHours(1), "UTC", offset, offset, false);
    var block =
        new com.apptolast.organization.domain.PlannedBlock(
            blockId,
            project,
            task,
            request,
            new com.apptolast.organization.domain.ResolvedBlockTime(
                local.toInstant(offset), local.plusHours(1).toInstant(offset), offset, offset, 60),
            created);
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,'Original',?,?,'UTC','Z','Z',false,?,?,60,?)",
        blockId,
        project,
        task,
        key,
        local,
        local.plusHours(1),
        java.sql.Timestamp.from(block.time().startAt()),
        java.sql.Timestamp.from(block.time().endAt()),
        java.sql.Timestamp.from(created));
    var receipt =
        new com.apptolast.organization.domain.BlockChangeReceipt(
            changeId, blockId, "CANCELLED", 2, changed, block, null);
    jdbc.update(
        "INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt) VALUES (?,?,?,?,?,'CANCELLED',2,?,?::jsonb)",
        changeId,
        project,
        task,
        blockId,
        key,
        java.sql.Timestamp.from(changed),
        json.writeValueAsString(receipt));
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,2,'cancelled',?)",
        blockId,
        java.sql.Timestamp.from(changed));
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var receipts = jdbc.queryForList("SELECT * FROM block_changes");
    var projections = jdbc.queryForList("SELECT * FROM block_projections");
    var result = start().start("owner", project, task, key, 25);
    assertThat(result.replayed()).isFalse();
    assertThat(store.byRequest("owner", key)).contains(result.session());
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForList("SELECT * FROM block_changes")).isEqualTo(receipts);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projections);
    assertCounts(1);
  }
}
