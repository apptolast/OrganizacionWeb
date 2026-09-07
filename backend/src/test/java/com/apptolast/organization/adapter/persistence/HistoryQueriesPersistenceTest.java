package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.SessionStart;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class HistoryQueriesPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static final com.fasterxml.jackson.databind.ObjectMapper json =
      new com.fasterxml.jackson.databind.ObjectMapper()
          .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
  UUID project;
  UUID task;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
  }

  @BeforeEach
  void context() {
    jdbc.execute(
        "TRUNCATE work_session_intervals,work_session_changes,work_sessions,block_changes,block_projections,planned_blocks,task_status_history,tasks,outbox_events,projects");
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Entrega','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Publicar','','pending',now(),now())",
        task,
        project);
  }

  @Test
  void s1_readsTheOriginalStartWithCurrentLabelsFromPostgres() {
    var id = UUID.randomUUID();
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var start = new SessionStart(id, project, task, at, 25, at.plusSeconds(1500), "UTC");
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,changed_at,running_since) VALUES (?,'owner',?,?,?,?,25,?,'UTC','running',?,?)",
        id,
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(start.plannedEndAt()),
        Timestamp.from(at),
        Timestamp.from(at));
    var before = jdbc.queryForList("SELECT * FROM work_sessions");

    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", new HistoryFilters(null, null, null, null, null), null);

    assertThat(rows)
        .containsExactly(
            new HistoryEntry<>(
                id, "SESSION_STARTED", at, project, "Entrega", task, "Publicar", start));
    assertThat(jdbc.queryForList("SELECT * FROM work_sessions")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }

  @Test
  void s3_anotherOwnerCannotReadASessionStart() {
    var at = Instant.parse("2026-09-07T10:00:00Z");
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'owner',?,?,?,?,25,?,'UTC','running')",
        UUID.randomUUID(),
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(at.plusSeconds(1500)));
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("other-owner", new HistoryFilters(null, null, null, null, null), null))
        .isEmpty();
  }

  @Test
  void s1_readsATaskTransitionAsItsFourFieldHistoryFact() {
    var id = UUID.randomUUID();
    var at = Instant.parse("2026-09-07T11:00:00.123456Z");
    jdbc.update(
        "UPDATE tasks SET status='completed',version=1,updated_at=?,completed_at=? WHERE id=?",
        Timestamp.from(at),
        Timestamp.from(at),
        task);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',?)",
        id,
        project,
        task,
        Timestamp.from(at));
    var expected =
        new com.apptolast.organization.domain.TaskHistoryEntry(id, 1, "pending", "completed", at);
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters(null, null, null, null, null), null))
        .containsExactly(
            new HistoryEntry<>(
                id, "TASK_STATUS_CHANGED", at, project, "Entrega", task, "Publicar", expected));
  }

  @Test
  void s1_readsADurablePauseAlongsideItsOriginalStart() throws Exception {
    var at = Instant.parse("2026-09-07T10:00:00Z");
    var id = UUID.randomUUID();
    var start = new SessionStart(id, project, task, at, 25, at.plusSeconds(1500), "UTC");
    var before =
        new com.apptolast.organization.domain.WorkSessionState(start, "running", 1, at, 0, at);
    var after =
        new com.apptolast.organization.domain.WorkSessionState(
            start, "paused", 2, at.plusSeconds(1), 1000000, null);
    var change =
        new WorkSessionTransitionReceipt(
            UUID.randomUUID(), id, "PAUSE", after.changedAt(), before, after);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds) VALUES (?,'owner',?,?,?,?,25,?,'UTC','paused',2,?,1000000)",
        id,
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(start.plannedEndAt()),
        Timestamp.from(after.changedAt()));
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,2,?,?)",
        id,
        Timestamp.from(at),
        Timestamp.from(after.changedAt()));
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,'owner',?,?,'PAUSE',1,?,?::jsonb)",
        change.id(),
        id,
        UUID.randomUUID(),
        Timestamp.from(change.occurredAt()),
        json.writeValueAsString(change));
    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", new HistoryFilters(null, null, null, null, null), null);
    assertThat(rows).hasSize(2);
    assertThat(rows)
        .filteredOn(e -> e.type().equals("SESSION_CHANGED"))
        .containsExactly(
            new HistoryEntry<>(
                change.id(),
                "SESSION_CHANGED",
                change.occurredAt(),
                project,
                "Entrega",
                task,
                "Publicar",
                change));
    assertThat(rows)
        .filteredOn(e -> e.type().equals("SESSION_STARTED"))
        .extracting(HistoryEntry::details)
        .isEqualTo(java.util.List.of(start));
  }

  @Test
  void s1_plannedHistoryUsesCreationTimeAndTheOriginalDestination() {
    var block = plannedBlock();
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters(null, null, null, null, null), null))
        .containsExactly(
            new HistoryEntry<>(
                block.id(),
                "BLOCK_PLANNED",
                block.createdAt(),
                project,
                "Entrega",
                task,
                "Publicar",
                block));
  }

  private com.apptolast.organization.domain.PlannedBlock plannedBlock() {
    var id = UUID.randomUUID();
    var created = Instant.parse("2026-09-07T10:00:00.123456Z");
    var start = Instant.parse("2026-10-01T11:00:00Z");
    var end = start.plusSeconds(3600);
    var offset = java.time.ZoneOffset.UTC;
    var request =
        new com.apptolast.organization.domain.BlockRequest(
            "Preparar entrega",
            java.time.LocalDateTime.ofInstant(start, offset),
            java.time.LocalDateTime.ofInstant(end, offset),
            "UTC",
            offset,
            offset,
            false);
    var time =
        new com.apptolast.organization.domain.ResolvedBlockTime(start, end, offset, offset, 60);
    var block =
        new com.apptolast.organization.domain.PlannedBlock(
            id, project, task, request, time, created);
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,?,?,?,'UTC','Z','Z',false,?,?,60,?)",
        id,
        project,
        task,
        UUID.randomUUID(),
        request.objective(),
        request.startLocal(),
        request.endLocal(),
        Timestamp.from(start),
        Timestamp.from(end),
        Timestamp.from(created));
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,1,'planned',?)",
        id,
        Timestamp.from(created));
    return block;
  }

  @Test
  void s1_cancelledBlockRetainsItsPlanAndImmutableCancellation() throws Exception {
    var block = plannedBlock();
    var at = block.createdAt().plusSeconds(60);
    var receipt =
        new com.apptolast.organization.domain.BlockChangeReceipt(
            UUID.randomUUID(), block.id(), "CANCELLED", 2, at, block, null);
    jdbc.update(
        "UPDATE block_projections SET status='cancelled',version=2,updated_at=? WHERE block_id=?",
        Timestamp.from(at),
        block.id());
    jdbc.update(
        "INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt) VALUES (?,?,?,?,?,'CANCELLED',2,?,?::jsonb)",
        receipt.id(),
        project,
        task,
        block.id(),
        UUID.randomUUID(),
        Timestamp.from(at),
        json.writeValueAsString(receipt));
    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", new HistoryFilters(null, null, null, null, null), null);
    assertThat(rows)
        .containsExactlyInAnyOrder(
            new HistoryEntry<>(
                block.id(),
                "BLOCK_PLANNED",
                block.createdAt(),
                project,
                "Entrega",
                task,
                "Publicar",
                block),
            new HistoryEntry<>(
                receipt.id(), "BLOCK_CHANGED", at, project, "Entrega", task, "Publicar", receipt));
  }

  @Test
  void s6_databaseReturnsOnlyTwentyOneFactsInDescendingTime() {
    var at = Instant.parse("2026-09-07T10:00:00Z");
    var expected = new java.util.ArrayList<UUID>();
    for (int version = 1; version <= 22; version++) {
      var id = UUID.randomUUID();
      expected.addFirst(id);
      jdbc.update(
          "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,?,?,?,?)",
          id,
          project,
          task,
          version,
          version % 2 == 1 ? "pending" : "completed",
          version % 2 == 1 ? "completed" : "pending",
          Timestamp.from(at.plusSeconds(version)));
    }
    jdbc.update(
        "UPDATE tasks SET version=22,updated_at=? WHERE id=?",
        Timestamp.from(at.plusSeconds(22)),
        task);
    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", new HistoryFilters(null, null, null, null, null), null);
    assertThat(rows).extracting(HistoryEntry::id).isEqualTo(expected.subList(0, 21));
  }

  @Test
  void s13_equalTimesUseSourceRankThenUnsignedUuidDescending() {
    var block = plannedBlock();
    var at = block.createdAt();
    var low = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var high = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',?),(?,?,?,2,'completed','pending',?)",
        low,
        project,
        task,
        Timestamp.from(at),
        high,
        project,
        task,
        Timestamp.from(at));
    jdbc.update("UPDATE tasks SET version=2,updated_at=? WHERE id=?", Timestamp.from(at), task);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'owner',?,?,?,?,25,?,'UTC','running')",
        block.id(),
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(at.plusSeconds(1500)));
    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", new HistoryFilters(null, null, null, null, null), null);
    assertThat(rows)
        .extracting(HistoryEntry::type)
        .containsExactly(
            "SESSION_STARTED", "TASK_STATUS_CHANGED", "TASK_STATUS_CHANGED", "BLOCK_PLANNED");
    assertThat(rows)
        .extracting(HistoryEntry::id)
        .containsExactly(block.id(), high, low, block.id());
  }

  @Test
  void s7_sessionsCategoryExcludesPlanning() {
    var block = plannedBlock();
    var at = block.createdAt();
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'owner',?,?,?,?,25,?,'UTC','running')",
        id,
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(at.plusSeconds(1500)));
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters("sessions", null, null, null, null), null))
        .extracting(HistoryEntry::id)
        .containsExactly(id);
  }

  @Test
  void s7_planningCategoryIncludesTheOriginalPlan() {
    var block = plannedBlock();
    var at = block.createdAt();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'owner',?,?,?,?,25,?,'UTC','running')",
        UUID.randomUUID(),
        project,
        task,
        UUID.randomUUID(),
        Timestamp.from(at),
        Timestamp.from(at.plusSeconds(1500)));
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters("planning", null, null, null, null), null))
        .extracting(HistoryEntry::id)
        .containsExactly(block.id());
  }

  @Test
  void s7_taskStatusCategoryExcludesPlanning() {
    var block = plannedBlock();
    var id = UUID.randomUUID();
    var at = block.createdAt();
    jdbc.update(
        "UPDATE tasks SET status='completed',version=1,completed_at=?,updated_at=? WHERE id=?",
        Timestamp.from(at),
        Timestamp.from(at),
        task);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',?)",
        id,
        project,
        task,
        Timestamp.from(at));
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters("task-status", null, null, null, null), null))
        .extracting(HistoryEntry::id)
        .containsExactly(id);
  }

  @Test
  void s9_unknownExplicitProjectIsNotAnEmptyHistory() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list(
                        "owner",
                        new HistoryFilters(null, UUID.randomUUID(), null, null, null),
                        null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void s9_taskOfAnotherOwnedProjectIsNotTheRequestedContext() {
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Otro','','idea',now(),now())",
        other);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list("owner", new HistoryFilters(null, other, task, null, null), null))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void s8_ownedIdeaProjectWithoutFactsDoesNotLeakAnotherProjectsFacts() {
    plannedBlock();
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Vacío','','idea',now(),now())",
        other);
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters(null, other, null, null, null), null))
        .isEmpty();
  }

  @Test
  void s8_completedTaskWithoutFactsDoesNotLeakItsSiblingsHistory() {
    plannedBlock();
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,completed_at) VALUES (?,?,'Terminada','','completed',now(),now(),now())",
        other,
        project);
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters(null, project, other, null, null), null))
        .isEmpty();
  }

  @Test
  void s17_cursorOwnerMustMatchAfterOwnedContextIsConfirmed() {
    var block = plannedBlock();
    var filters = new HistoryFilters(null, project, task, null, null);
    var position = new HistoryPosition(block.createdAt(), "BLOCK_PLANNED", block.id());
    var cursor = new HistoryCursor("other-owner", filters, position, position);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list("owner", filters, cursor))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.ValidationException.class,
            e ->
                assertThat(e.errors())
                    .extracting(
                        com.apptolast.organization.domain.FieldError::field,
                        com.apptolast.organization.domain.FieldError::code)
                    .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("cursor", "INVALID_VALUE")));
  }

  @Test
  void s16_foreignProjectPrecedesCursorOwnerMismatch() {
    var filters = new HistoryFilters(null, project, task, null, null);
    var position =
        new HistoryPosition(
            Instant.parse("2026-09-07T10:00:00Z"), "BLOCK_PLANNED", UUID.randomUUID());
    var cursor = new HistoryCursor("owner", filters, position, position);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list("other-owner", filters, cursor))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void s17_cursorFiltersCannotBeReboundToAnotherCategory() {
    var block = plannedBlock();
    var filters = new HistoryFilters("planning", project, task, null, null);
    var position = new HistoryPosition(block.createdAt(), "BLOCK_PLANNED", block.id());
    var cursor =
        new HistoryCursor(
            "owner", new HistoryFilters(null, project, task, null, null), position, position);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list("owner", filters, cursor))
        .isInstanceOf(com.apptolast.organization.domain.ValidationException.class);
  }

  @Test
  void s17_afterCannotBeAboveUpperAtTheSameInstant() {
    var block = plannedBlock();
    var filters = new HistoryFilters(null, project, task, null, null);
    var upper = new HistoryPosition(block.createdAt(), "BLOCK_PLANNED", block.id());
    var after = new HistoryPosition(block.createdAt(), "SESSION_STARTED", block.id());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(
                        jdbc,
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                            jdbc.getDataSource()),
                        json)
                    .list("owner", filters, new HistoryCursor("owner", filters, upper, after)))
        .isInstanceOf(com.apptolast.organization.domain.ValidationException.class);
  }

  @Test
  void s14_keysetIsStrictAcrossUuidAndFamilyTies() {
    var block = plannedBlock();
    var at = block.createdAt();
    var low = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var high = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',?),(?,?,?,2,'completed','pending',?)",
        low,
        project,
        task,
        Timestamp.from(at),
        high,
        project,
        task,
        Timestamp.from(at));
    jdbc.update("UPDATE tasks SET version=2,updated_at=? WHERE id=?", Timestamp.from(at), task);
    var filters = new HistoryFilters(null, null, null, null, null);
    var position = new HistoryPosition(at, "TASK_STATUS_CHANGED", high);
    var rows =
        new PostgresHistoryQueries(
                jdbc,
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                    jdbc.getDataSource()),
                json)
            .list("owner", filters, new HistoryCursor("owner", filters, position, position));
    assertThat(rows).extracting(HistoryEntry::id).containsExactly(low, block.id());
  }

  @Test
  void s10_dateBoundsIncludeTheWholeUtcDayAtMicrosecondPrecision() {
    var instants =
        java.util.List.of(
            Instant.parse("2026-09-06T23:59:59.999999Z"),
            Instant.parse("2026-09-07T00:00:00Z"),
            Instant.parse("2026-09-07T23:59:59.999999Z"),
            Instant.parse("2026-09-08T00:00:00Z"));
    var ids = new java.util.ArrayList<UUID>();
    for (int i = 0; i < instants.size(); i++) {
      var id = UUID.randomUUID();
      ids.add(id);
      jdbc.update(
          "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,?,?,?,?)",
          id,
          project,
          task,
          i + 1,
          i % 2 == 0 ? "pending" : "completed",
          i % 2 == 0 ? "completed" : "pending",
          Timestamp.from(instants.get(i)));
    }
    jdbc.update(
        "UPDATE tasks SET version=4,updated_at=? WHERE id=?",
        Timestamp.from(instants.getLast()),
        task);
    var day = java.time.LocalDate.of(2026, 9, 7);
    assertThat(
            new PostgresHistoryQueries(
                    jdbc,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    json)
                .list("owner", new HistoryFilters(null, null, null, day, day), null))
        .extracting(HistoryEntry::id)
        .containsExactly(ids.get(2), ids.get(1));
  }
}
