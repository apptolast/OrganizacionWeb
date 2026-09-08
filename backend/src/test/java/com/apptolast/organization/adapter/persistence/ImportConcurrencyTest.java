package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ImportConcurrencyTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final Instant NOW = Instant.parse("2026-09-08T01:02:03.123456Z");
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
  private static JdbcTemplate jdbc;

  @BeforeAll
  static void database() {
    var source = source("observer");
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
  }

  @ParameterizedTest
  @EnumSource(CustomizationScope.class)
  void s25_writerAlreadyReadBeforeImportCannotBeOverwritten(CustomizationScope scope)
      throws Exception {
    var owner = "concurrent-" + UUID.randomUUID();
    var writerSource = source("writer-" + owner);
    var writer =
        new PostgresCustomizationStore(
            new JdbcTemplate(writerSource), new DataSourceTransactionManager(writerSource), JSON);
    var importName = "import-" + owner;
    var importer = importer(importName);
    var bytes = configurationFile(owner, scope, UUID.randomUUID());
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var winner = UUID.randomUUID();
    var key = UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  writer.change(
                      owner,
                      scope,
                      previous -> {
                        assertThat(previous).isEmpty();
                        entered.countDown();
                        await(release);
                        return new Customization(
                            winner, owner, scope, List.of("updatedAt"), List.of(), 0, NOW);
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    importer.apply(
                        owner, key, sha(bytes), new ByteArrayInputStream(bytes), () -> NOW));
        blocked(importName);
        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).id()).isEqualTo(winner);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
            .hasCauseInstanceOf(ImportConflictException.class);
        assertThat(writer.find(owner, scope).orElseThrow().id()).isEqualTo(winner);
        assertThat(importer.find(owner, key)).isEmpty();
      } finally {
        release.countDown();
      }
    }
  }

  @ParameterizedTest
  @EnumSource(CustomizationScope.class)
  void s26_writerStartingDuringImportReadsCommittedImportedRevision(CustomizationScope scope)
      throws Exception {
    var owner = "during-" + UUID.randomUUID();
    var importedId = UUID.randomUUID();
    var bytes = configurationFile(owner, scope, importedId);
    var key = UUID.randomUUID();
    var importer = importer("import-" + owner);
    var writerName = "writer-" + owner;
    var writerSource = source(writerName);
    var writer =
        new PostgresCustomizationStore(
            new JdbcTemplate(writerSource), new DataSourceTransactionManager(writerSource), JSON);
    var command =
        new SaveCustomizationView(writer, java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  importer.apply(
                      owner,
                      key,
                      sha(bytes),
                      new ByteArrayInputStream(bytes),
                      () -> {
                        entered.countDown();
                        await(release);
                        return NOW;
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    command.save(
                        owner, scope, new CustomizationRevision(null, 0), List.of("updatedAt")));
        blocked(writerName);
        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).outcome()).isEqualTo("IMPORTED");
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
            .hasCauseInstanceOf(CustomizationConflictException.class);
        var stored = writer.find(owner, scope).orElseThrow();
        assertThat(stored.id()).isEqualTo(importedId);
        assertThat(stored.visibleFields()).containsExactly("createdAt");
        assertThat(stored.version()).isZero();
        assertThat(importer.find(owner, key)).isPresent();
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.api.Test
  void s27_advisoryContentionTimesOutWithoutPartialDataOrReceipt() throws Exception {
    var owner = "timeout-" + UUID.randomUUID();
    var name = "blocked-" + owner;
    var importer = importer(name);
    var source = source("holder-" + owner);
    var tx =
        new org.springframework.transaction.support.TransactionTemplate(
            new DataSourceTransactionManager(source));
    var holder = new JdbcTemplate(source);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var bytes = configurationFile(owner, CustomizationScope.PROJECT, UUID.randomUUID());
    var key = UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  tx.execute(
                      status -> {
                        holder.queryForObject(
                            "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                            Object.class,
                            "customization:" + owner + ":PROJECT");
                        entered.countDown();
                        await(release);
                        return true;
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    importer.apply(
                        owner, key, sha(bytes), new ByteArrayInputStream(bytes), () -> NOW));
        blocked(name);
        assertThatThrownBy(() -> second.get(8, TimeUnit.SECONDS))
            .hasCauseInstanceOf(StorageUnavailableException.class);
        assertThat(importer.find(owner, key)).isEmpty();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM customization_preferences WHERE owner_id=?",
                    Integer.class,
                    owner))
            .isZero();
      } finally {
        release.countDown();
        first.get(10, TimeUnit.SECONDS);
      }
    }
    released(owner, name);
  }

  @org.junit.jupiter.api.Test
  void s23_deferredCommitRejectionRollsBackDataAndReceipt() throws Exception {
    var owner = "deferred-" + UUID.randomUUID();
    var key = UUID.randomUUID();
    var bytes = configurationFile(owner, CustomizationScope.PROJECT, UUID.randomUUID());
    var importer = importer("import-" + owner);
    jdbc.execute(
        "CREATE FUNCTION reject_import_concurrency_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled deferred rejection'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reject_import_concurrency_commit AFTER INSERT ON import_receipts DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_import_concurrency_commit()");
    var reachedReceipt = new java.util.concurrent.atomic.AtomicBoolean();
    try {
      assertThatThrownBy(
              () ->
                  importer.apply(
                      owner,
                      key,
                      sha(bytes),
                      new ByteArrayInputStream(bytes),
                      () -> {
                        reachedReceipt.set(true);
                        return NOW;
                      }))
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(reachedReceipt).isTrue();
      assertThat(importer.find(owner, key)).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM customization_preferences WHERE owner_id=?",
                  Integer.class,
                  owner))
          .isZero();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_import_concurrency_commit ON import_receipts");
      jdbc.execute("DROP FUNCTION reject_import_concurrency_commit()");
    }
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"project", "task", "block", "session", "availability", "appearance"})
  void s24_existingWriterFamiliesWaitForImportAndKeepTheirBusinessChanges(String family)
      throws Exception {
    var owner = "family-" + UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,'','active',?,?)",
        project,
        owner,
        "Base",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,?,'','pending',?,?)",
        task,
        project,
        "Base",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    var writerName = "writer-" + owner;
    var source = source(writerName);
    var writerJdbc = new JdbcTemplate(source);
    var manager = new DataSourceTransactionManager(source);
    var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
    var clock = java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    ZoneCatalog zones = () -> java.util.Set.of("UTC");
    var days = new java.util.EnumMap<java.time.DayOfWeek, Integer>(java.time.DayOfWeek.class);
    for (var day : java.time.DayOfWeek.values()) days.put(day, 480);
    Runnable action;
    Runnable verify;
    switch (family) {
      case "project" -> {
        action =
            () ->
                new CreateProject(new PostgresProjectCommit(writerJdbc, tx, JSON), clock)
                    .execute(owner, "Concurrent project", "Preserved");
        verify =
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "SELECT count(*) FROM projects WHERE owner_id=? AND name='Concurrent project' AND description='Preserved'",
                            Integer.class,
                            owner))
                    .isEqualTo(1);
      }
      case "task" -> {
        action =
            () ->
                new ChangeTaskStatus(new PostgresTaskStatusStore(writerJdbc, tx, JSON), clock)
                    .execute(owner, project, task, new TaskRevision(task, 0), "completed");
        verify =
            () -> {
              assertThat(
                      jdbc.queryForObject(
                          "SELECT status FROM tasks WHERE id=?", String.class, task))
                  .isEqualTo("completed");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM task_status_history WHERE task_id=? AND task_version=1 AND to_status='completed'",
                          Integer.class,
                          task))
                  .isEqualTo(1);
            };
      }
      case "block" -> {
        var availability = new PostgresAvailabilityStore(writerJdbc, tx);
        var settings =
            new SaveAvailability(availability, zones, clock)
                .execute(owner, new AvailabilityRevision(null, 0), "UTC", days);
        var store = new PostgresBlockStore(writerJdbc, tx, availability, JSON);
        var start = java.time.LocalDateTime.of(2026, 9, 9, 10, 0);
        var block =
            new PlanBlock(store, store, zones, clock)
                .create(
                    owner,
                    project,
                    task,
                    UUID.randomUUID(),
                    new AvailabilityRevision(settings.id(), 0),
                    new BlockRequest(
                        "Plan",
                        start,
                        start.plusMinutes(30),
                        "UTC",
                        java.time.ZoneOffset.UTC,
                        java.time.ZoneOffset.UTC,
                        false))
                .block();
        action =
            () ->
                new CancelBlock(store, clock)
                    .cancel(
                        owner,
                        project,
                        task,
                        block.id(),
                        UUID.randomUUID(),
                        store.state(owner, project, task, block.id()).version());
        verify =
            () -> {
              assertThat(
                      jdbc.queryForObject(
                          "SELECT status FROM block_projections WHERE block_id=?",
                          String.class,
                          block.id()))
                  .isEqualTo("cancelled");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM block_changes WHERE block_id=? AND kind='CANCELLED'",
                          Integer.class,
                          block.id()))
                  .isEqualTo(1);
            };
      }
      case "session" -> {
        var store = new PostgresWorkSessionStore(writerJdbc, manager, JSON);
        var session =
            new StartWorkSession(store, clock, zones)
                .start(owner, project, task, UUID.randomUUID(), 30)
                .session();
        action =
            () ->
                new ChangeWorkSession(
                        store, java.time.Clock.fixed(NOW.plusSeconds(60), java.time.ZoneOffset.UTC))
                    .close(
                        owner,
                        session.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(
                            session.id(),
                            store
                                .read(
                                    owner,
                                    session.id(),
                                    state -> new WorkSessionSnapshot(state, NOW, 0))
                                .state()
                                .revision()),
                        new WorkSessionCloseNotes("Kept", "Next"));
        verify =
            () -> {
              assertThat(
                      jdbc.queryForObject(
                          "SELECT status FROM work_sessions WHERE id=?",
                          String.class,
                          session.id()))
                  .isEqualTo("closed");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                          Integer.class,
                          session.id()))
                  .isEqualTo(1);
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM work_session_changes WHERE session_id=? AND action='CLOSE'",
                          Integer.class,
                          session.id()))
                  .isEqualTo(1);
            };
      }
      case "availability" -> {
        action =
            () ->
                new SaveAvailability(new PostgresAvailabilityStore(writerJdbc, tx), zones, clock)
                    .execute(owner, new AvailabilityRevision(null, 0), "UTC", days);
        verify =
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "SELECT monday_minutes FROM availability_preferences WHERE owner_id=?",
                            Integer.class,
                            owner))
                    .isEqualTo(480);
      }
      default -> {
        action =
            () ->
                new SaveAppearance(new PostgresAppearanceStore(writerJdbc, tx), clock)
                    .execute(owner, new AppearanceRevision(null, 0), "DARK", "#244c3c", "#b8e0c2");
        verify =
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "SELECT theme FROM appearance_preferences WHERE owner_id=?",
                            String.class,
                            owner))
                    .isEqualTo("DARK");
      }
    }
    int eventsBefore =
        jdbc.queryForObject(
            "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner);
    var out = new ByteArrayOutputStream();
    new PrepareExportData(new PostgresExportDataQueries(writerJdbc, manager), clock)
        .prepare(owner)
        .writeTo(out);
    var bytes = out.toByteArray();
    var exported = JSON.readTree(bytes);
    assertThat(exported.path("counts").path("projects").asInt()).isEqualTo(1);
    assertThat(exported.path("counts").path("tasks").asInt()).isEqualTo(1);
    var key = UUID.randomUUID();
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  importer("import-" + owner)
                      .apply(
                          owner,
                          key,
                          sha(bytes),
                          new ByteArrayInputStream(bytes),
                          () -> {
                            entered.countDown();
                            await(release);
                            return NOW;
                          }));
      try {
        await(entered);
        var second = pool.submit(action);
        blocked(writerName);
        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).outcome()).isEqualTo("NO_CHANGE");
        second.get(10, TimeUnit.SECONDS);
        verify.run();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
            .isEqualTo(
                eventsBefore
                    + (family.equals("availability") || family.equals("appearance") ? 0 : 1));
        assertThat(
                jdbc.queryForObject("SELECT name FROM projects WHERE id=?", String.class, project))
            .isEqualTo("Base");
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.api.Test
  void s24_activationRechecksQuotaAfterImportedActiveProjectsCommit() throws Exception {
    var owner = "quota-" + UUID.randomUUID();
    var idea = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,'','idea',?,?)",
        idea,
        owner,
        "Waiting idea",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    for (int i = 0; i < 3; i++)
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,'','active',?,?)",
          UUID.randomUUID(),
          owner,
          "Active " + i,
          java.sql.Timestamp.from(NOW),
          java.sql.Timestamp.from(NOW));
    var writerName = "writer-" + owner;
    var source = source(writerName);
    var writerJdbc = new JdbcTemplate(source);
    var manager = new DataSourceTransactionManager(source);
    var clock = java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    var out = new ByteArrayOutputStream();
    new PrepareExportData(new PostgresExportDataQueries(writerJdbc, manager), clock)
        .prepare(owner)
        .writeTo(out);
    var bytes = out.toByteArray();
    assertThat(JSON.readTree(bytes).path("counts").path("projects").asInt()).isEqualTo(4);
    jdbc.update("DELETE FROM projects WHERE owner_id=? AND status='active'", owner);
    var writer =
        new ChangeProjectStatus(
            new PostgresProjectStatusEditing(
                writerJdbc,
                new org.springframework.transaction.support.TransactionTemplate(manager),
                new PostgresProjectQueries(writerJdbc),
                JSON),
            clock,
            3);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var key = UUID.randomUUID();
    var importer = importer("import-" + owner);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  importer.apply(
                      owner,
                      key,
                      sha(bytes),
                      new ByteArrayInputStream(bytes),
                      () -> {
                        entered.countDown();
                        await(release);
                        return NOW;
                      }));
      try {
        await(entered);
        var second =
            pool.submit(() -> writer.execute(owner, idea, new ProjectRevision(idea, 0), "active"));
        blocked(writerName);
        release.countDown();
        var receipt = first.get(10, TimeUnit.SECONDS);
        assertThat(receipt.insertedCounts().projects()).isEqualTo(3);
        assertThat(receipt.identicalCounts().projects()).isEqualTo(1);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
            .hasCauseInstanceOf(ActiveProjectLimitException.class);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM projects WHERE owner_id=? AND status='active'",
                    Integer.class,
                    owner))
            .isEqualTo(3);
        assertThat(
                jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, idea))
            .isEqualTo("idea");
        assertThat(jdbc.queryForObject("SELECT version FROM projects WHERE id=?", Long.class, idea))
            .isZero();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
            .isZero();
        assertThat(importer.find(owner, key)).contains(receipt);
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.api.Test
  void s27_tableContentionAfterPartialLocksRollsBackAndReleasesEverything() throws Exception {
    var owner = "table-" + UUID.randomUUID();
    var name = "import-" + owner;
    var importer = importer(name);
    var source = source("holder-" + owner);
    var tx =
        new org.springframework.transaction.support.TransactionTemplate(
            new DataSourceTransactionManager(source));
    var holder = new JdbcTemplate(source);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var key = UUID.randomUUID();
    var bytes = configurationFile(owner, CustomizationScope.PROJECT, UUID.randomUUID());
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  tx.execute(
                      status -> {
                        holder.execute("LOCK TABLE projects IN ROW EXCLUSIVE MODE");
                        entered.countDown();
                        await(release);
                        return true;
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    importer.apply(
                        owner, key, sha(bytes), new ByteArrayInputStream(bytes), () -> NOW));
        blocked(name);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.application_name=? AND l.relation='appearance_preferences'::regclass AND l.mode='ExclusiveLock' AND l.granted",
                    Integer.class,
                    name))
            .isEqualTo(1);
        assertThatThrownBy(() -> second.get(8, TimeUnit.SECONDS))
            .hasCauseInstanceOf(StorageUnavailableException.class);
        assertThat(importer.find(owner, key)).isEmpty();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM customization_preferences WHERE owner_id=?",
                    Integer.class,
                    owner))
            .isZero();
      } finally {
        release.countDown();
        first.get(10, TimeUnit.SECONDS);
      }
    }
    released(owner, name);
  }

  @org.junit.jupiter.api.Test
  void s27_deadlockWithOppositeWriterOrderAbortsImportWithoutPartialState() throws Exception {
    var owner = "deadlock-" + UUID.randomUUID();
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,'','idea',?,?)",
        project,
        owner,
        "Writer",
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    var name = "import-" + owner;
    var importer = importer(name);
    var writerName = "writer-" + owner;
    var source = source(writerName);
    var writerJdbc = new JdbcTemplate(source);
    var manager = new DataSourceTransactionManager(source);
    var tx = new org.springframework.transaction.support.TransactionTemplate(manager);
    var clock = java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC);
    var writerStore =
        new PostgresProjectStatusEditing(
            writerJdbc, tx, new PostgresProjectQueries(writerJdbc), JSON);
    var entered = new CountDownLatch(1);
    var continueWriter = new CountDownLatch(1);
    ProjectStatusEditing guarded =
        (who, id, operation) ->
            writerStore.update(
                who,
                id,
                (previous, count) -> {
                  entered.countDown();
                  await(continueWriter);
                  new SaveAppearance(new PostgresAppearanceStore(writerJdbc, tx), clock)
                      .execute(
                          owner, new AppearanceRevision(null, 0), "DARK", "#244C3C", "#B8E0C2");
                  return operation.apply(previous, count);
                });
    var command = new ChangeProjectStatus(guarded, clock, 3);
    var key = UUID.randomUUID();
    var bytes = configurationFile(owner, CustomizationScope.PROJECT, UUID.randomUUID());
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var writer =
          pool.submit(
              () -> command.execute(owner, project, new ProjectRevision(project, 0), "active"));
      try {
        await(entered);
        var imported =
            pool.submit(
                () ->
                    importer.apply(
                        owner, key, sha(bytes), new ByteArrayInputStream(bytes), () -> NOW));
        blocked(name);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.application_name=? AND l.relation='appearance_preferences'::regclass AND l.mode='ExclusiveLock' AND l.granted",
                    Integer.class,
                    name))
            .isEqualTo(1);
        continueWriter.countDown();
        var failure =
            org.assertj.core.api.Assertions.catchThrowable(
                () -> imported.get(10, TimeUnit.SECONDS));
        var writerFailure =
            org.assertj.core.api.Assertions.catchThrowable(() -> writer.get(10, TimeUnit.SECONDS));
        System.out.println(
            "DEADLOCK import=" + sqlState(failure) + " writer=" + sqlState(writerFailure));
        assertThat(failure).hasCauseInstanceOf(StorageUnavailableException.class);
        assertThat(sqlState(failure)).isEqualTo("40P01");
        assertThat(writerFailure).isNull();
        assertThat(importer.find(owner, key)).isEmpty();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM customization_preferences WHERE owner_id=?",
                    Integer.class,
                    owner))
            .isZero();
        assertThat(
                jdbc.queryForObject(
                    "SELECT status FROM projects WHERE id=?", String.class, project))
            .isEqualTo("active");
        assertThat(
                jdbc.queryForObject(
                    "SELECT theme FROM appearance_preferences WHERE owner_id=?",
                    String.class,
                    owner))
            .isEqualTo("DARK");
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
            .isEqualTo(1);
      } finally {
        continueWriter.countDown();
      }
    }
    released(owner, name);
    released(owner, writerName);
  }

  @org.junit.jupiter.api.Test
  void s27_statementOverTenSecondsAbortsAndReleasesLocksWithoutPartialReceipt() throws Exception {
    var owner = "statement-" + UUID.randomUUID();
    var name = "import-" + owner;
    var importer = importer(name);
    var key = UUID.randomUUID();
    var bytes = configurationFile(owner, CustomizationScope.PROJECT, UUID.randomUUID());
    jdbc.execute(
        "CREATE FUNCTION delay_import_concurrency_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN PERFORM pg_sleep(11); RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER delay_import_concurrency_receipt BEFORE INSERT ON import_receipts FOR EACH ROW EXECUTE FUNCTION delay_import_concurrency_receipt()");
    var reachedReceipt = new java.util.concurrent.atomic.AtomicBoolean();
    try {
      var failure =
          org.assertj.core.api.Assertions.catchThrowable(
              () ->
                  importer.apply(
                      owner,
                      key,
                      sha(bytes),
                      new ByteArrayInputStream(bytes),
                      () -> {
                        reachedReceipt.set(true);
                        return NOW;
                      }));
      assertThat(failure).isInstanceOf(StorageUnavailableException.class);
      assertThat(sqlState(failure)).isEqualTo("57014");
      assertThat(reachedReceipt).isTrue();
      assertThat(importer.find(owner, key)).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM customization_preferences WHERE owner_id=?",
                  Integer.class,
                  owner))
          .isZero();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
          .isZero();
      released(owner, name);
    } finally {
      jdbc.execute("DROP TRIGGER delay_import_concurrency_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION delay_import_concurrency_receipt()");
    }
  }

  private static String sqlState(Throwable failure) {
    for (var current = failure; current != null; current = current.getCause())
      if (current instanceof java.sql.SQLException sql) return sql.getSQLState();
    return failure == null ? "SUCCESS" : failure.getClass().getSimpleName();
  }

  @ParameterizedTest
  @EnumSource(CustomizationScope.class)
  void s25_valuesWriterAlreadyReadCannotBeOverwritten(CustomizationScope scope) throws Exception {
    var f = valuesFixture(scope);
    removeValues(f);
    var writer = customizationStore("values-writer-" + f.owner());
    var importerName = "values-import-" + f.owner();
    var importer = importer(importerName);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var winner = UUID.randomUUID();
    var key = UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  writer.changeValues(
                      f.owner(),
                      scope,
                      f.project(),
                      f.entity(),
                      (configuration, previous) -> {
                        assertThat(configuration.orElseThrow().id()).isEqualTo(f.schema());
                        assertThat(previous).isEmpty();
                        entered.countDown();
                        await(release);
                        return new CustomFieldValuesCollection(
                            winner, java.util.Map.of(f.field(), "writer"), 0, NOW);
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    importer.apply(
                        f.owner(),
                        key,
                        sha(f.bytes()),
                        new ByteArrayInputStream(f.bytes()),
                        () -> NOW));
        blocked(importerName);
        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).revision().id()).isEqualTo(winner);
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
            .hasCauseInstanceOf(ImportConflictException.class);
        var stored = writer.find(f.owner(), scope, f.project(), f.entity());
        assertThat(stored.revision()).isEqualTo(new CustomizationRevision(winner, 0));
        assertThat(stored.values().get(0).value()).isEqualTo("writer");
        assertThat(importer.find(f.owner(), key)).isEmpty();
        assertNoCustomizationEvents(f.owner());
      } finally {
        release.countDown();
      }
    }
  }

  @ParameterizedTest
  @EnumSource(CustomizationScope.class)
  void s26_valuesWriterStartingDuringImportRevalidatesCompositeRevision(CustomizationScope scope)
      throws Exception {
    var f = valuesFixture(scope);
    removeValues(f);
    var importer = importer("values-import-" + f.owner());
    var writerName = "values-writer-" + f.owner();
    var store = customizationStore(writerName);
    var command =
        new SaveCustomFieldValues(store, java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
    var expected =
        new CustomFieldValuesRevision(
            scope,
            f.entity(),
            new CustomizationRevision(f.schema(), 0),
            new CustomizationRevision(null, 0));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var key = UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  importer.apply(
                      f.owner(),
                      key,
                      sha(f.bytes()),
                      new ByteArrayInputStream(f.bytes()),
                      () -> {
                        entered.countDown();
                        await(release);
                        return NOW;
                      }));
      try {
        await(entered);
        var second =
            pool.submit(
                () ->
                    command.save(
                        f.owner(),
                        scope,
                        f.project(),
                        f.entity(),
                        expected,
                        List.of(new CustomFieldInput(f.field(), "writer"))));
        blocked(writerName);
        release.countDown();
        assertThat(first.get(10, TimeUnit.SECONDS).outcome()).isEqualTo("IMPORTED");
        assertThatThrownBy(() -> second.get(10, TimeUnit.SECONDS))
            .hasCauseInstanceOf(CustomizationConflictException.class);
        var stored = store.find(f.owner(), scope, f.project(), f.entity());
        assertThat(stored.schema()).isEqualTo(new CustomizationRevision(f.schema(), 0));
        assertThat(stored.revision()).isEqualTo(new CustomizationRevision(f.valuesId(), 0));
        assertThat(stored.values().get(0).value()).isEqualTo("imported");
        assertThat(importer.find(f.owner(), key)).isPresent();
        assertNoCustomizationEvents(f.owner());
      } finally {
        release.countDown();
      }
    }
  }

  @ParameterizedTest
  @EnumSource(CustomizationScope.class)
  void s18_previewKeepsOneSnapshotWhileSchemaAndValuesCommit(CustomizationScope scope)
      throws Exception {
    var f = valuesFixture(scope);
    var name = "snapshot-" + f.owner();
    var preview = importer(name);
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var input =
        new java.io.FilterInputStream(new ByteArrayInputStream(f.bytes())) {
          private boolean reached;

          @Override
          public int read(byte[] bytes, int offset, int length) throws java.io.IOException {
            if (!reached) {
              reached = true;
              entered.countDown();
              await(release);
            }
            return super.read(bytes, offset, length);
          }
        };
    var writerSource = source("snapshot-writer-" + f.owner());
    var writer =
        new PostgresCustomizationStore(
            new JdbcTemplate(writerSource), new DataSourceTransactionManager(writerSource), JSON);
    var transaction =
        new org.springframework.transaction.support.TransactionTemplate(
            new DataSourceTransactionManager(writerSource));
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first = pool.submit(() -> preview.preview(f.owner(), input));
      try {
        await(entered);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE application_name=? AND backend_xmin IS NOT NULL",
                    Integer.class,
                    name))
            .isEqualTo(1);
        var second =
            pool.submit(
                () ->
                    transaction.execute(
                        status -> {
                          writer.change(
                              f.owner(),
                              scope,
                              previous -> {
                                var old = previous.orElseThrow();
                                return new Customization(
                                    old.id(),
                                    f.owner(),
                                    scope,
                                    old.visibleFields(),
                                    List.of(
                                        new CustomFieldDefinition(
                                            f.field(), "Renamed", CustomFieldType.TEXT, true)),
                                    1,
                                    NOW.plusSeconds(1));
                              });
                          return new SaveCustomFieldValues(
                                  writer,
                                  java.time.Clock.fixed(
                                      NOW.plusSeconds(1), java.time.ZoneOffset.UTC))
                              .save(
                                  f.owner(),
                                  scope,
                                  f.project(),
                                  f.entity(),
                                  new CustomFieldValuesRevision(
                                      scope,
                                      f.entity(),
                                      new CustomizationRevision(f.schema(), 1),
                                      new CustomizationRevision(f.valuesId(), 0)),
                                  List.of(new CustomFieldInput(f.field(), "updated")));
                        }));
        var changed = second.get(10, TimeUnit.SECONDS);
        assertThat(changed.schema().version()).isEqualTo(1);
        assertThat(changed.revision().version()).isEqualTo(1);
        assertThat(changed.values().get(0).label()).isEqualTo("Renamed");
        assertThat(changed.values().get(0).value()).isEqualTo("updated");
        assertThat(first).isNotDone();
        release.countDown();
        var observed = first.get(10, TimeUnit.SECONDS);
        assertThat(observed.identicalCounts().customization()).isEqualTo(1);
        assertThat(
                scope == CustomizationScope.PROJECT
                    ? observed.identicalCounts().projectCustomFieldValues()
                    : observed.identicalCounts().taskCustomFieldValues())
            .isEqualTo(1);
        assertThat(observed.insertCounts().customization()).isZero();
        assertThatThrownBy(() -> preview.preview(f.owner(), new ByteArrayInputStream(f.bytes())))
            .isInstanceOf(ImportConflictException.class);
        assertThat(writer.find(f.owner(), scope, f.project(), f.entity())).isEqualTo(changed);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM import_receipts WHERE owner_id=?",
                    Integer.class,
                    f.owner()))
            .isZero();
        assertNoCustomizationEvents(f.owner());
      } finally {
        release.countDown();
      }
    }
  }

  private record ValuesFixture(
      String owner,
      CustomizationScope scope,
      UUID project,
      UUID entity,
      UUID schema,
      UUID field,
      UUID valuesId,
      byte[] bytes) {}

  private static PostgresCustomizationStore customizationStore(String name) {
    var source = source(name);
    return new PostgresCustomizationStore(
        new JdbcTemplate(source), new DataSourceTransactionManager(source), JSON);
  }

  private static ValuesFixture valuesFixture(CustomizationScope scope) throws Exception {
    var owner = "values-" + UUID.randomUUID();
    var project = UUID.randomUUID();
    var entity = scope == CustomizationScope.PROJECT ? project : UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Project','','idea',?,?)",
        project,
        owner,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    if (scope == CustomizationScope.TASK)
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Task','','pending',?,?)",
          entity,
          project,
          java.sql.Timestamp.from(NOW),
          java.sql.Timestamp.from(NOW));
    var schema = UUID.randomUUID();
    var field = UUID.randomUUID();
    var values = UUID.randomUUID();
    var store = customizationStore("seed-" + owner);
    store.change(
        owner,
        scope,
        previous ->
            new Customization(
                schema,
                owner,
                scope,
                List.of("createdAt"),
                List.of(new CustomFieldDefinition(field, "Text", CustomFieldType.TEXT, true)),
                0,
                NOW));
    store.changeValues(
        owner,
        scope,
        project,
        entity,
        (configuration, previous) ->
            new CustomFieldValuesCollection(values, java.util.Map.of(field, "imported"), 0, NOW));
    var bytes = new ByteArrayOutputStream();
    var source = source("export-" + owner);
    new PostgresExportDataQueries(
            new JdbcTemplate(source), new DataSourceTransactionManager(source))
        .prepare(owner, () -> NOW)
        .writeTo(bytes);
    return new ValuesFixture(
        owner, scope, project, entity, schema, field, values, bytes.toByteArray());
  }

  private static void removeValues(ValuesFixture f) {
    var table =
        f.scope() == CustomizationScope.PROJECT
            ? "project_custom_field_values"
            : "task_custom_field_values";
    assertThat(jdbc.update("DELETE FROM " + table + " WHERE owner_id=?", f.owner())).isEqualTo(1);
  }

  private static void assertNoCustomizationEvents(String owner) {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  private static void released(String owner, String name) {
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM pg_locks l JOIN pg_stat_activity a ON a.pid=l.pid WHERE a.application_name=?",
                Integer.class,
                name))
        .isZero();
    var source = source("release-check-" + UUID.randomUUID());
    var check = new JdbcTemplate(source);
    new org.springframework.transaction.support.TransactionTemplate(
            new DataSourceTransactionManager(source))
        .executeWithoutResult(
            status -> {
              for (var scope : CustomizationScope.values())
                assertThat(
                        check.queryForObject(
                            "SELECT pg_try_advisory_xact_lock(hashtextextended(?,0))",
                            Boolean.class,
                            "customization:" + owner + ":" + scope))
                    .isTrue();
              check.execute(
                  "LOCK TABLE appearance_preferences,availability_preferences,block_changes,block_projections,customization_preferences,import_receipts,planned_blocks,project_custom_field_values,projects,task_custom_field_values,task_status_history,tasks,work_session_changes,work_session_intervals,work_sessions IN EXCLUSIVE MODE NOWAIT");
            });
  }

  private static byte[] configurationFile(String owner, CustomizationScope scope, UUID id)
      throws Exception {
    var out = new ByteArrayOutputStream();
    new ExportJsonWriter().empty(owner, NOW).writeTo(out);
    var root = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.readTree(out.toByteArray());
    ((com.fasterxml.jackson.databind.node.ObjectNode) root.get("counts")).put("customization", 1);
    var configs =
        (com.fasterxml.jackson.databind.node.ArrayNode) root.path("data").path("customization");
    var row =
        configs
            .addObject()
            .put("id", id.toString())
            .put("scope", scope.name())
            .put("version", "0")
            .put("updatedAt", NOW.toString());
    row.putArray("visibleFields").add("createdAt");
    row.putArray("customFields");
    return JSON.writeValueAsBytes(root);
  }

  private static DriverManagerDataSource source(String name) {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var properties = new java.util.Properties();
    properties.setProperty("ApplicationName", name);
    source.setConnectionProperties(properties);
    return source;
  }

  private static PostgresImportDataStore importer(String name) {
    var source = source(name);
    return new PostgresImportDataStore(
        new JdbcTemplate(source), new DataSourceTransactionManager(source));
  }

  private static void blocked(String name) {
    org.awaitility.Awaitility.await()
        .atMost(java.time.Duration.ofSeconds(1))
        .pollInterval(java.time.Duration.ofMillis(20))
        .untilAsserted(
            () ->
                assertThat(
                        jdbc.queryForObject(
                            "SELECT count(*) FROM pg_stat_activity WHERE application_name=? AND cardinality(pg_blocking_pids(pid))>0",
                            Integer.class,
                            name))
                    .isEqualTo(1));
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Barrier not reached");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError(interrupted);
    }
  }

  private static String sha(byte[] bytes) {
    try {
      return java.util.HexFormat.of()
          .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new AssertionError(impossible);
    }
  }
}
