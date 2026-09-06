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
class RescheduleCoordinationTest {
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
        "TRUNCATE"
            + " block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
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
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,?,'Project','','active',?,?)",
        project,
        owner,
        java.sql.Timestamp.from(NOW),
        java.sql.Timestamp.from(NOW));
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Task','','pending',?,?)",
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
          "INSERT INTO"
              + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
              + " VALUES (?,?,'UTC',120,120,120,120,120,120,120,0,?,?)",
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

  void awaitWaiting(String fragment, int expected) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
    int waiting;
    do {
      waiting =
          jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND"
                  + " wait_event_type='Lock' AND query LIKE ?",
              Integer.class,
              "%" + fragment + "%");
      if (waiting >= expected) return;
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    assertThat(waiting)
        .as("database sessions waiting on %s", fragment)
        .isGreaterThanOrEqualTo(expected);
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

  @Test
  void s23_projectCompletionFirstRejectsWaitingMove() throws Exception {
    rejectsAfter("project", ProjectCompletedException.class);
  }

  @Test
  void s23_taskCompletionFirstRejectsWaitingMove() throws Exception {
    rejectsAfter("task", TaskCompletedException.class);
  }

  @Test
  void s23_availabilityFirstRejectsWaitingMoveWithOldRevision() throws Exception {
    rejectsAfter("availability", AvailabilityConflictException.class);
  }

  void rejectsAfter(String kind, Class<? extends RuntimeException> expected) throws Exception {
    var original = create(context, UUID.randomUUID(), request("10:00", "11:00", false)).block();
    var before = jdbc.queryForList("SELECT * FROM planned_blocks");
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
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
                          var result = change(kind);
                          locked.countDown();
                          await(release);
                          return result;
                        }));
        await(locked);
        var second =
            workers.submit(
                () -> {
                  try {
                    return move(original.id());
                  } catch (RuntimeException failure) {
                    return failure;
                  }
                });
        awaitWaiting(
            kind.equals("availability")
                ? "availability_preferences"
                : kind.equals("project") ? "projects" : "tasks",
            1);
        assertThat(second.isDone()).isFalse();
        release.countDown();
        first.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(second.get(10, java.util.concurrent.TimeUnit.SECONDS)).isInstanceOf(expected);
        if (kind.equals("availability")) {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT version FROM availability_preferences WHERE id=?",
                      Long.class,
                      context.preference()))
              .isEqualTo(1);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT zone_id FROM availability_preferences WHERE id=?",
                      String.class,
                      context.preference()))
              .isEqualTo("Europe/Madrid");
        } else {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT status FROM "
                          + (kind.equals("project") ? "projects" : "tasks")
                          + " WHERE id=?",
                      String.class,
                      kind.equals("project") ? context.project() : context.task()))
              .isEqualTo("completed");
        }
        assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class))
            .isZero();
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
                    Long.class))
            .isZero();
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void s23_moveFirstThenProjectCompletionPreservesReservation() throws Exception {
    preservesBefore("project");
  }

  @Test
  void s23_moveFirstThenTaskCompletionPreservesReservation() throws Exception {
    preservesBefore("task");
  }

  @Test
  void s23_moveFirstThenAvailabilityPreservesInstants() throws Exception {
    preservesBefore("availability");
  }

  void preservesBefore(String kind) throws Exception {
    var original = create(context, UUID.randomUUID(), request("10:00", "11:00", false)).block();
    var before = jdbc.queryForList("SELECT * FROM planned_blocks");
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
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
                          var result = move(original.id());
                          locked.countDown();
                          await(release);
                          return result;
                        }));
        await(locked);
        var second = workers.submit(() -> change(kind));
        awaitWaiting(
            kind.equals("availability")
                ? "availability_preferences"
                : kind.equals("project") ? "projects" : "tasks",
            1);
        assertThat(second.isDone()).isFalse();
        release.countDown();
        var confirmation =
            (BlockChangeConfirmation) first.get(10, java.util.concurrent.TimeUnit.SECONDS);
        second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(confirmation.replayed()).isFalse();
        assertThat(confirmation.receipt().kind()).isEqualTo("RESCHEDULED");
        assertThat(confirmation.receipt().version()).isEqualTo(2);
        assertThat(confirmation.receipt().before()).isEqualTo(original);
        assertThat(confirmation.receipt().after().time().startAt())
            .isEqualTo(Instant.parse("2030-01-07T12:00:00Z"));
        if (kind.equals("availability")) {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT version FROM availability_preferences WHERE id=?",
                      Long.class,
                      context.preference()))
              .isEqualTo(1);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT zone_id FROM availability_preferences WHERE id=?",
                      String.class,
                      context.preference()))
              .isEqualTo("Europe/Madrid");
        } else {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT status FROM "
                          + (kind.equals("project") ? "projects" : "tasks")
                          + " WHERE id=?",
                      String.class,
                      kind.equals("project") ? context.project() : context.task()))
              .isEqualTo("completed");
        }
        assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(before);
        assertThat(store.detail(context.owner(), context.project(), context.task(), original.id()))
            .isEqualTo(confirmation.receipt().after());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class))
            .isEqualTo(1);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM block_projections WHERE status='planned' AND version=2",
                    Long.class))
            .isEqualTo(1);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
                    Long.class))
            .isEqualTo(1);
      } finally {
        release.countDown();
      }
    }
  }

  Object move(UUID block) {
    return new MoveBlock(store, ZoneId::getAvailableZoneIds, Clock.fixed(NOW, ZoneOffset.UTC))
        .move(
            context.owner(),
            context.project(),
            context.task(),
            block,
            UUID.randomUUID(),
            1,
            new AvailabilityRevision(context.preference(), 0),
            new BlockMoveRequest(
                LocalDateTime.parse("2030-01-07T12:00"),
                LocalDateTime.parse("2030-01-07T13:00"),
                "UTC",
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                false));
  }
}
