package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class ScheduleBlockPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
  static JdbcTemplate jdbc;
  static TransactionTemplate transaction;
  static ObjectMapper json;
  PostgresBlockStore store;
  PlanBlock service;
  Context context;

  record Context(String owner, UUID project, UUID task, UUID preference) {}

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @BeforeEach
  void reset() {
    jdbc.execute(
        "TRUNCATE planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    store =
        new PostgresBlockStore(
            jdbc, transaction, new PostgresAvailabilityStore(jdbc, transaction), json);
    service =
        new PlanBlock(store, store, ZoneId::getAvailableZoneIds, Clock.fixed(NOW, ZoneOffset.UTC));
    context = context("owner-a");
  }

  Context context(String owner) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Project','','active',?,?)",
        project,
        owner,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Task','','pending',?,?)",
        task,
        project,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    var preferences =
        jdbc.queryForList(
            "SELECT id FROM availability_preferences WHERE owner_id=?", UUID.class, owner);
    var preference = preferences.isEmpty() ? UUID.randomUUID() : preferences.getFirst();
    if (preferences.isEmpty())
      jdbc.update(
          "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,'UTC',120,120,120,120,120,120,120,0,?,?)",
          preference,
          owner,
          java.sql.Timestamp.from(NOW),
          java.sql.Timestamp.from(NOW));
    return new Context(owner, project, task, preference);
  }

  BlockRequest request(String start, String end, boolean consent) {
    return new BlockRequest(
        "Meta",
        LocalDateTime.parse("2030-01-07T" + start),
        LocalDateTime.parse("2030-01-07T" + end),
        "UTC",
        ZoneOffset.UTC,
        ZoneOffset.UTC,
        consent);
  }

  BlockCreation create(Context c, UUID key, BlockRequest request) {
    return service.create(
        c.owner(),
        c.project(),
        c.task(),
        key,
        new AvailabilityRevision(c.preference(), 0),
        request);
  }

  @Test
  void s12_previewCountsOwnReservationsAcrossCompletedProjectsOnly() {
    create(context, UUID.randomUUID(), request("09:00", "09:30", false));
    var completed = context("owner-a");
    create(completed, UUID.randomUUID(), request("08:00", "09:00", false));
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", completed.project());
    jdbc.update(
        "UPDATE tasks SET status='completed',completed_at=? WHERE id=?",
        java.sql.Timestamp.from(NOW),
        completed.task());
    var other = context("owner-b");
    create(other, UUID.randomUUID(), request("08:00", "10:00", false));
    var preview =
        service.preview(
            context.owner(), context.project(), context.task(), request("10:00", "11:00", false));
    assertThat(preview.days())
        .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 5400, 3600, 1800));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(3);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"overlap", "budget"})
  void s13_s15_creationRechecksAllOwnReservationsBeforeWriting(String conflict) {
    var another = context("owner-a");
    var existing = create(another, UUID.randomUUID(), request("09:00", "10:00", false));
    var next =
        conflict.equals("overlap")
            ? request("09:30", "10:30", true)
            : request("10:00", "12:00", false);
    assertThatThrownBy(() -> create(context, UUID.randomUUID(), next))
        .isInstanceOf(
            conflict.equals("overlap")
                ? BlockOverlapException.class
                : BlockBudgetExceededException.class);
    assertThat(jdbc.queryForList("SELECT id FROM planned_blocks", UUID.class))
        .containsExactly(existing.block().id());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  void awaitWaiting(String fragment, int expected) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
    int waiting;
    do {
      waiting =
          jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE ?",
              Integer.class,
              "%" + fragment + "%");
      if (waiting >= expected) return;
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    assertThat(waiting)
        .as("database sessions waiting on %s", fragment)
        .isGreaterThanOrEqualTo(expected);
  }

  @Test
  void s29_sameKeyWaitingForAvailabilityReplaysAfterWinningCommit() throws Exception {
    var key = UUID.randomUUID();
    var intent = request("10:00", "11:00", false);
    try (var blocker =
        java.sql.DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      blocker.setAutoCommit(false);
      try (var lock =
          blocker.prepareStatement(
              "SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE")) {
        lock.setString(1, context.owner());
        lock.executeQuery().close();
      }
      try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          var first = workers.submit(() -> create(context, key, intent));
          var second = workers.submit(() -> create(context, key, intent));
          awaitWaiting("availability_preferences", 2);
          blocker.commit();
          var results =
              List.of(
                  first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(10, java.util.concurrent.TimeUnit.SECONDS));
          assertThat(results)
              .extracting(BlockCreation::replayed)
              .containsExactlyInAnyOrder(false, true);
          assertThat(results.getFirst().block()).isEqualTo(results.getLast().block());
          assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
              .isEqualTo(1);
          assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
              .isEqualTo(1);
        } finally {
          blocker.rollback();
        }
      }
    }
  }

  Object creationOutcome(Context c, UUID key, BlockRequest intent) {
    try {
      return create(c, key, intent);
    } catch (RuntimeException error) {
      return error;
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"overlap", "budget", "sameKey"})
  void s30_serializesConflictingReservationsAcrossProjects(String conflict) throws Exception {
    var secondContext = conflict.equals("sameKey") ? context : context("owner-a");
    var firstKey = UUID.randomUUID();
    var secondKey = conflict.equals("sameKey") ? firstKey : UUID.randomUUID();
    var firstIntent = request("10:00", conflict.equals("budget") ? "12:00" : "11:00", false);
    var secondIntent =
        conflict.equals("budget")
            ? request("12:00", "14:00", false)
            : request("10:30", "11:30", false);
    Class<?> expected =
        conflict.equals("overlap")
            ? BlockOverlapException.class
            : conflict.equals("budget")
                ? BlockBudgetExceededException.class
                : BlockIdempotencyConflictException.class;
    try (var blocker =
        java.sql.DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      blocker.setAutoCommit(false);
      try (var lock =
          blocker.prepareStatement(
              "SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE")) {
        lock.setString(1, context.owner());
        lock.executeQuery().close();
      }
      try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          var first = workers.submit(() -> creationOutcome(context, firstKey, firstIntent));
          var second =
              workers.submit(() -> creationOutcome(secondContext, secondKey, secondIntent));
          awaitWaiting("availability_preferences", 2);
          blocker.commit();
          var results =
              List.of(
                  first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(10, java.util.concurrent.TimeUnit.SECONDS));
          assertThat(results).filteredOn(BlockCreation.class::isInstance).hasSize(1);
          assertThat(results).filteredOn(expected::isInstance).hasSize(1);
          assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
              .isEqualTo(1);
          assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
              .isEqualTo(1);
        } finally {
          blocker.rollback();
        }
      }
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"blockFailure", "blockSuppressed", "outboxFailure", "outboxSuppressed", "commit"})
  void s34_rollsBackBothWritesAndClassifiesEveryPrecommitFailure(String fault) {
    var table = fault.startsWith("block") ? "planned_blocks" : "outbox_events";
    var projectBefore = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", context.project());
    var taskBefore = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", context.task());
    var availabilityBefore =
        jdbc.queryForMap("SELECT * FROM availability_preferences WHERE id=?", context.preference());
    jdbc.execute(
        "CREATE FUNCTION block_persistence_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + (fault.endsWith("Suppressed")
                ? "RETURN NULL;"
                : "RAISE EXCEPTION 'injected storage failure';")
            + " END; $$");
    jdbc.execute(
        fault.equals("commit")
            ? "CREATE CONSTRAINT TRIGGER block_persistence_fault AFTER INSERT ON outbox_events DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION block_persistence_failure()"
            : "CREATE TRIGGER block_persistence_fault BEFORE INSERT ON "
                + table
                + " FOR EACH ROW EXECUTE FUNCTION block_persistence_failure()");
    var key = UUID.randomUUID();
    try {
      assertThatThrownBy(() -> create(context, key, request("10:00", "11:00", false)))
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
      assertThatThrownBy(
              () -> store.byRequest(context.owner(), context.project(), context.task(), key))
          .isInstanceOf(BlockNotFoundException.class);
      assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", context.project()))
          .isEqualTo(projectBefore);
      assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", context.task()))
          .isEqualTo(taskBefore);
      assertThat(
              jdbc.queryForMap(
                  "SELECT * FROM availability_preferences WHERE id=?", context.preference()))
          .isEqualTo(availabilityBefore);
    } finally {
      jdbc.execute("DROP TRIGGER block_persistence_fault ON " + table);
      jdbc.execute("DROP FUNCTION block_persistence_failure()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "order",
        "zero",
        "long",
        "duration",
        "fraction",
        "yearZero",
        "year10000",
        "localYear",
        "localPrecision",
        "relationship",
        "key"
      })
  void s2_s6_s10_s29_databaseRejectsInvalidStoredIntervalsAndDuplicateKeys(String defect) {
    create(context, UUID.randomUUID(), request("10:00", "11:00", false));
    var original = jdbc.queryForMap("SELECT * FROM planned_blocks");
    String assignment =
        switch (defect) {
          case "order" -> "end_at=start_at";
          case "zero" -> "duration_minutes=0";
          case "long" -> "duration_minutes=1441,end_at=start_at+interval '1441 minutes'";
          case "duration" -> "duration_minutes=59";
          case "fraction" ->
              "start_at=start_at+interval '1 microsecond',end_at=end_at+interval '1 microsecond'";
          case "yearZero" ->
              "start_at=timestamptz '0001-01-01 10:00:00+00'-interval '1 year',end_at=timestamptz '0001-01-01 11:00:00+00'-interval '1 year'";
          case "year10000" ->
              "start_at=timestamptz '9999-12-31 23:30:00+00',end_at=timestamptz '10000-01-01 00:30:00+00'";
          case "localYear" ->
              "start_local=timestamp '10000-01-01 10:00:00',end_local=timestamp '10000-01-01 11:00:00'";
          case "localPrecision" -> "start_local=start_local+interval '1 second'";
          default -> "project_id=gen_random_uuid()";
        };
    String sql =
        defect.equals("key")
            ? "INSERT INTO planned_blocks SELECT gen_random_uuid(),project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at FROM planned_blocks"
            : "UPDATE planned_blocks SET " + assignment;
    assertThatThrownBy(() -> jdbc.execute(sql))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(jdbc.queryForMap("SELECT * FROM planned_blocks")).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s33_absentLockedPreferenceCannotBeReplacedByUnlockedConcurrentInsert(boolean create) {
    jdbc.update("DELETE FROM availability_preferences WHERE owner_id=?", context.owner());
    var observedJdbc = org.mockito.Mockito.spy(jdbc);
    var inserted = new java.util.concurrent.atomic.AtomicBoolean();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              var rows = invocation.callRealMethod();
              java.util.concurrent.CompletableFuture.runAsync(
                      () -> {
                        jdbc.update(
                            "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,'UTC',120,120,120,120,120,120,120,0,?,?)",
                            context.preference(),
                            context.owner(),
                            java.sql.Timestamp.from(NOW),
                            java.sql.Timestamp.from(NOW));
                        inserted.set(true);
                      })
                  .get(10, java.util.concurrent.TimeUnit.SECONDS);
              return rows;
            })
        .when(observedJdbc)
        .queryForList(
            "SELECT id FROM availability_preferences WHERE owner_id=? FOR "
                + (create ? "UPDATE" : "SHARE"),
            context.owner());
    var guardedStore =
        new PostgresBlockStore(
            observedJdbc, transaction, new PostgresAvailabilityStore(jdbc, transaction), json);
    var guarded =
        new PlanBlock(
            guardedStore,
            guardedStore,
            ZoneId::getAvailableZoneIds,
            Clock.fixed(NOW, ZoneOffset.UTC));
    assertThatThrownBy(
            () -> {
              if (create)
                guarded.create(
                    context.owner(),
                    context.project(),
                    context.task(),
                    UUID.randomUUID(),
                    new AvailabilityRevision(context.preference(), 0),
                    request("10:00", "11:00", false));
              else
                guarded.preview(
                    context.owner(),
                    context.project(),
                    context.task(),
                    request("10:00", "11:00", false));
            })
        .isInstanceOf(AvailabilityRequiredException.class);
    assertThat(inserted).isTrue();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  Object change(String kind) {
    var clock = Clock.fixed(NOW.plusSeconds(1), ZoneOffset.UTC);
    return switch (kind) {
      case "task" ->
          new ChangeTaskStatus(new PostgresTaskStatusStore(jdbc, transaction, json), clock)
              .execute(
                  context.owner(),
                  context.project(),
                  context.task(),
                  new TaskRevision(context.task(), 0),
                  "completed");
      case "project" ->
          new ChangeProjectStatus(
                  new PostgresProjectStatusEditing(
                      jdbc, transaction, new PostgresProjectQueries(jdbc), json),
                  clock,
                  10)
              .execute(
                  context.owner(),
                  context.project(),
                  new ProjectRevision(context.project(), 0),
                  "completed");
      default ->
          new PostgresAvailabilityStore(jdbc, transaction)
              .save(
                  context.owner(),
                  previous -> {
                    var prior = previous.orElseThrow();
                    return new Availability(
                        prior.id(),
                        prior.ownerId(),
                        "Europe/Madrid",
                        prior.dailyMinutes(),
                        prior.version() + 1,
                        prior.createdAt(),
                        NOW.plusSeconds(1));
                  });
    };
  }

  static void await(java.util.concurrent.CountDownLatch latch) {
    try {
      assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new AssertionError(error);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "task,true",
    "task,false",
    "project,true",
    "project,false",
    "availability,true",
    "availability,false"
  })
  void s31_realStateAndPreferenceWritersCoordinateInBothCommitOrders(
      String kind, boolean creationFirst) throws Exception {
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var key = UUID.randomUUID();
    var intent = request("10:00", "11:00", false);
    try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      try {
        var first =
            workers.submit(
                () ->
                    transaction.execute(
                        status -> {
                          assertThat(
                                  jdbc.queryForObject("SHOW transaction_isolation", String.class))
                              .isEqualTo("read committed");
                          var result = creationFirst ? create(context, key, intent) : change(kind);
                          locked.countDown();
                          await(release);
                          return result;
                        }));
        await(locked);
        var second =
            workers.submit(
                () -> creationFirst ? change(kind) : creationOutcome(context, key, intent));
        awaitWaiting(
            kind.equals("availability")
                ? "availability_preferences"
                : kind.equals("task") ? "tasks" : "projects",
            1);
        assertThat(second.isDone()).isFalse();
        release.countDown();
        var firstResult = first.get(10, java.util.concurrent.TimeUnit.SECONDS);
        var secondResult = second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        if (creationFirst) {
          assertThat(firstResult).isInstanceOf(BlockCreation.class);
          var restored = store.byRequest(context.owner(), context.project(), context.task(), key);
          assertThat(restored).isEqualTo(((BlockCreation) firstResult).block());
          assertThat(restored.request().zoneId()).isEqualTo("UTC");
        } else {
          assertThat(secondResult)
              .isInstanceOf(
                  kind.equals("task")
                      ? TaskCompletedException.class
                      : kind.equals("project")
                          ? ProjectCompletedException.class
                          : AvailabilityConflictException.class);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
            .isEqualTo(creationFirst ? 1 : 0);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
                    Long.class))
            .isEqualTo(creationFirst ? 1 : 0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
            .isEqualTo((creationFirst ? 1L : 0L) + (kind.equals("availability") ? 0L : 1L));
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"availability", "reservation"})
  void s33_previewHoldsOneCoherentSnapshotAgainstConcurrentWriters(String change) throws Exception {
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    BlockPlanning held =
        (owner, project, task, operation) ->
            store.preview(
                owner,
                project,
                task,
                snapshot -> {
                  locked.countDown();
                  await(release);
                  return operation.apply(snapshot);
                });
    var reader =
        new PlanBlock(held, store, ZoneId::getAvailableZoneIds, Clock.fixed(NOW, ZoneOffset.UTC));
    var another = context("owner-a");
    var intent = request("10:00", "11:00", false);
    try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      try {
        var preview =
            workers.submit(
                () -> reader.preview(context.owner(), context.project(), context.task(), intent));
        await(locked);
        var writer =
            workers.submit(
                () -> {
                  if (change.equals("reservation"))
                    return create(another, UUID.randomUUID(), request("09:00", "10:00", false));
                  return transaction.execute(
                      status -> {
                        var result = change("availability");
                        jdbc.update(
                            "UPDATE availability_preferences SET monday_minutes=30 WHERE owner_id=?",
                            context.owner());
                        return result;
                      });
                });
        awaitWaiting("availability_preferences", 1);
        assertThat(writer.isDone()).isFalse();
        release.countDown();
        var original = preview.get(10, java.util.concurrent.TimeUnit.SECONDS);
        writer.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(original.availabilityRevision())
            .isEqualTo(new AvailabilityRevision(context.preference(), 0));
        assertThat(original.budgetZoneId()).isEqualTo("UTC");
        assertThat(original.days())
            .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 0, 3600, 0));
        var current = service.preview(context.owner(), context.project(), context.task(), intent);
        if (change.equals("availability")) {
          assertThat(current.availabilityRevision())
              .isEqualTo(new AvailabilityRevision(context.preference(), 1));
          assertThat(current.budgetZoneId()).isEqualTo("Europe/Madrid");
          assertThat(current.days())
              .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 30, 0, 3600, 1800));
        } else {
          assertThat(current.availabilityRevision()).isEqualTo(original.availabilityRevision());
          assertThat(current.days())
              .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 3600, 3600, 0));
        }
        long expected = change.equals("reservation") ? 1 : 0;
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
            .isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
            .isEqualTo(expected);
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void s32_otherOwnerCommitsWhileFirstOwnerStillHoldsAvailability() throws Exception {
    var other = context("owner-b");
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      try {
        var first =
            workers.submit(
                () ->
                    transaction.execute(
                        status -> {
                          var created =
                              create(context, UUID.randomUUID(), request("10:00", "11:00", false));
                          locked.countDown();
                          await(release);
                          return created;
                        }));
        await(locked);
        var second =
            workers.submit(
                () -> create(other, UUID.randomUUID(), request("10:00", "11:00", false)));
        var independent = second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(first.isDone()).isFalse();
        assertThat(independent.replayed()).isFalse();
        assertThat(independent.block().projectId()).isEqualTo(other.project());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
            .isEqualTo(1);
        release.countDown();
        assertThat(first.get(10, java.util.concurrent.TimeUnit.SECONDS).block().id())
            .isNotEqualTo(independent.block().id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
            .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
            .isEqualTo(2);
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"preview", "list", "detail", "byRequest"})
  void s24_s26_s34_readFailuresStayStorageErrorsWithoutPartialResults(String operation) {
    var key = UUID.randomUUID();
    var created = create(context, key, request("10:00", "11:00", false));
    jdbc.execute("ALTER TABLE planned_blocks RENAME TO unavailable_planned_blocks");
    try {
      assertThatThrownBy(
              () -> {
                switch (operation) {
                  case "preview" ->
                      service.preview(
                          context.owner(),
                          context.project(),
                          context.task(),
                          request("11:00", "12:00", false));
                  case "list" ->
                      store.list(context.owner(), context.project(), context.task(), null);
                  case "detail" ->
                      store.detail(
                          context.owner(), context.project(), context.task(), created.block().id());
                  default ->
                      store.byRequest(context.owner(), context.project(), context.task(), key);
                }
              })
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_planned_blocks RENAME TO planned_blocks");
    }
    assertThat(store.byRequest(context.owner(), context.project(), context.task(), key))
        .isEqualTo(created.block());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }
}
