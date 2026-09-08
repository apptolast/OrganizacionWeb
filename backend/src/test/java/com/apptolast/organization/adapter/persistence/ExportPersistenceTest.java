package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class ExportPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
  }

  @Test
  void s1_s4_emptyExportUsesAReadOnlyRepeatableSnapshotBeforeClockWithoutWriting()
      throws Exception {
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare(
                "owner-a",
                () -> {
                  assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                      .isEqualTo("repeatable read");
                  assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                      .isEqualTo("on");
                  assertThat(
                          jdbc.queryForObject("SELECT pg_current_snapshot()::text", String.class))
                      .isNotBlank();
                  return Instant.parse("2026-09-08T01:02:03Z");
                });
    var bytes = new ByteArrayOutputStream();
    file.writeTo(bytes);
    var document = new ObjectMapper().readTree(bytes.toByteArray());
    assertThat(document.path("data").size()).isEqualTo(14);
    for (var count : document.path("counts")) assertThat(count.intValue()).isZero();
    for (var table :
        java.util.List.of(
            "projects",
            "outbox_events",
            "availability_preferences",
            "appearance_preferences",
            "customization_preferences"))
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM " + table + " WHERE owner_id='owner-a'", Integer.class))
          .isZero();
  }

  @Test
  void s2_projectsAreClosedOwnerFilteredAndOrderedWithoutNormalizingStoredText() throws Exception {
    var owner = "projects-owner";
    var first = java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc");
    var second = java.util.UUID.fromString("22345678-1234-1234-1234-123456789abc");
    for (var id : java.util.List.of(second, first))
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,?,'idea',9007199254740993,'0001-01-01T00:00:00Z','2026-09-08T01:02:03.123456Z')",
          id,
          owner,
          "  Nombre 🧭  ",
          "<script>\n=SUM(1)");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'AJENO','','idea',0,now(),now())",
        java.util.UUID.randomUUID(),
        "another-owner");
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03Z"));
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("projects").intValue()).isEqualTo(2);
    var rows = json.path("data").path("projects");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).path("id").asText()).isEqualTo(first.toString());
    assertThat(rows.get(1).path("id").asText()).isEqualTo(second.toString());
    assertThat(rows.get(0).properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id", "name", "description", "status", "version", "createdAt", "updatedAt");
    assertThat(rows.get(0).path("name").asText()).isEqualTo("  Nombre 🧭  ");
    assertThat(rows.get(0).path("description").asText()).isEqualTo("<script>\n=SUM(1)");
    assertThat(rows.get(0).path("version").isTextual()).isTrue();
    assertThat(rows.get(0).path("version").asText()).isEqualTo("9007199254740993");
    assertThat(rows.get(0).path("createdAt").asText()).isEqualTo("0001-01-01T00:00:00.000000Z");
    assertThat(output.toString(java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("AJENO");
  }

  @Test
  void s15_recordExcessIsRejectedBeforeReadingPayloadOrClock() {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) SELECT md5(i::text)::uuid,'large-owner','P','','idea',0,now(),now() FROM generate_series(1,100001) i");
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    java.util.function.Supplier<Instant> clock =
        () -> {
          calls.incrementAndGet();
          return Instant.EPOCH;
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new PostgresExportDataQueries(jdbc, manager).prepare("large-owner", clock))
        .isInstanceOf(com.apptolast.organization.application.ExportTooLargeException.class);
    assertThat(calls.get()).isZero();
  }

  @Test
  void s14_clockFailureDoesNotProduceAFileAndReleasesTheTransaction() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "clock-owner",
                        () -> {
                          throw new IllegalStateException("private clock cause");
                        }))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
        .hasMessage("El almacenamiento no está disponible.");
    assertThat(
            org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive())
        .isFalse();
  }

  @Test
  void s2_tasksIncludeSubtaskLinksAndCompletedFactsWithExactFields() throws Exception {
    var project = java.util.UUID.randomUUID();
    var parent = java.util.UUID.fromString("12345678-0000-0000-0000-000000000001");
    var child = java.util.UUID.fromString("12345678-0000-0000-0000-000000000002");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'tasks-owner','Proyecto','','idea',0,now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,version,created_at,updated_at) VALUES (?,?,'Padre','',null,'pending',0,'2026-09-01Z','2026-09-01Z')",
        parent,
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,estimated_minutes,status,version,completed_at,created_at,updated_at) VALUES (?,?,?,' Hija ',' Resultado ',25,'completed',7,'2026-09-02Z','2026-09-01Z','2026-09-02Z')",
        child,
        project,
        parent);
    var file =
        new PostgresExportDataQueries(jdbc, manager).prepare("tasks-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("tasks").intValue()).isEqualTo(2);
    var task = json.path("data").path("tasks").get(1);
    assertThat(task.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id",
            "projectId",
            "parentId",
            "title",
            "completionCriterion",
            "estimatedMinutes",
            "status",
            "version",
            "completedAt",
            "createdAt",
            "updatedAt");
    assertThat(task.path("id").asText()).isEqualTo(child.toString());
    assertThat(task.path("projectId").asText()).isEqualTo(project.toString());
    assertThat(task.path("parentId").asText()).isEqualTo(parent.toString());
    assertThat(task.path("title").asText()).isEqualTo(" Hija ");
    assertThat(task.path("completionCriterion").asText()).isEqualTo(" Resultado ");
    assertThat(task.path("estimatedMinutes").intValue()).isEqualTo(25);
    assertThat(task.path("status").asText()).isEqualTo("completed");
    assertThat(task.path("version").asText()).isEqualTo("7");
    assertThat(task.path("completedAt").asText()).isEqualTo("2026-09-02T00:00:00.000000Z");
    assertThat(json.path("data").path("tasks").get(0).path("estimatedMinutes").isNull()).isTrue();
    assertThat(json.path("data").path("tasks").get(0).path("parentId").isNull()).isTrue();
  }

  @Test
  void s2_taskStatusHistoryKeepsItsOwnTaskVersionAndTransition() throws Exception {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    var history = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'history-owner','P','','idea',0,now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'T','','pending',8,now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,7,'pending','completed','2026-09-08T01:02:03.123456Z')",
        history,
        project,
        task);
    var file =
        new PostgresExportDataQueries(jdbc, manager).prepare("history-owner", () -> Instant.EPOCH);
    var bytes = new ByteArrayOutputStream();
    file.writeTo(bytes);
    var json = new ObjectMapper().readTree(bytes.toByteArray());
    assertThat(json.path("counts").path("taskStatusHistory").intValue()).isEqualTo(1);
    var row = json.path("data").path("taskStatusHistory").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id", "projectId", "taskId", "taskVersion", "fromStatus", "toStatus", "occurredAt");
    assertThat(row.path("id").asText()).isEqualTo(history.toString());
    assertThat(row.path("projectId").asText()).isEqualTo(project.toString());
    assertThat(row.path("taskId").asText()).isEqualTo(task.toString());
    assertThat(row.path("taskVersion").asText()).isEqualTo("7");
    assertThat(row.path("fromStatus").asText()).isEqualTo("pending");
    assertThat(row.path("toStatus").asText()).isEqualTo("completed");
    assertThat(row.path("occurredAt").asText()).isEqualTo("2026-09-08T01:02:03.123456Z");
  }

  @Test
  void s2_availabilityExportsStoredBudgetsWithoutCreatingDefaults() throws Exception {
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'availability-owner','Europe/Madrid',0,1,30,60,120,1440,90,9223372036854775807,'2026-09-01Z','2026-09-02Z')",
        id);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("availability-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("availability").intValue()).isEqualTo(1);
    var row = json.path("data").path("availability").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id",
            "zoneId",
            "mondayMinutes",
            "tuesdayMinutes",
            "wednesdayMinutes",
            "thursdayMinutes",
            "fridayMinutes",
            "saturdayMinutes",
            "sundayMinutes",
            "version",
            "createdAt",
            "updatedAt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("zoneId").asText()).isEqualTo("Europe/Madrid");
    assertThat(row.path("mondayMinutes").intValue()).isZero();
    assertThat(row.path("saturdayMinutes").intValue()).isEqualTo(1440);
    assertThat(row.path("version").isTextual()).isTrue();
    assertThat(row.path("version").asText()).isEqualTo("9223372036854775807");
  }

  @Test
  void s2_appearanceKeepsTheSavedPreferenceAndDoesNotExportItsOwnerColumn() throws Exception {
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO appearance_preferences(id,owner_id,theme,accent_light,accent_dark,version,updated_at) VALUES (?,'appearance-owner','DARK','#0000FF','#00FFFF',8,'2026-09-08T01:02:03.123456Z')",
        id);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("appearance-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("appearance").intValue()).isEqualTo(1);
    var row = json.path("data").path("appearance").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "theme", "accentLight", "accentDark", "version", "updatedAt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("theme").asText()).isEqualTo("DARK");
    assertThat(row.path("accentLight").asText()).isEqualTo("#0000FF");
    assertThat(row.path("accentDark").asText()).isEqualTo("#00FFFF");
    assertThat(row.path("version").asText()).isEqualTo("8");
    assertThat(row.path("updatedAt").asText()).isEqualTo("2026-09-08T01:02:03.123456Z");
  }

  @Test
  void s6_configurationKeepsColumnAndDefinitionOrderIncludingInactiveFields() throws Exception {
    var field = java.util.UUID.randomUUID();
    var raw =
        "[{\"id\":\"" + field + "\",\"label\":\"Oculto\",\"type\":\"TEXT\",\"active\":false}]";
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'configuration-owner','TASK','[\"estimatedMinutes\",\"completionCriterion\"]'::jsonb,?::jsonb,7,'2026-09-08Z')",
        java.util.UUID.fromString("12345678-0000-0000-0000-000000000001"),
        raw);
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'configuration-owner','PROJECT','[]'::jsonb,'[]'::jsonb,1,'2026-09-08Z')",
        java.util.UUID.fromString("22345678-0000-0000-0000-000000000001"));
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("configuration-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    var collection = json.path("data").path("customization");
    assertThat(json.path("counts").path("customization").intValue()).isEqualTo(2);
    assertThat(collection.get(0).path("scope").asText()).isEqualTo("PROJECT");
    var task = collection.get(1);
    assertThat(task.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "scope", "visibleFields", "customFields", "version", "updatedAt");
    assertThat(task.path("scope").asText()).isEqualTo("TASK");
    assertThat(task.path("visibleFields").get(0).asText()).isEqualTo("estimatedMinutes");
    assertThat(task.path("visibleFields").get(1).asText()).isEqualTo("completionCriterion");
    assertThat(task.path("version").asText()).isEqualTo("7");
    assertThat(task.path("customFields").get(0).properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "label", "type", "active");
    assertThat(task.path("customFields").get(0).path("active").booleanValue()).isFalse();
    assertThat(task.path("customFields").get(0).path("id").asText()).isEqualTo(field.toString());
  }

  @Test
  void s6_projectValuesPreserveInactiveZeroFalseAndExplicitNullButNotAbsentFields()
      throws Exception {
    var project = java.util.UUID.randomUUID();
    var a = "12345678-0000-0000-0000-000000000001";
    var b = "22345678-0000-0000-0000-000000000001";
    var c = "32345678-0000-0000-0000-000000000001";
    var d = "42345678-0000-0000-0000-000000000001";
    var e = "52345678-0000-0000-0000-000000000001";
    var f = "62345678-0000-0000-0000-000000000001";
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'values-owner','P','','idea',0,now(),now())",
        project);
    var definitions = new ObjectMapper().createArrayNode();
    for (var pair :
        java.util.List.of(
            new String[] {a, "NUMBER"},
            new String[] {b, "BOOLEAN"},
            new String[] {c, "TEXT"},
            new String[] {d, "DATE"},
            new String[] {e, "DATE"},
            new String[] {f, "TEXT"}))
      definitions
          .addObject()
          .put("id", pair[0])
          .put("label", pair[1] + pair[0])
          .put("type", pair[1])
          .put("active", false);
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'values-owner','PROJECT','[]'::jsonb,?::jsonb,1,now())",
        java.util.UUID.randomUUID(),
        definitions.toString());
    var values =
        new ObjectMapper()
            .createObjectNode()
            .put(b, false)
            .putNull(c)
            .put(a, 0)
            .put(e, "0001-01-01")
            .put(f, "  Texto 🧭\nexacto  ");
    jdbc.update(
        "INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at) VALUES (?,'values-owner',?,?::jsonb,7,'2026-09-08Z')",
        java.util.UUID.randomUUID(),
        project,
        values.toString());
    var file =
        new PostgresExportDataQueries(jdbc, manager).prepare("values-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("projectCustomFieldValues").intValue()).isEqualTo(1);
    var row = json.path("data").path("projectCustomFieldValues").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "projectId", "values", "version", "updatedAt");
    assertThat(row.path("projectId").asText()).isEqualTo(project.toString());
    assertThat(row.path("version").asText()).isEqualTo("7");
    var exported = row.path("values");
    assertThat(exported).hasSize(5);
    assertThat(exported.get(3).path("fieldId").asText()).isEqualTo(e);
    assertThat(exported.get(3).path("value").asText()).isEqualTo("0001-01-01");
    assertThat(exported.get(4).path("value").asText()).isEqualTo("  Texto 🧭\nexacto  ");
    assertThat(exported.get(0).path("fieldId").asText()).isEqualTo(a);
    assertThat(exported.get(0).path("value").isIntegralNumber()).isTrue();
    assertThat(exported.get(0).path("value").intValue()).isZero();
    assertThat(exported.get(1).path("value").isBoolean()).isTrue();
    assertThat(exported.get(1).path("value").booleanValue()).isFalse();
    assertThat(exported.get(2).path("value").isNull()).isTrue();
    assertThat(exported.get(0).properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("fieldId", "value");
  }

  @Test
  void s2_taskValuesIncludeTheProjectDerivedFromTheirTask() throws Exception {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    var values = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'task-values-owner','P','','idea',0,now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'T','','pending',0,now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO task_custom_field_values(id,owner_id,task_id,field_values,version,updated_at) VALUES (?,'task-values-owner',?,'{}'::jsonb,0,'2026-09-08Z')",
        values,
        task);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("task-values-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("taskCustomFieldValues").intValue()).isEqualTo(1);
    var row = json.path("data").path("taskCustomFieldValues").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "projectId", "taskId", "values", "version", "updatedAt");
    assertThat(row.path("id").asText()).isEqualTo(values.toString());
    assertThat(row.path("projectId").asText()).isEqualTo(project.toString());
    assertThat(row.path("taskId").asText()).isEqualTo(task.toString());
    assertThat(row.path("values").isArray()).isTrue();
    assertThat(row.path("values").size()).isZero();
    assertThat(json.path("counts").path("customization").intValue()).isZero();
  }

  private record TaskFixture(java.util.UUID project, java.util.UUID task) {}

  private TaskFixture task(String owner) {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'P','','idea',0,'2026-09-01Z','2026-09-01Z')",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'T','','pending',0,'2026-09-01Z','2026-09-01Z')",
        task,
        project);
    return new TaskFixture(project, task);
  }

  private java.util.UUID block(TaskFixture context) {
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,?,'2026-09-08T16:00:00','2026-09-08T16:30:00','Europe/Madrid','+02:00','+02:00',true,'2026-09-08T14:00:00Z','2026-09-08T14:30:00Z',30,'2026-09-01Z')",
        id,
        context.project(),
        context.task(),
        id,
        " Objetivo 🧭 ");
    return id;
  }

  @Test
  void s8_originalBlockKeepsItsIntentionWithoutFabricatingAProjection() throws Exception {
    var context = task("block-owner");
    var id = block(context);
    var file =
        new PostgresExportDataQueries(jdbc, manager).prepare("block-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("plannedBlocks").intValue()).isEqualTo(1);
    assertThat(json.path("counts").path("blockProjections").intValue()).isZero();
    var row = json.path("data").path("plannedBlocks").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id",
            "projectId",
            "taskId",
            "requestKey",
            "objective",
            "startLocal",
            "endLocal",
            "zoneId",
            "startOffset",
            "endOffset",
            "allowOverBudget",
            "startAt",
            "endAt",
            "durationMinutes",
            "createdAt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("projectId").asText()).isEqualTo(context.project().toString());
    assertThat(row.path("taskId").asText()).isEqualTo(context.task().toString());
    assertThat(row.path("requestKey").asText()).isEqualTo(id.toString());
    assertThat(row.path("objective").asText()).isEqualTo(" Objetivo 🧭 ");
    assertThat(row.path("startLocal").asText()).isEqualTo("2026-09-08T16:00:00");
    assertThat(row.path("startAt").asText()).isEqualTo("2026-09-08T14:00:00.000000Z");
    assertThat(row.path("startOffset").asText()).isEqualTo("+02:00");
    assertThat(row.path("zoneId").asText()).isEqualTo("Europe/Madrid");
    assertThat(row.path("durationMinutes").intValue()).isEqualTo(30);
    assertThat(row.path("allowOverBudget").booleanValue()).isTrue();
  }

  @Test
  void s8_cancelledProjectionRetainsVersionAndExplicitNullTimes() throws Exception {
    var context = task("projection-owner");
    var id = block(context);
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,7,'cancelled','2026-09-08T12:00:00Z')",
        id);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("projection-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("blockProjections").intValue()).isEqualTo(1);
    assertThat(json.path("counts").path("plannedBlocks").intValue()).isEqualTo(1);
    var row = json.path("data").path("blockProjections").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "blockId",
            "version",
            "status",
            "updatedAt",
            "startLocal",
            "endLocal",
            "zoneId",
            "startOffset",
            "endOffset",
            "startAt",
            "endAt",
            "durationMinutes");
    assertThat(row.path("blockId").asText()).isEqualTo(id.toString());
    assertThat(row.path("version").isTextual()).isTrue();
    assertThat(row.path("version").asText()).isEqualTo("7");
    assertThat(row.path("status").asText()).isEqualTo("cancelled");
    assertThat(row.path("updatedAt").asText()).isEqualTo("2026-09-08T12:00:00.000000Z");
    for (var field :
        java.util.List.of(
            "startLocal",
            "endLocal",
            "zoneId",
            "startOffset",
            "endOffset",
            "startAt",
            "endAt",
            "durationMinutes")) assertThat(row.path(field).isNull()).as(field).isTrue();
  }

  @Test
  void s11_legacySessionPreservesNullableFactsWithoutLiveAccrual() throws Exception {
    var context = task("session-owner");
    var id = java.util.UUID.randomUUID();
    var key = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'session-owner',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','Europe/Madrid','running')",
        id,
        context.project(),
        context.task(),
        key);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("session-owner", () -> Instant.parse("2026-09-09T00:00:00Z"));
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("workSessions").intValue()).isEqualTo(1);
    var row = json.path("data").path("workSessions").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id",
            "projectId",
            "taskId",
            "requestKey",
            "startedAt",
            "plannedMinutes",
            "plannedEndAt",
            "zoneId",
            "status",
            "revision",
            "changedAt",
            "workedMicroseconds",
            "runningSince",
            "effectiveEndAt",
            "lastDecisionAt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("projectId").asText()).isEqualTo(context.project().toString());
    assertThat(row.path("taskId").asText()).isEqualTo(context.task().toString());
    assertThat(row.path("requestKey").asText()).isEqualTo(key.toString());
    assertThat(row.path("startedAt").asText()).isEqualTo("2026-09-08T12:00:00.000000Z");
    assertThat(row.path("plannedMinutes").intValue()).isEqualTo(25);
    assertThat(row.path("plannedEndAt").asText()).isEqualTo("2026-09-08T12:25:00.000000Z");
    assertThat(row.path("zoneId").asText()).isEqualTo("Europe/Madrid");
    assertThat(row.path("status").asText()).isEqualTo("running");
    assertThat(row.path("revision").asText()).isEqualTo("1");
    assertThat(row.path("workedMicroseconds").asText()).isEqualTo("0");
    for (var field :
        java.util.List.of("changedAt", "runningSince", "effectiveEndAt", "lastDecisionAt"))
      assertThat(row.path(field).isNull()).as(field).isTrue();
  }

  @Test
  void s7_s11_intervalsSortRevisionNumericallyAndDoNotAccrueTheOpenTail() throws Exception {
    var context = task("interval-owner");
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since) VALUES (?,'interval-owner',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','running',11,'2026-09-08T12:05:00Z',180000000,'2026-09-08T12:05:00Z')",
        id,
        context.project(),
        context.task(),
        id);
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,10,'2026-09-08T12:02:00Z','2026-09-08T12:04:00Z'),(?,2,'2026-09-08T12:00:00Z','2026-09-08T12:01:00Z')",
        id,
        id);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("interval-owner", () -> Instant.parse("2026-09-08T13:00:00Z"));
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("counts").path("workSessionIntervals").intValue()).isEqualTo(2);
    var intervals = json.path("data").path("workSessionIntervals");
    assertThat(intervals.get(0).properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("sessionId", "revision", "startAt", "endAt");
    assertThat(intervals.get(0).path("sessionId").asText()).isEqualTo(id.toString());
    assertThat(intervals.get(0).path("revision").asText()).isEqualTo("2");
    assertThat(intervals.get(1).path("revision").asText()).isEqualTo("10");
    assertThat(intervals.get(0).path("startAt").asText()).isEqualTo("2026-09-08T12:00:00.000000Z");
    assertThat(intervals.get(1).path("endAt").asText()).isEqualTo("2026-09-08T12:04:00.000000Z");
    var session = json.path("data").path("workSessions").get(0);
    assertThat(session.path("workedMicroseconds").asText()).isEqualTo("180000000");
    assertThat(session.path("runningSince").asText()).isEqualTo("2026-09-08T12:05:00.000000Z");
    assertThat(session.path("revision").asText()).isEqualTo("11");
  }

  @Test
  void s5_ownedSessionCannotExportAReferenceToAnotherOwnersProject() {
    var foreign = task("foreign-session-project");
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) VALUES (?,'broken-session-owner',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','running')",
        id,
        foreign.project(),
        foreign.task(),
        id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("broken-session-owner", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
        .hasMessage("El almacenamiento no está disponible.");
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM work_sessions WHERE id=?", Integer.class, id))
        .isEqualTo(1);
    assertThat(
            org.springframework.transaction.support.TransactionSynchronizationManager
                .isActualTransactionActive())
        .isFalse();
  }

  @Test
  void s15_recordLimitIncludesSessionsBeforeClockOrPayload() {
    var context = task("session-limit-owner");
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) SELECT md5('export-session-'||n)::uuid,'session-limit-owner',?,?,md5('export-key-'||n)::uuid,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','closed' FROM generate_series(1,99999) n",
        context.project(),
        context.task());
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "session-limit-owner",
                        () -> {
                          reads.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.ExportTooLargeException.class);
    assertThat(reads.get()).isZero();
  }

  @Test
  void s16_knownUtf8TextExcessIsRejectedBeforePayloadOrClock() {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) SELECT md5('export-utf8-'||n)::uuid,'text-limit-owner','P',repeat(chr(129517),4000),'idea',0,'2026-09-01Z','2026-09-01Z' FROM generate_series(1,2100) n");
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "text-limit-owner",
                        () -> {
                          reads.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.ExportTooLargeException.class);
    assertThat(reads.get()).isZero();
  }

  @Test
  void s16_oversizedUnknownJsonIsUnavailableNotExportTooLarge() {
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'json-limit-owner','PROJECT','[]'::jsonb,jsonb_build_array(jsonb_build_object('unknown',repeat('x',33554433))),0,'2026-09-01Z')",
        java.util.UUID.randomUUID());
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "json-limit-owner",
                        () -> {
                          reads.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(reads.get()).isZero();
  }

  @Test
  void s5_ownedProjectValuesMustNotReferenceAnUnexportedForeignProject() {
    var context = task("foreign-values-project");
    jdbc.update(
        "INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at) VALUES (?,'broken-project-values',?,'{}'::jsonb,0,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        context.project());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("broken-project-values", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s2_s9_blockChangesUseClosedReceiptAndDurableRowMetadata() throws Exception {
    var context = task("block-change-owner");
    var blockId = block(context);
    var id = java.util.UUID.randomUUID();
    var key = java.util.UUID.randomUUID();
    var local = java.time.LocalDateTime.parse("2026-09-08T16:00:00");
    var offset = java.time.ZoneOffset.ofHours(2);
    var before =
        new com.apptolast.organization.domain.PlannedBlock(
            blockId,
            context.project(),
            context.task(),
            new com.apptolast.organization.domain.BlockRequest(
                "Objetivo 🧭", local, local.plusMinutes(30), "Europe/Madrid", offset, offset, true),
            new com.apptolast.organization.domain.ResolvedBlockTime(
                local.toInstant(offset),
                local.plusMinutes(30).toInstant(offset),
                offset,
                offset,
                30),
            Instant.parse("2026-09-01T00:00:00Z"));
    var receipt =
        new com.apptolast.organization.domain.BlockChangeReceipt(
            id, blockId, "CANCELLED", 1, Instant.parse("2026-09-08T12:00:00Z"), before, null);
    var mapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.valueToTree(receipt);
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/before/request"))
        .put("objective", " Objetivo 🧭 ")
        .putNull("startOffset")
        .putNull("endOffset");
    tree.put("internalOnly", "DO NOT EXPORT");
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,1,'cancelled','2026-09-08T12:00:00Z')",
        blockId);
    jdbc.update(
        "INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt) VALUES (?,?,?,?,?,'CANCELLED',1,'2026-09-08T12:00:00Z',?::jsonb)",
        id,
        context.project(),
        context.task(),
        blockId,
        key,
        tree.toString());
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("block-change-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = mapper.readTree(output.toByteArray());
    assertThat(json.path("counts").path("blockChanges").intValue()).isEqualTo(1);
    var row = json.path("data").path("blockChanges").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id",
            "projectId",
            "taskId",
            "blockId",
            "requestKey",
            "kind",
            "version",
            "occurredAt",
            "receipt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("requestKey").asText()).isEqualTo(key.toString());
    assertThat(row.path("version").asText()).isEqualTo("1");
    assertThat(row.at("/receipt/version").asText()).isEqualTo("1");
    assertThat(row.at("/receipt/before/request/objective").asText()).isEqualTo(" Objetivo 🧭 ");
    assertThat(row.at("/receipt/before/request/startOffset").isNull()).isTrue();
    assertThat(row.at("/receipt/before/time/startOffset").asText()).isEqualTo("+02:00");
    assertThat(row.at("/receipt/after").isNull()).isTrue();
    assertThat(row.path("receipt").has("internalOnly")).isFalse();
  }

  @Test
  void s2_s10_sessionChangesValidateAgainstTheRealImmutableSessionContext() throws Exception {
    var context = task("session-change-owner");
    var sessionId = java.util.UUID.randomUUID();
    var id = java.util.UUID.randomUUID();
    var key = java.util.UUID.randomUUID();
    var at = Instant.parse("2026-09-08T12:00:00Z");
    var occurred = at.plusSeconds(60);
    var original =
        new com.apptolast.organization.domain.SessionStart(
            sessionId, context.project(), context.task(), at, 25, at.plusSeconds(1500), "UTC");
    var before =
        new com.apptolast.organization.domain.WorkSessionState(original, "running", 1, at, 0, at);
    var after =
        new com.apptolast.organization.domain.WorkSessionState(
            original, "paused", 2, occurred, 60000000, null);
    var receipt =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            id, sessionId, "PAUSE", occurred, before, after);
    var mapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds) VALUES (?,'session-change-owner',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','paused',2,'2026-09-08T12:01:00Z',60000000)",
        sessionId,
        context.project(),
        context.task(),
        sessionId);
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,2,'2026-09-08T12:00:00Z','2026-09-08T12:01:00Z')",
        sessionId);
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,'session-change-owner',?,?,'PAUSE',1,'2026-09-08T12:01:00Z',?::jsonb)",
        id,
        sessionId,
        key,
        mapper.writeValueAsString(receipt));
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("session-change-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = mapper.readTree(output.toByteArray());
    assertThat(json.path("counts").path("workSessionChanges").intValue()).isEqualTo(1);
    var row = json.path("data").path("workSessionChanges").get(0);
    assertThat(row.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly(
            "id", "sessionId", "requestKey", "action", "expectedRevision", "occurredAt", "receipt");
    assertThat(row.path("id").asText()).isEqualTo(id.toString());
    assertThat(row.path("sessionId").asText()).isEqualTo(sessionId.toString());
    assertThat(row.path("requestKey").asText()).isEqualTo(key.toString());
    assertThat(row.path("expectedRevision").asText()).isEqualTo("1");
    assertThat(row.at("/receipt/before/revision").asText()).isEqualTo("1");
    assertThat(row.at("/receipt/after/revision").asText()).isEqualTo("2");
    assertThat(row.at("/receipt/after/workedMicroseconds").asText()).isEqualTo("60000000");
    assertThat(row.at("/receipt/before/session/projectId").asText())
        .isEqualTo(context.project().toString());
    assertThat(row.path("receipt").properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "sessionId", "action", "occurredAt", "before", "after");
  }

  @Test
  void s4_concurrentCommitCannotMixConfigurationValuesOrCounts() throws Exception {
    var context = task("snapshot-owner");
    var field = java.util.UUID.randomUUID();
    var definition =
        "[{\"id\":\"" + field + "\",\"label\":\"Antes\",\"type\":\"TEXT\",\"active\":true}]";
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'snapshot-owner','PROJECT','[]'::jsonb,?::jsonb,0,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        definition);
    jdbc.update(
        "INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at) VALUES (?,'snapshot-owner',?,?::jsonb,0,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        context.project(),
        "{\"" + field + "\":\"valor anterior\"}");
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare(
                "snapshot-owner",
                () -> {
                  calls.incrementAndGet();
                  var outerPid = jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
                  var writer =
                      new org.springframework.transaction.support.TransactionTemplate(manager);
                  writer.setPropagationBehavior(
                      org.springframework.transaction.TransactionDefinition
                          .PROPAGATION_REQUIRES_NEW);
                  writer.executeWithoutResult(
                      status -> {
                        assertThat(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class))
                            .isNotEqualTo(outerPid);
                        jdbc.update(
                            "UPDATE customization_preferences SET custom_fields=?::jsonb,version=1 WHERE owner_id='snapshot-owner'",
                            definition.replace("Antes", "Después"));
                        jdbc.update(
                            "UPDATE project_custom_field_values SET field_values=?::jsonb,version=1 WHERE owner_id='snapshot-owner'",
                            "{\"" + field + "\":\"valor posterior\"}");
                        jdbc.update(
                            "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'snapshot-owner','Posterior','','idea',0,'2026-09-01Z','2026-09-01Z')",
                            java.util.UUID.randomUUID());
                      });
                  return Instant.EPOCH;
                });
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(calls.get()).isEqualTo(1);
    assertThat(json.path("counts").path("projects").intValue()).isEqualTo(1);
    assertThat(json.at("/data/customization/0/customFields/0/label").asText()).isEqualTo("Antes");
    assertThat(json.at("/data/projectCustomFieldValues/0/values/0/value").asText())
        .isEqualTo("valor anterior");
    assertThat(json.at("/data/projectCustomFieldValues/0/version").asText()).isEqualTo("0");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id='snapshot-owner'", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM project_custom_field_values WHERE owner_id='snapshot-owner'",
                Long.class))
        .isEqualTo(1L);
  }

  @Test
  void s17_lateSqlFailureDoesNotReturnPreparedBytesOrChangeFacts() {
    var context = task("late-sql-owner");
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'late-sql-owner','UTC',0,0,0,0,0,0,0,0,'2026-09-01Z','2026-09-01Z')",
        java.util.UUID.randomUUID());
    jdbc.execute(
        "CREATE FUNCTION export_fail_minutes() RETURNS integer LANGUAGE plpgsql STABLE AS 'BEGIN RAISE EXCEPTION ''private late SQL failure''; END'");
    jdbc.execute("ALTER TABLE availability_preferences RENAME TO export_availability_backing");
    try {
      jdbc.execute(
          "CREATE VIEW availability_preferences AS SELECT id,owner_id,zone_id,CASE WHEN owner_id='late-sql-owner' THEN export_fail_minutes() ELSE monday_minutes END AS monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at FROM export_availability_backing");
      var clockCalls = new java.util.concurrent.atomic.AtomicInteger();
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresExportDataQueries(jdbc, manager)
                      .prepare(
                          "late-sql-owner",
                          () -> {
                            clockCalls.incrementAndGet();
                            return Instant.EPOCH;
                          }))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
          .hasMessage("El almacenamiento no está disponible.");
      assertThat(clockCalls.get()).isEqualTo(1);
      assertThat(
              org.springframework.transaction.support.TransactionSynchronizationManager
                  .isActualTransactionActive())
          .isFalse();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM projects WHERE id=?", Integer.class, context.project()))
          .isEqualTo(1);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM outbox_events WHERE owner_id='late-sql-owner'",
                  Integer.class))
          .isZero();
    } finally {
      jdbc.execute("DROP VIEW IF EXISTS availability_preferences");
      jdbc.execute("ALTER TABLE export_availability_backing RENAME TO availability_preferences");
      jdbc.execute("DROP FUNCTION export_fail_minutes()");
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT zone_id FROM availability_preferences WHERE owner_id='late-sql-owner'",
                String.class))
        .isEqualTo("UTC");
  }

  @Test
  void s15_exactRecordAndByteLimitsKeepEveryRowAndRejectOneMoreByte(
      @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) SELECT md5('export-inclusive-'||n)::uuid,'inclusive-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z' FROM generate_series(1,100000) n");
    var queries = new PostgresExportDataQueries(jdbc, manager);
    var firstStart = System.nanoTime();
    long baseLength = queries.prepare("inclusive-owner", () -> Instant.EPOCH).contentLength();
    long padding = 33554432 - baseLength;
    assertThat(padding).isBetween(1L, 399999999L);
    jdbc.update(
        "WITH ordered AS (SELECT id,row_number() OVER(ORDER BY id)-1 AS n FROM projects WHERE owner_id='inclusive-owner') UPDATE projects p SET description=repeat('x',greatest(0,least(4000,? - ordered.n*4000))::int) FROM ordered WHERE p.id=ordered.id",
        padding);
    var started = System.nanoTime();
    var file = queries.prepare("inclusive-owner", () -> Instant.EPOCH);
    long elapsedMillis = (System.nanoTime() - started) / 1000000;
    assertThat(file.contentLength()).isEqualTo(33554432L);
    var path = directory.resolve("inclusive.json");
    try (var output = java.nio.file.Files.newOutputStream(path)) {
      file.writeTo(output);
    }
    assertThat(java.nio.file.Files.size(path)).isEqualTo(33554432L);
    int rows = 0;
    String previous = "";
    var reader = new ObjectMapper();
    try (var parser = reader.getFactory().createParser(path.toFile())) {
      while (parser.nextToken() != null) {
        if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.FIELD_NAME
            && parser.currentName().equals("projects")) {
          var token = parser.nextToken();
          if (token == com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
            while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
              com.fasterxml.jackson.databind.JsonNode row = reader.readTree(parser);
              var id = row.path("id").asText();
              assertThat(id.compareTo(previous)).isPositive();
              previous = id;
              rows++;
            }
          } else assertThat(parser.getIntValue()).isEqualTo(100000);
        }
      }
    }
    assertThat(rows).isEqualTo(100000);
    jdbc.update(
        "UPDATE projects SET description=description||'x' WHERE id=(SELECT id FROM projects WHERE owner_id='inclusive-owner' AND description='' ORDER BY id LIMIT 1)");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> queries.prepare("inclusive-owner", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.ExportTooLargeException.class);
    System.out.println(
        "export-inclusive: records=100000 bytes=33554432 prepareMillis="
            + elapsedMillis
            + " totalMillis="
            + (System.nanoTime() - firstStart) / 1000000);
  }

  @Test
  void s5_ownedTaskValuesCannotExposeTheForeignTasksProject() {
    var context = task("foreign-task-values-project");
    jdbc.update(
        "INSERT INTO task_custom_field_values(id,owner_id,task_id,field_values,version,updated_at) VALUES (?,'broken-task-values',?,'{}'::jsonb,0,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        context.task());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("broken-task-values", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s5_changeOwnerCannotExposeAnotherOwnersImmutableSession() throws Exception {
    var context = task("foreign-change-session");
    var session = java.util.UUID.randomUUID();
    var change = java.util.UUID.randomUUID();
    var start = Instant.parse("2026-09-08T12:00:00Z");
    var original =
        new com.apptolast.organization.domain.SessionStart(
            session, context.project(), context.task(), start, 25, start.plusSeconds(1500), "UTC");
    var before =
        new com.apptolast.organization.domain.WorkSessionState(
            original, "running", 1, start, 0, start);
    var after =
        new com.apptolast.organization.domain.WorkSessionState(
            original, "paused", 2, start.plusSeconds(60), 60000000, null);
    var receipt =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            change, session, "PAUSE", after.changedAt(), before, after);
    var mapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds) VALUES (?,'foreign-change-session',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','paused',2,'2026-09-08T12:01:00Z',60000000)",
        session,
        context.project(),
        context.task(),
        session);
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,'broken-change-owner',?,?,'PAUSE',1,'2026-09-08T12:01:00Z',?::jsonb)",
        change,
        session,
        change,
        mapper.writeValueAsString(receipt));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("broken-change-owner", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s5_s13_unrepresentableStoredInstantCannotProduceAnInvalidTimestamp() {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'ancient-owner','P','','idea',0,'0001-01-01 BC','2026-09-01Z')",
        java.util.UUID.randomUUID());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("ancient-owner", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s5_negativeSessionRevisionCannotBeExportedAsAValidLong() {
    var context = task("negative-session-revision");
    var session = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,worked_microseconds) VALUES (?,'negative-session-revision',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','running',-1,0)",
        session,
        context.project(),
        context.task(),
        session);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("negative-session-revision", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_sessions WHERE id=?", Long.class, session))
        .isEqualTo(-1L);
  }

  @Test
  void s5_negativeIntervalRevisionCannotBeExportedAsAValidLong() {
    var context = task("negative-interval-revision");
    var session = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,worked_microseconds) VALUES (?,'negative-interval-revision',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','running',1,0)",
        session,
        context.project(),
        context.task(),
        session);
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,-1,'2026-09-08T12:00:00Z','2026-09-08T12:01:00Z')",
        session);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("negative-interval-revision", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_session_intervals WHERE session_id=?",
                Long.class,
                session))
        .isEqualTo(-1L);
  }

  @Test
  void s5_invalidStoredAppearanceContrastCannotBecomeASuccessfulExport() {
    jdbc.update(
        "INSERT INTO appearance_preferences(id,owner_id,theme,accent_light,accent_dark,version,updated_at) VALUES (?,'broken-appearance','LIGHT','#FFFFFF','#00FFFF',0,'2026-09-08Z')",
        java.util.UUID.randomUUID());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("broken-appearance", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT accent_light FROM appearance_preferences WHERE owner_id='broken-appearance'",
                String.class))
        .isEqualTo("#FFFFFF");
  }

  @Test
  void s16_oversizedUnvalidatedZoneIsRejectedBeforeMaterializingTextOrClock() {
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'oversized-zone',repeat('z',33554433),0,0,0,0,0,0,0,0,'2026-09-08Z','2026-09-08Z')",
        java.util.UUID.randomUUID());
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "oversized-zone",
                        () -> {
                          calls.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(calls).hasValue(0);
  }

  @Test
  void s16_oversizedSessionStatusIsRejectedBeforeClock() {
    var context = task("oversized-status");
    var session = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,worked_microseconds) VALUES (?,'oversized-status',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC',repeat('s',33554433),1,0)",
        session,
        context.project(),
        context.task(),
        session);
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "oversized-status",
                        () -> {
                          calls.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(calls).hasValue(0);
  }

  @Test
  void s16_oversizedChangeActionIsRejectedBeforeClock() {
    var context = task("oversized-action");
    var session = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,worked_microseconds) VALUES (?,'oversized-action',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','running',1,0)",
        session,
        context.project(),
        context.task(),
        session);
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,'oversized-action',?,?,repeat('a',33554433),1,'2026-09-08T12:01:00Z','{}'::jsonb)",
        java.util.UUID.randomUUID(),
        session,
        java.util.UUID.randomUUID());
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare(
                        "oversized-action",
                        () -> {
                          calls.incrementAndGet();
                          return Instant.EPOCH;
                        }))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(calls).hasValue(0);
  }

  @Test
  void s5_unknownStoredSessionStatusCannotEscapeTheClosedSchema() {
    var context = task("unknown-status");
    var session = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,worked_microseconds) VALUES (?,'unknown-status',?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','alien',1,0)",
        session,
        context.project(),
        context.task(),
        session);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("unknown-status", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM work_sessions WHERE id=?", String.class, session))
        .isEqualTo("alien");
  }

  @Test
  void s5_s8_invalidOriginalOffsetCannotBecomeAnExportedOffset() {
    var context = task("invalid-block-offset");
    var id = block(context);
    jdbc.update("UPDATE planned_blocks SET start_offset='bogus' WHERE id=?", id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("invalid-block-offset", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT start_offset FROM planned_blocks WHERE id=?", String.class, id))
        .isEqualTo("bogus");
  }

  @Test
  void s5_s8_invalidProjectionEndOffsetIsRejectedWithoutChangingItsOriginal() {
    var context = task("invalid-projection-offset");
    var id = block(context);
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) SELECT id,1,'planned','2026-09-02Z',start_local,end_local,zone_id,start_offset,'bogus',start_at,end_at,duration_minutes FROM planned_blocks WHERE id=?",
        id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresExportDataQueries(jdbc, manager)
                    .prepare("invalid-projection-offset", () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT end_offset FROM planned_blocks WHERE id=?", String.class, id))
        .isEqualTo("+02:00");
  }

  @Test
  void s6_exactlyTwelveConfiguredValuesRemainCompleteAndOrdered() throws Exception {
    var project = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'twelve-values-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z')",
        project);
    var mapper = new ObjectMapper();
    var definitions = mapper.createArrayNode();
    var values = mapper.createObjectNode();
    for (int n = 12; n >= 1; n--) {
      var id = String.format(java.util.Locale.ROOT, "00000000-0000-0000-0000-%012d", n);
      definitions
          .addObject()
          .put("id", id)
          .put("label", "Campo " + n)
          .put("type", "NUMBER")
          .put("active", n % 2 == 0);
      values.put(id, 100 + n);
    }
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'twelve-values-owner','PROJECT','[]'::jsonb,?::jsonb,1,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        definitions.toString());
    jdbc.update(
        "INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at) VALUES (?,'twelve-values-owner',?,?::jsonb,3,'2026-09-01Z')",
        java.util.UUID.randomUUID(),
        project,
        values.toString());
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("twelve-values-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = mapper.readTree(output.toByteArray());
    assertThat(json.path("counts").path("customization").intValue()).isEqualTo(1);
    assertThat(json.path("counts").path("projectCustomFieldValues").intValue()).isEqualTo(1);
    assertThat(json.path("data").path("customization").get(0).path("customFields"))
        .isEqualTo(definitions);
    var exported = json.path("data").path("projectCustomFieldValues").get(0);
    assertThat(exported.path("projectId").asText()).isEqualTo(project.toString());
    assertThat(exported.path("version").asText()).isEqualTo("3");
    assertThat(exported.path("values")).hasSize(12);
    for (int n = 1; n <= 12; n++) {
      var row = exported.path("values").get(n - 1);
      assertThat(row.path("fieldId").asText())
          .isEqualTo(String.format(java.util.Locale.ROOT, "00000000-0000-0000-0000-%012d", n));
      assertThat(row.path("value").isIntegralNumber()).isTrue();
      assertThat(row.path("value").intValue()).isEqualTo(100 + n);
    }
  }

  @Test
  void s8_completeProjectionKeepsNumericDurationAndItsOwnResolvedTimes() throws Exception {
    var context = task("complete-projection-owner");
    var blockId = block(context);
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) VALUES (?,7,'planned','2026-09-02T12:00:00.123456Z','2026-09-09T18:00:00','2026-09-09T18:45:00','Europe/Madrid','+02:00','+02:00','2026-09-09T16:00:00Z','2026-09-09T16:45:00Z',45)",
        blockId);
    var file =
        new PostgresExportDataQueries(jdbc, manager)
            .prepare("complete-projection-owner", () -> Instant.EPOCH);
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var mapper = new ObjectMapper();
    var json = mapper.readTree(output.toByteArray());
    var expected =
        mapper.readTree(
            """
        {"blockId":"%s","version":"7","status":"planned",
         "updatedAt":"2026-09-02T12:00:00.123456Z",
         "startLocal":"2026-09-09T18:00:00","endLocal":"2026-09-09T18:45:00",
         "zoneId":"Europe/Madrid","startOffset":"+02:00","endOffset":"+02:00",
         "startAt":"2026-09-09T16:00:00.000000Z","endAt":"2026-09-09T16:45:00.000000Z",
         "durationMinutes":45}
        """
                .formatted(blockId));
    assertThat(json.path("counts").path("blockProjections").intValue()).isEqualTo(1);
    assertThat(json.path("counts").path("plannedBlocks").intValue()).isEqualTo(1);
    var projection = json.path("data").path("blockProjections").get(0);
    assertThat(projection).isEqualTo(expected);
    assertThat(projection.path("durationMinutes").isIntegralNumber()).isTrue();
    var original = json.path("data").path("plannedBlocks").get(0);
    assertThat(original.path("id").asText()).isEqualTo(blockId.toString());
    assertThat(original.path("startLocal").asText()).isEqualTo("2026-09-08T16:00:00");
    assertThat(original.path("startAt").asText()).isEqualTo("2026-09-08T14:00:00.000000Z");
    assertThat(original.path("endAt").asText()).isEqualTo("2026-09-08T14:30:00.000000Z");
    assertThat(original.path("durationMinutes").intValue()).isEqualTo(30);
  }
}
