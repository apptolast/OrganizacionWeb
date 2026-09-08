package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.PreviewImportData;
import java.io.ByteArrayInputStream;
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
class ImportPersistenceTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"missing", "string", "fraction", "negative", "total", "outcome", "date"})
  void s22_corruptStoredReceiptNeverBecomesConfirmation(String corruption) throws Exception {
    var owner = "corrupt-receipt-" + java.util.UUID.randomUUID();
    var key = java.util.UUID.randomUUID();
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var zero = mapper.valueToTree(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    var counts = (com.fasterxml.jackson.databind.node.ObjectNode) zero.deepCopy();
    switch (corruption) {
      case "missing" -> counts.remove("tasks");
      case "string" -> counts.put("tasks", "0");
      case "fraction" -> counts.put("tasks", new java.math.BigDecimal("0.1"));
      case "negative" -> counts.put("tasks", -1);
      case "total" -> {
        counts.put("tasks", 60000);
        counts.put("projects", 60000);
      }
      case "outcome" -> counts.put("tasks", 1);
      default -> {}
    }
    jdbc.update(
        "INSERT INTO import_receipts VALUES (?,?,?,1,?::timestamptz,'NO_CHANGE',?::jsonb,?::jsonb)",
        owner,
        key,
        "a".repeat(64),
        corruption.equals("date") ? "10000-01-01Z" : "2026-09-08Z",
        counts.toString(),
        zero.toString());
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(r)::text FROM import_receipts r WHERE owner_id=?",
            String.class,
            owner);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new PostgresImportDataStore(jdbc, manager).find(owner, key))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT row_to_json(r)::text FROM import_receipts r WHERE owner_id=?",
                String.class,
                owner))
        .isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("snapshotMismatches")
  void s10_currentFactsMustMatchTheirHistoricalSnapshots(
      String collection, String field, String value) throws Exception {
    invalidMutation(collection, field, value);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> snapshotMismatches() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "blockChanges", "/receipt/before/request/objective", "\"Otra intención\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "blockChanges", "/receipt/before/createdAt", "\"2026-09-07T12:00:00.000000Z\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "blockProjections", "/updatedAt", "\"2026-09-08T12:00:01.000000Z\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "blockProjections", "/status", "\"planned\""),
        org.junit.jupiter.params.provider.Arguments.of("workSessions", "/status", "\"running\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "workSessions", "/changedAt", "\"2026-09-08T12:02:00.000000Z\""));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("durableChecks")
  void s10_durableChecksRejectInvalidHistoryBeforeSql(String collection, String field, String value)
      throws Exception {
    invalidMutation(collection, field, value);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> durableChecks() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of("taskStatusHistory", "taskVersion", "\"0\""),
        org.junit.jupiter.params.provider.Arguments.of("tasks", "status", "\"completed\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "tasks", "completedAt", "\"2026-09-08T12:02:00.000000Z\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "workSessions", "effectiveEndAt", "\"2026-09-08T12:24:00.000000Z\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "blockProjections", "durationMinutes", "30"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"running", "paused"})
  void s15_unionCannotIntroduceASecondOpenSession(String state) throws Exception {
    var owner = "open-union-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    jdbc.update("DELETE FROM work_session_changes WHERE session_id=?", ids[3]);
    jdbc.update("DELETE FROM work_session_intervals WHERE session_id=?", ids[3]);
    jdbc.update("DELETE FROM work_sessions WHERE id=?", ids[3]);
    jdbc.update(
        """
        INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since)
        VALUES (?,?,?,?,?,'2026-09-08T14:00:00Z',25,'2026-09-08T14:25:00Z','UTC',?,2,'2026-09-08T14:00:00Z',0,
          CASE WHEN ?='running' THEN '2026-09-08T14:00:00Z'::timestamptz ELSE NULL END)
        """,
        java.util.UUID.randomUUID(),
        owner,
        ids[0],
        ids[1],
        java.util.UUID.randomUUID(),
        state,
        state);
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var before = businessRows();
    var store = new PostgresImportDataStore(jdbc, manager);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.preview(owner, new ByteArrayInputStream(input)))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                store.apply(
                    owner,
                    java.util.UUID.randomUUID(),
                    sha,
                    new ByteArrayInputStream(input),
                    () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    assertThat(businessRows()).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s15_unionCannotExceedTheConfiguredActiveProjectQuota() throws Exception {
    var owner = "quota-" + java.util.UUID.randomUUID();
    for (int i = 0; i < 3; i++)
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Activo','','active',0,'2026-09-01Z','2026-09-01Z')",
          java.util.UUID.randomUUID(),
          owner);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(invalidProjectText(owner, "Otro activo"));
    var project =
        (com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path("projects").get(0);
    project.put("id", java.util.UUID.randomUUID().toString());
    project.put("status", "active");
    var input = json.writeValueAsBytes(file);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var before = businessRows();
    var store = new PostgresImportDataStore(jdbc, manager);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.preview(owner, new ByteArrayInputStream(input)))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                store.apply(
                    owner,
                    java.util.UUID.randomUUID(),
                    sha,
                    new ByteArrayInputStream(input),
                    () -> Instant.EPOCH))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    assertThat(businessRows()).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"false,false", "false,true", "true,false"})
  void s14_previewWarnsOnlyAboutNewRunningSessionsAndKeepsLegacyNull(
      boolean existing, boolean legacyNull) throws Exception {
    var owner = "running-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    jdbc.update("DELETE FROM work_session_changes WHERE session_id=?", ids[3]);
    jdbc.update("DELETE FROM work_session_intervals WHERE session_id=?", ids[3]);
    jdbc.update(
        "UPDATE work_sessions SET status='running',worked_microseconds=0,changed_at=started_at,running_since=started_at WHERE id=?",
        ids[3]);
    if (legacyNull)
      jdbc.update("UPDATE work_sessions SET changed_at=NULL,running_since=NULL WHERE id=?", ids[3]);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    if (!existing) jdbc.update("DELETE FROM work_sessions WHERE id=?", ids[3]);
    var before = businessRows();
    var preview =
        new PostgresImportDataStore(jdbc, manager)
            .preview(owner, new ByteArrayInputStream(bytes.toByteArray()));
    if (existing) assertThat(preview.runningSessions()).isEmpty();
    else
      assertThat(preview.runningSessions())
          .containsExactly(
              new com.apptolast.organization.application.ImportPreview.RunningSession(
                  ids[3], legacyNull ? null : Instant.parse("2026-09-08T12:00:00Z")));
    assertThat(businessRows()).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "appearance",
        "customization",
        "projectCustomFieldValues",
        "taskCustomFieldValues"
      })
  void s9_preferencesAndValuesAreValidatedAgainstTheFileInsteadOfDestination(String collection)
      throws Exception {
    var owner = "preferences-" + java.util.UUID.randomUUID();
    seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    if (collection.equals("appearance"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path(collection).get(0))
          .put("theme", "unknown");
    else if (collection.equals("customization"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path(collection).get(0))
          .set("visibleFields", json.createArrayNode().add("unknown"));
    else {
      String scope = collection.equals("projectCustomFieldValues") ? "PROJECT" : "TASK";
      for (var config : file.path("data").path("customization"))
        if (config.path("scope").asText().equals(scope))
          ((com.fasterxml.jackson.databind.node.ArrayNode) config.path("customFields")).removeAll();
    }
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "taskStatusHistory",
        "availability",
        "plannedBlocks",
        "blockChanges",
        "workSessions",
        "workSessionChanges",
        "appearance",
        "customization",
        "projectCustomFieldValues",
        "taskCustomFieldValues"
      })
  void s6_alternativeKeyCannotBeTakenByANewIdentity(String collection) throws Exception {
    var owner = "alternative-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    if (collection.equals("workSessions")) {
      jdbc.update("DELETE FROM work_session_changes WHERE session_id=?", ids[3]);
      jdbc.update("DELETE FROM work_session_intervals WHERE session_id=?", ids[3]);
      jdbc.update(
          "UPDATE work_sessions SET status='closed',worked_microseconds=0,changed_at=NULL,running_since=NULL WHERE id=?",
          ids[3]);
    }
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    var original = file.path("data").path(collection).get(0).path("id").textValue();
    if (collection.equals("plannedBlocks")) {
      for (var child : java.util.List.of("blockChanges", "blockProjections")) {
        ((com.fasterxml.jackson.databind.node.ArrayNode) file.path("data").path(child)).removeAll();
        ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("counts")).put(child, 0);
      }
    }
    var input =
        json.writeValueAsString(file)
            .replace(original, java.util.UUID.randomUUID().toString())
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(input)))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "projects",
        "tasks",
        "taskStatusHistory",
        "availability",
        "plannedBlocks",
        "blockProjections",
        "blockChanges",
        "workSessions",
        "workSessionIntervals",
        "workSessionChanges",
        "appearance",
        "customization",
        "projectCustomFieldValues",
        "taskCustomFieldValues"
      })
  void s10_duplicateDurableIdentityIsInvalidEvenWhenRowsAreIdentical(String collection)
      throws Exception {
    duplicateFile(collection, false);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "taskStatusHistory",
        "availability",
        "plannedBlocks",
        "blockChanges",
        "workSessions",
        "workSessionChanges",
        "appearance",
        "customization",
        "projectCustomFieldValues",
        "taskCustomFieldValues"
      })
  void s10_alternativeKeysMustAlsoBeUniqueInsideTheFile(String collection) throws Exception {
    duplicateFile(collection, true);
  }

  private void duplicateFile(String collection, boolean newIdentity) throws Exception {
    var owner = "duplicate-" + java.util.UUID.randomUUID();
    seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    var rows = (com.fasterxml.jackson.databind.node.ArrayNode) file.path("data").path(collection);
    var copy = (com.fasterxml.jackson.databind.node.ObjectNode) rows.get(0).deepCopy();
    if (newIdentity) {
      copy =
          (com.fasterxml.jackson.databind.node.ObjectNode)
              json.readTree(
                  json.writeValueAsString(copy)
                      .replace(copy.path("id").asText(), java.util.UUID.randomUUID().toString()));
      if (collection.equals("workSessions")) {
        copy.put("status", "closed");
        copy.put("workedMicroseconds", "0");
        copy.putNull("changedAt");
        copy.putNull("runningSince");
      }
    }
    rows.add(copy);
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("counts"))
        .put(collection, rows.size());
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "availability,mondayMinutes",
    "plannedBlocks,durationMinutes",
    "workSessions,plannedMinutes",
    "tasks,estimatedMinutes"
  })
  void s9_mathematicallyIntegralNumbersAreIdenticalWithoutLexicalCoercion(
      String collection, String field) throws Exception {
    var owner = "integral-" + java.util.UUID.randomUUID();
    seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    var row =
        (com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path(collection).get(0);
    row.set(
        field,
        com.fasterxml.jackson.databind.node.DecimalNode.valueOf(
            new java.math.BigDecimal(row.get(field).asText() + ".0")));
    var before = businessRows();
    var preview =
        new PostgresImportDataStore(jdbc, manager)
            .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file)));
    assertThat(preview.identicalCounts())
        .isEqualTo(json.treeToValue(file.path("counts"), ImportCounts.class));
    assertThat(businessRows()).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidTypedRows")
  void s9_closedRowsRejectCoercionAndUnrepresentableFacts(
      String collection, String field, String value) throws Exception {
    invalidMutation(collection, field, value);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "taskStatusHistory,taskId", "plannedBlocks,projectId", "blockProjections,blockId",
    "blockChanges,blockId", "workSessions,taskId", "workSessionIntervals,sessionId",
    "workSessionChanges,sessionId", "projectCustomFieldValues,projectId",
        "taskCustomFieldValues,taskId"
  })
  void s10_everyFamilyMustReferenceFactsInsideItsOwnFile(String collection, String field)
      throws Exception {
    invalidMutation(collection, field, "\"" + java.util.UUID.randomUUID() + "\"");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "tasks,parentId", "taskStatusHistory,projectId", "plannedBlocks,projectId",
    "blockChanges,projectId", "workSessions,projectId", "taskCustomFieldValues,projectId"
  })
  void s10_relatedFactsMustBelongToTheSameProject(String collection, String field)
      throws Exception {
    invalidMutation(collection, field, "cross-project");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("inconsistentHistoricalFacts")
  void s10_historicalFactsKeepTheirDurableInvariants(String collection, String field, String value)
      throws Exception {
    invalidMutation(collection, field, value);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments>
      inconsistentHistoricalFacts() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of("plannedBlocks", "durationMinutes", "31"),
        org.junit.jupiter.params.provider.Arguments.of("taskStatusHistory", "taskVersion", "\"3\""),
        org.junit.jupiter.params.provider.Arguments.of("blockProjections", "version", "\"2\""),
        org.junit.jupiter.params.provider.Arguments.of("workSessionIntervals", "revision", "\"3\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "workSessions", "workedMicroseconds", "\"60000001\""),
        org.junit.jupiter.params.provider.Arguments.of("availability", "mondayMinutes", "1441"),
        org.junit.jupiter.params.provider.Arguments.of("workSessions", "status", "\"alien\""),
        org.junit.jupiter.params.provider.Arguments.of("plannedBlocks", "objective", "123"));
  }

  private void invalidMutation(String collection, String field, String value) throws Exception {

    var owner = "typed-" + java.util.UUID.randomUUID();
    var originalIds = seedCompleteAccount(owner);
    if (value.equals("cross-project")) {
      var project = java.util.UUID.randomUUID();
      var task = java.util.UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Segundo','','idea',0,'2026-09-01Z','2026-09-01Z')",
          project,
          owner);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'Segunda','Hecha','pending',0,'2026-09-01Z','2026-09-01Z')",
          task,
          project);
      value = "\"" + (field.equals("parentId") ? task : project) + "\"";
    }
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00.123456Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    var selected = file.path("data").path(collection).get(0);
    if (collection.equals("tasks") && field.equals("parentId")) {
      for (var candidate : file.path("data").path("tasks"))
        if (candidate.path("id").asText().equals(originalIds[1].toString())) selected = candidate;
    }
    if (field.startsWith("/")) {
      int last = field.lastIndexOf('/');
      ((com.fasterxml.jackson.databind.node.ObjectNode) selected.at(field.substring(0, last)))
          .set(field.substring(last + 1), json.readTree(value));
    } else
      ((com.fasterxml.jackson.databind.node.ObjectNode) selected).set(field, json.readTree(value));
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidTypedRows() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of("tasks", "unknown", "true"),
        org.junit.jupiter.params.provider.Arguments.of("availability", "mondayMinutes", "\"60\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "blockProjections", "durationMinutes", "\"30\""),
        org.junit.jupiter.params.provider.Arguments.of("workSessions", "revision", "\"-1\""),
        org.junit.jupiter.params.provider.Arguments.of("taskCustomFieldValues", "version", "1"),
        org.junit.jupiter.params.provider.Arguments.of(
            "workSessionIntervals", "endAt", "\"2026-09-08T12:01:00.1234567Z\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "plannedBlocks", "allowOverBudget", "\"true\""),
        org.junit.jupiter.params.provider.Arguments.of(
            "customization", "updatedAt", "\"2026-09-08T12:00:00\""));
  }

  @Test
  void s9_oversizedInvalidIdentityIsRejectedBeforeIndexConstruction() throws Exception {
    var owner = "invalid-index-key-" + java.util.UUID.randomUUID();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(invalidProjectText(owner, "Proyecto"));
    var invalid = new StringBuilder();
    for (int i = 0; i < 1000; i++) invalid.append(java.util.UUID.randomUUID());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path("projects").get(0))
        .put("id", invalid.toString());
    var body = json.writeValueAsBytes(file);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(body));
    var store = new PostgresImportDataStore(jdbc, manager);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.preview(owner, new ByteArrayInputStream(body)))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                store.apply(
                    owner,
                    java.util.UUID.randomUUID(),
                    sha,
                    new ByteArrayInputStream(body),
                    () -> {
                      throw new AssertionError("Invalid identity must not read clock");
                    }))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id=?", Integer.class, owner))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "block_changes,false",
    "work_session_changes,false",
    "block_changes,true",
    "work_session_changes,true"
  })
  void s22_existingHistoricalReceiptCorruptionIsStorageUnavailable(String table, boolean oversized)
      throws Exception {
    var owner = "existing-corrupt-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var where = table.equals("block_changes") ? "project_id=?" : "session_id=?";
    var id = table.equals("block_changes") ? ids[0] : ids[3];
    var original =
        jdbc.queryForObject(
            "SELECT receipt::text FROM " + table + " WHERE " + where, String.class, id);
    try {
      jdbc.update(
          "UPDATE "
              + table
              + " SET receipt=jsonb_build_object('unknown',repeat('x',?)) WHERE "
              + where,
          oversized ? 33554433 : 1,
          id);
      var before =
          jdbc.queryForMap(
              "SELECT xmin::text,ctid::text,md5(receipt::text) AS digest FROM "
                  + table
                  + " WHERE "
                  + where,
              id);
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .preview(owner, new ByteArrayInputStream(bytes.toByteArray())))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(
              jdbc.queryForMap(
                  "SELECT xmin::text,ctid::text,md5(receipt::text) AS digest FROM "
                      + table
                      + " WHERE "
                      + where,
                  id))
          .isEqualTo(before);
    } finally {
      jdbc.update("UPDATE " + table + " SET receipt=?::jsonb WHERE " + where, original, id);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"effectiveEndAt", "lastDecisionAt"})
  void s10_extendedSessionKeepsTheConfirmedEndAndLastDecision(String field) throws Exception {
    var owner = "extended-snapshot-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    var json =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var at = Instant.parse("2026-09-08T12:02:00Z");
    new com.apptolast.organization.application.ExtendWorkSession(
            new PostgresWorkSessionStore(jdbc, manager, json),
            java.time.Clock.fixed(at, java.time.ZoneOffset.UTC))
        .extend(
            owner,
            ids[3],
            java.util.UUID.randomUUID(),
            new com.apptolast.organization.application.WorkSessionRevision(ids[3], 2),
            5);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> at.plusSeconds(1))
        .writeTo(bytes);
    var store = new PostgresImportDataStore(jdbc, manager);
    assertThat(
            store
                .preview(owner, new ByteArrayInputStream(bytes.toByteArray()))
                .identicalCounts()
                .workSessions())
        .isEqualTo(1);
    var file = json.readTree(bytes.toByteArray());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.path("data").path("workSessions").get(0))
        .put(
            field,
            field.equals("effectiveEndAt")
                ? "2026-09-08T12:31:00.000000Z"
                : "2026-09-08T12:03:00.000000Z");
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  @Test
  void s10_projectionIntervalMustMatchLatestReceiptWithoutRecalculatingZone() throws Exception {
    var owner = "projection-snapshot-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    jdbc.update(
        "UPDATE block_projections p SET (start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes)=(b.start_local,b.end_local,b.zone_id,b.start_offset,b.end_offset,b.start_at,b.end_at,b.duration_minutes) FROM planned_blocks b WHERE p.block_id=b.id AND b.id=?",
        ids[2]);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    var store = new PostgresImportDataStore(jdbc, manager);
    assertThat(
            store
                .preview(owner, new ByteArrayInputStream(bytes.toByteArray()))
                .identicalCounts()
                .blockProjections())
        .isEqualTo(1);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    var projection =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            file.path("data").path("blockProjections").get(0);
    projection
        .put("startLocal", "2026-09-08T17:00:00")
        .put("endLocal", "2026-09-08T17:30:00")
        .put("startAt", "2026-09-08T15:00:00.000000Z")
        .put("endAt", "2026-09-08T15:30:00.000000Z");
    var before = businessRows();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> store.preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(businessRows()).isEqualTo(before);
  }

  @Test
  void s23_receiptFailureRollsBackAllFourteenCollectionsAndKeepsExistingFacts() throws Exception {
    var owner = "late-rollback-" + java.util.UUID.randomUUID();
    var ids = seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00Z"))
        .writeTo(bytes);
    for (var table : java.util.List.of("block_changes", "task_status_history", "planned_blocks")) {
      if (table.equals("planned_blocks"))
        jdbc.update("DELETE FROM block_projections WHERE block_id=?", ids[2]);
      jdbc.update("DELETE FROM " + table + " WHERE project_id=?", ids[0]);
    }
    jdbc.update("DELETE FROM work_session_changes WHERE owner_id=?", owner);
    jdbc.update("DELETE FROM work_session_intervals WHERE session_id=?", ids[3]);
    for (var table :
        java.util.List.of(
            "work_sessions",
            "project_custom_field_values",
            "task_custom_field_values",
            "customization_preferences",
            "availability_preferences",
            "appearance_preferences"))
      jdbc.update("DELETE FROM " + table + " WHERE owner_id=?", owner);
    jdbc.update("DELETE FROM tasks WHERE project_id=?", ids[0]);
    jdbc.update("DELETE FROM projects WHERE id=?", ids[0]);
    seedCompleteAccount("untouched-" + java.util.UUID.randomUUID());
    var before = businessRows();
    var outbox =
        jdbc.queryForList(
            "SELECT xmin::text,ctid::text,row_to_json(o)::text AS contents FROM outbox_events o ORDER BY event_id");
    var key = java.util.UUID.randomUUID();
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    jdbc.execute(
        "CREATE FUNCTION reject_integral_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled late failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_integral_receipt BEFORE INSERT ON import_receipts FOR EACH ROW EXECUTE FUNCTION reject_integral_receipt()");
    var reached = new java.util.concurrent.atomic.AtomicBoolean();
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .apply(
                          owner,
                          key,
                          sha,
                          new ByteArrayInputStream(bytes.toByteArray()),
                          () -> {
                            for (var table :
                                java.util.List.of(
                                    "projects",
                                    "availability_preferences",
                                    "appearance_preferences",
                                    "customization_preferences",
                                    "project_custom_field_values",
                                    "task_custom_field_values",
                                    "work_sessions",
                                    "work_session_changes"))
                              assertThat(
                                      jdbc.queryForObject(
                                          "SELECT count(*) FROM " + table + " WHERE owner_id=?",
                                          Integer.class,
                                          owner))
                                  .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM tasks WHERE project_id=?",
                                        Integer.class,
                                        ids[0]))
                                .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM task_status_history WHERE project_id=?",
                                        Integer.class,
                                        ids[0]))
                                .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM planned_blocks WHERE project_id=?",
                                        Integer.class,
                                        ids[0]))
                                .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM block_changes WHERE project_id=?",
                                        Integer.class,
                                        ids[0]))
                                .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM block_projections WHERE block_id=?",
                                        Integer.class,
                                        ids[2]))
                                .isPositive();
                            assertThat(
                                    jdbc.queryForObject(
                                        "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                                        Integer.class,
                                        ids[3]))
                                .isPositive();
                            reached.set(true);
                            return Instant.parse("2026-09-08T16:00:00Z");
                          }))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(reached).isTrue();
      assertThat(businessRows()).isEqualTo(before);
      assertThat(
              jdbc.queryForList(
                  "SELECT xmin::text,ctid::text,row_to_json(o)::text AS contents FROM outbox_events o ORDER BY event_id"))
          .isEqualTo(outbox);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_integral_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION reject_integral_receipt()");
    }
  }

  @Test
  void s2_allFourteenCollectionsAreRestoredTogetherAndUnrelatedFactsSurvive() throws Exception {
    var owner = "complete-import";
    var ids = seedCompleteAccount(owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T15:00:00.123456Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var expected = json.readTree(bytes.toByteArray());
    assertThat(expected.get("counts").size()).isEqualTo(14);
    expected.get("counts").forEach(count -> assertThat(count.intValue()).isPositive());
    var originalPreview =
        new PostgresImportDataStore(jdbc, manager)
            .preview(owner, new ByteArrayInputStream(bytes.toByteArray()));
    assertThat(originalPreview.identicalCounts())
        .isEqualTo(json.treeToValue(expected.get("counts"), ImportCounts.class));
    assertThat(originalPreview.insertCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    for (var table : java.util.List.of("block_changes", "task_status_history", "planned_blocks")) {
      if (table.equals("planned_blocks"))
        jdbc.update("DELETE FROM block_projections WHERE block_id=?", ids[2]);
      jdbc.update("DELETE FROM " + table + " WHERE project_id=?", ids[0]);
    }
    jdbc.update("DELETE FROM work_session_changes WHERE owner_id=?", owner);
    jdbc.update("DELETE FROM work_session_intervals WHERE session_id=?", ids[3]);
    for (var table :
        java.util.List.of(
            "work_sessions",
            "project_custom_field_values",
            "task_custom_field_values",
            "customization_preferences",
            "availability_preferences",
            "appearance_preferences"))
      jdbc.update("DELETE FROM " + table + " WHERE owner_id=?", owner);
    jdbc.update("DELETE FROM tasks WHERE project_id=?", ids[0]);
    jdbc.update("DELETE FROM projects WHERE id=?", ids[0]);
    var unrelated = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Otra idea','','idea',0,'2026-09-01Z','2026-09-01Z')",
        unrelated,
        owner);
    var untouched =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
            unrelated);
    var outboxBefore =
        jdbc.queryForList(
            "SELECT xmin::text,ctid::text,row_to_json(o)::text AS contents FROM outbox_events o ORDER BY event_id");
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var store = new PostgresImportDataStore(jdbc, manager);
    var receipt =
        store.apply(
            owner,
            java.util.UUID.randomUUID(),
            sha,
            new ByteArrayInputStream(input),
            () -> Instant.parse("2026-09-08T16:00:00.123456Z"));
    assertThat(receipt.outcome()).isEqualTo("IMPORTED");
    assertThat(receipt.insertedCounts())
        .isEqualTo(json.treeToValue(expected.get("counts"), ImportCounts.class));
    assertThat(receipt.identicalCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(store.find(owner, receipt.requestKey())).contains(receipt);
    var after = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T17:00:00.123456Z"))
        .writeTo(after);
    var data = json.readTree(after.toByteArray()).get("data");
    var projects = (com.fasterxml.jackson.databind.node.ArrayNode) data.get("projects");
    for (int i = projects.size() - 1; i >= 0; i--)
      if (projects.get(i).get("id").textValue().equals(unrelated.toString())) projects.remove(i);
    assertThat(data).isEqualTo(expected.get("data"));
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
                unrelated))
        .isEqualTo(untouched);
    assertThat(
            jdbc.queryForList(
                "SELECT xmin::text,ctid::text,row_to_json(o)::text AS contents FROM outbox_events o ORDER BY event_id"))
        .isEqualTo(outboxBefore);
    var physical = businessRows();
    var repeated =
        store.apply(
            owner,
            java.util.UUID.randomUUID(),
            sha,
            new ByteArrayInputStream(input),
            () -> Instant.parse("2026-09-08T18:00:00.123456Z"));
    assertThat(repeated.outcome()).isEqualTo("NO_CHANGE");
    assertThat(repeated.insertedCounts()).isEqualTo(receipt.identicalCounts());
    assertThat(repeated.identicalCounts()).isEqualTo(receipt.insertedCounts());
    assertThat(businessRows()).isEqualTo(physical);
  }

  private java.util.Map<String, java.util.List<java.util.Map<String, Object>>> businessRows() {
    var result =
        new java.util.LinkedHashMap<String, java.util.List<java.util.Map<String, Object>>>();
    for (var table :
        java.util.List.of(
            "projects",
            "tasks",
            "task_status_history",
            "availability_preferences",
            "planned_blocks",
            "block_projections",
            "block_changes",
            "work_sessions",
            "work_session_intervals",
            "work_session_changes",
            "appearance_preferences",
            "customization_preferences",
            "project_custom_field_values",
            "task_custom_field_values")) {
      result.put(
          table,
          jdbc.queryForList(
              "SELECT xmin::text,ctid::text,row_to_json(r)::text AS contents FROM "
                  + table
                  + " r ORDER BY contents"));
    }
    return result;
  }

  private java.util.UUID[] seedCompleteAccount(String owner) throws Exception {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    var block = java.util.UUID.randomUUID();
    var session = java.util.UUID.randomUUID();
    var json =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,' Proyecto ñ ',' Descripción ','idea',9007199254740993,'2026-09-01Z','2026-09-02Z')",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,version,created_at,updated_at) VALUES (?,?,' Tarea ',' Criterio ',30,'pending',2,'2026-09-01Z','2026-09-03Z')",
        task,
        project);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed','2026-09-02Z'),(?,?,?,2,'completed','pending','2026-09-03Z')",
        java.util.UUID.randomUUID(),
        project,
        task,
        java.util.UUID.randomUUID(),
        project,
        task);
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,'Europe/Madrid',0,1,30,60,120,1440,90,7,'2026-09-01Z','2026-09-02Z')",
        java.util.UUID.randomUUID(),
        owner);
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,'Objetivo','2026-09-08T16:00:00','2026-09-08T16:30:00','Europe/Madrid','+02:00','+02:00',true,'2026-09-08T14:00:00Z','2026-09-08T14:30:00Z',30,'2026-09-01Z')",
        block,
        project,
        task,
        java.util.UUID.randomUUID());
    var local = java.time.LocalDateTime.parse("2026-09-08T16:00:00");
    var offset = java.time.ZoneOffset.ofHours(2);
    var planned =
        new com.apptolast.organization.domain.PlannedBlock(
            block,
            project,
            task,
            new com.apptolast.organization.domain.BlockRequest(
                "Objetivo", local, local.plusMinutes(30), "Europe/Madrid", offset, offset, true),
            new com.apptolast.organization.domain.ResolvedBlockTime(
                local.toInstant(offset),
                local.plusMinutes(30).toInstant(offset),
                offset,
                offset,
                30),
            Instant.parse("2026-09-01T00:00:00Z"));
    var change =
        new com.apptolast.organization.domain.BlockChangeReceipt(
            java.util.UUID.randomUUID(),
            block,
            "CANCELLED",
            1,
            Instant.parse("2026-09-08T12:00:00Z"),
            planned,
            null);
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,1,'cancelled','2026-09-08T12:00:00Z')",
        block);
    jdbc.update(
        "INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt) VALUES (?,?,?,?,?,'CANCELLED',1,'2026-09-08T12:00:00Z',?::jsonb)",
        change.id(),
        project,
        task,
        block,
        java.util.UUID.randomUUID(),
        json.writeValueAsString(change));
    var started = Instant.parse("2026-09-08T12:00:00Z");
    var original =
        new com.apptolast.organization.domain.SessionStart(
            session, project, task, started, 25, started.plusSeconds(1500), "UTC");
    var before =
        new com.apptolast.organization.domain.WorkSessionState(
            original, "running", 1, started, 0, started);
    var paused =
        new com.apptolast.organization.domain.WorkSessionState(
            original, "paused", 2, started.plusSeconds(60), 60000000, null);
    var transition =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            java.util.UUID.randomUUID(), session, "PAUSE", started.plusSeconds(60), before, paused);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds) VALUES (?,?,?,?,?,'2026-09-08T12:00:00Z',25,'2026-09-08T12:25:00Z','UTC','paused',2,'2026-09-08T12:01:00Z',60000000)",
        session,
        owner,
        project,
        task,
        java.util.UUID.randomUUID());
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,2,'2026-09-08T12:00:00Z','2026-09-08T12:01:00Z')",
        session);
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,?,?,?,'PAUSE',1,'2026-09-08T12:01:00Z',?::jsonb)",
        transition.id(),
        owner,
        session,
        java.util.UUID.randomUUID(),
        json.writeValueAsString(transition));
    jdbc.update(
        "INSERT INTO appearance_preferences(id,owner_id,theme,accent_light,accent_dark,version,updated_at) VALUES (?,?,'DARK','#0000FF','#00FFFF',8,'2026-09-08T01:02:03.123456Z')",
        java.util.UUID.randomUUID(),
        owner);
    var projectField = java.util.UUID.randomUUID().toString();
    var taskField = java.util.UUID.randomUUID().toString();
    var projectDefinitions = json.createArrayNode();
    projectDefinitions
        .addObject()
        .put("id", projectField)
        .put("label", "Carga")
        .put("type", "NUMBER")
        .put("active", false);
    var taskDefinitions = json.createArrayNode();
    taskDefinitions
        .addObject()
        .put("id", taskField)
        .put("label", "Control")
        .put("type", "BOOLEAN")
        .put("active", false);
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,?,'PROJECT','[]'::jsonb,?::jsonb,7,'2026-09-08Z'),(?,?,'TASK','[\"estimatedMinutes\",\"completionCriterion\"]'::jsonb,?::jsonb,8,'2026-09-08Z')",
        java.util.UUID.randomUUID(),
        owner,
        projectDefinitions.toString(),
        java.util.UUID.randomUUID(),
        owner,
        taskDefinitions.toString());
    jdbc.update(
        "INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at) VALUES (?,?,?,?::jsonb,7,'2026-09-08Z')",
        java.util.UUID.randomUUID(),
        owner,
        project,
        json.createObjectNode().put(projectField, 0).toString());
    jdbc.update(
        "INSERT INTO task_custom_field_values(id,owner_id,task_id,field_values,version,updated_at) VALUES (?,?,?,?::jsonb,8,'2026-09-08Z')",
        java.util.UUID.randomUUID(),
        owner,
        task,
        json.createObjectNode().put(taskField, false).toString());
    return new java.util.UUID[] {project, task, block, session};
  }

  @Test
  void s9_fractionalTaskEstimateCannotBeRoundedIntoAValidMinute() throws Exception {
    var owner = "fractional-task-estimate";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = taskFile(owner);
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("tasks").get(0))
        .put("estimatedMinutes", new java.math.BigDecimal("1.25"));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s9_integralDecimalTaskEstimateIsAcceptedAndComparedAsAnInteger() throws Exception {
    var owner = "decimal-task-estimate";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = taskFile(owner);
    var task =
        (com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("tasks").get(0);
    task.put("estimatedMinutes", new java.math.BigDecimal("1.0"));
    var input = json.writeValueAsBytes(file);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var store = new PostgresImportDataStore(jdbc, manager);
    var result =
        store.apply(
            owner,
            java.util.UUID.randomUUID(),
            sha,
            new ByteArrayInputStream(input),
            () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(result.outcome()).isEqualTo("IMPORTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT estimated_minutes FROM tasks WHERE id=?",
                Integer.class,
                java.util.UUID.fromString(task.get("id").textValue())))
        .isEqualTo(1);
    var repeated = store.preview(owner, new ByteArrayInputStream(input));
    assertThat(repeated.identicalCounts()).isEqualTo(result.insertedCounts());
    assertThat(repeated.insertCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
  }

  @Test
  void s2_availabilityIsRestoredAsOneCompletePreference() throws Exception {
    var owner = "import-availability";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000240");
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,'Europe/Madrid',0,1,30,60,120,1440,90,9223372036854775807,'2026-09-01Z','2026-09-02Z')",
        id,
        owner);
    var before =
        jdbc.queryForMap(
            "SELECT row_to_json(a)::text AS contents FROM availability_preferences a WHERE owner_id=?",
            owner);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    jdbc.update("DELETE FROM availability_preferences WHERE owner_id=?", owner);
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(result.outcome()).isEqualTo("IMPORTED");
    assertThat(result.insertedCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT row_to_json(a)::text AS contents FROM availability_preferences a WHERE owner_id=?",
                owner))
        .isEqualTo(before);
    var physical =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(a)::text AS contents FROM availability_preferences a WHERE owner_id=?",
            owner);
    var repeated =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T04:05:06.123456Z"));
    assertThat(repeated.outcome()).isEqualTo("NO_CHANGE");
    assertThat(repeated.identicalCounts()).isEqualTo(result.insertedCounts());
    assertThat(repeated.insertedCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(a)::text AS contents FROM availability_preferences a WHERE owner_id=?",
                owner))
        .isEqualTo(physical);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM appearance_preferences WHERE owner_id=?",
                Integer.class,
                owner))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s2_taskStatusHistoryIsRestoredWithoutReplayingItsTransitions() throws Exception {
    var owner = "import-task-history";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = taskFile(owner);
    var project =
        (com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("projects").get(0);
    project.put("id", "00000000-0000-0000-0000-000000000230");
    var task =
        (com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("tasks").get(0);
    task.put("id", "00000000-0000-0000-0000-000000000231")
        .put("projectId", project.get("id").textValue())
        .put("version", "2")
        .put("updatedAt", "2025-01-01T01:00:00.000002Z");
    var history =
        (com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("taskStatusHistory");
    history
        .addObject()
        .put("id", "00000000-0000-0000-0000-000000000232")
        .put("projectId", project.get("id").textValue())
        .put("taskId", task.get("id").textValue())
        .put("taskVersion", "1")
        .put("fromStatus", "pending")
        .put("toStatus", "completed")
        .put("occurredAt", "2025-01-01T00:30:00.000001Z");
    history
        .addObject()
        .put("id", "00000000-0000-0000-0000-000000000233")
        .put("projectId", project.get("id").textValue())
        .put("taskId", task.get("id").textValue())
        .put("taskVersion", "2")
        .put("fromStatus", "completed")
        .put("toStatus", "pending")
        .put("occurredAt", "2025-01-01T01:00:00.000002Z");
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts"))
        .put("taskStatusHistory", 2);
    var input = json.writeValueAsBytes(file);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(result.insertedCounts())
        .isEqualTo(new ImportCounts(1, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    var after = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T03:04:05.123456Z"))
        .writeTo(after);
    assertThat(json.readTree(after.toByteArray()).get("data")).isEqualTo(file.get("data"));
    var historyBefore =
        jdbc.queryForList(
            "SELECT xmin::text,ctid::text,row_to_json(h)::text AS contents FROM task_status_history h WHERE project_id=? ORDER BY task_version",
            java.util.UUID.fromString(project.get("id").textValue()));
    var repeated =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T04:05:06.123456Z"));
    assertThat(repeated.outcome()).isEqualTo("NO_CHANGE");
    assertThat(repeated.identicalCounts()).isEqualTo(result.insertedCounts());
    assertThat(repeated.insertedCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForList(
                "SELECT xmin::text,ctid::text,row_to_json(h)::text AS contents FROM task_status_history h WHERE project_id=? ORDER BY task_version",
                java.util.UUID.fromString(project.get("id").textValue())))
        .isEqualTo(historyBefore);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s10_taskParentCycleIsInvalidEvenWhenEveryIdentityExistsInTheFile() throws Exception {
    var owner = "task-cycle";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = taskFile(owner);
    var tasks = (com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("tasks");
    var first = (com.fasterxml.jackson.databind.node.ObjectNode) tasks.get(0);
    var second = first.deepCopy();
    second
        .put("id", "00000000-0000-0000-0000-000000000128")
        .put("parentId", first.get("id").textValue());
    first.put("parentId", second.get("id").textValue());
    tasks.add(second);
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("tasks", 2);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s10_taskCannotBorrowItsMissingProjectFromTheDestination() throws Exception {
    var owner = "missing-file-project";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = taskFile(owner);
    var projectId =
        java.util.UUID.fromString(file.get("data").get("projects").get(0).get("id").textValue());
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Proyecto','','idea',0,'2025-01-01Z','2025-01-01Z')",
        projectId,
        owner);
    ((com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("projects")).removeAll();
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("projects", 0);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM tasks WHERE project_id=?", Integer.class, projectId))
        .isZero();
  }

  private com.fasterxml.jackson.databind.JsonNode taskFile(String owner) throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(invalidProjectText(owner, "Proyecto"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("projects").get(0))
        .put("id", java.util.UUID.randomUUID().toString());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("tasks", 1);
    var task =
        ((com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("tasks")).addObject();
    task.put("id", java.util.UUID.randomUUID().toString())
        .put("projectId", file.get("data").get("projects").get(0).get("id").textValue())
        .putNull("parentId")
        .put("title", "Tarea")
        .put("completionCriterion", "")
        .putNull("estimatedMinutes")
        .put("status", "pending")
        .put("version", "0")
        .putNull("completedAt")
        .put("createdAt", "2025-01-01T00:00:00.000001Z")
        .put("updatedAt", "2025-01-01T00:00:00.000001Z");
    return file;
  }

  @Test
  void s8_projectLengthUsesTheOriginalTextRatherThanTheNormalizedConstructorValue()
      throws Exception {
    var owner = "raw-project-length";
    var input = invalidProjectText(owner, " " + "a".repeat(120) + " ");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(input)))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s3_identicalTaskIsNotUpdatedOrReinserted() throws Exception {
    var owner = "identical-task";
    var project = java.util.UUID.fromString("00000000-0000-0000-0000-000000000210");
    var task = java.util.UUID.fromString("00000000-0000-0000-0000-000000000211");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Proyecto','','idea',0,'2026-09-01Z','2026-09-01Z')",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,version,created_at,updated_at) VALUES (?,?,' Tarea ñ ',' Criterio ',null,'pending',9223372036854775807,'2026-09-01Z','2026-09-01Z')",
        task,
        project);
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(t)::text AS contents FROM tasks t WHERE id=?",
            task);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(result.outcome()).isEqualTo("NO_CHANGE");
    assertThat(result.identicalCounts())
        .isEqualTo(new ImportCounts(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(result.insertedCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(t)::text AS contents FROM tasks t WHERE id=?",
                task))
        .isEqualTo(before);
  }

  @Test
  void s2_tasksAndSubtasksKeepAllFactsWhenTheirParentAppearsLaterInTheFile() throws Exception {
    var owner = "import-tasks";
    var project = java.util.UUID.fromString("00000000-0000-0000-0000-000000000200");
    var parent = java.util.UUID.fromString("00000000-0000-0000-0000-000000000202");
    var child = java.util.UUID.fromString("00000000-0000-0000-0000-000000000201");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,'Proyecto','','idea',0,'2026-09-01Z','2026-09-01Z')",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,version,created_at,updated_at) VALUES (?,?,'Padre','',null,'pending',0,'2026-09-01Z','2026-09-01Z')",
        parent,
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,estimated_minutes,status,version,completed_at,created_at,updated_at) VALUES (?,?,?,' Hija ñ ',' Resultado ',25,'completed',9007199254740993,'2026-09-02Z','2026-09-01Z','2026-09-02Z')",
        child,
        project,
        parent);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var input = bytes.toByteArray();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var expected = json.readTree(input);
    assertThat(expected.get("data").get("tasks").get(0).get("parentId").textValue())
        .isEqualTo(parent.toString());
    jdbc.update("DELETE FROM tasks WHERE project_id=?", project);
    jdbc.update("DELETE FROM projects WHERE id=?", project);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .apply(
                owner,
                java.util.UUID.randomUUID(),
                sha,
                new ByteArrayInputStream(input),
                () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(result.insertedCounts())
        .isEqualTo(new ImportCounts(1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    var after = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T03:04:05.123456Z"))
        .writeTo(after);
    assertThat(json.readTree(after.toByteArray()).get("data")).isEqualTo(expected.get("data"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s8_projectStateMustSatisfyTheExistingDomainBeforePlanning() throws Exception {
    var owner = "invalid-project-state";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(invalidProjectText(owner, "Proyecto válido"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("projects").get(0))
        .put("status", "archived");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_negativeProjectVersionIsInvalidBeforeSqlInsertion() throws Exception {
    var owner = "negative-project-version";
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(invalidProjectText(owner, "Proyecto válido"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("data").get("projects").get(0))
        .put("version", "-1");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_unpairedEscapedSurrogateCannotBeSilentlyReplacedDuringStaging() throws Exception {
    var owner = "unpaired-surrogate";
    var input = invalidProjectText(owner, "Proyecto" + (char) 0xd800);
    assertThat(
            new String(input, java.nio.charset.StandardCharsets.UTF_8)
                .toLowerCase(java.util.Locale.ROOT))
        .contains("\\ud800");
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .apply(
                        owner,
                        java.util.UUID.randomUUID(),
                        sha,
                        new ByteArrayInputStream(input),
                        () -> Instant.parse("2026-09-08T02:03:04.123456Z")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id=?", Integer.class, owner))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s8_matchingHashRejectsEscapedNulAsInvalidFileNotStorageFailure() throws Exception {
    var owner = "nul-after-hash";
    var input = invalidProjectText(owner, "Proyecto" + (char) 0);
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .apply(
                        owner,
                        java.util.UUID.randomUUID(),
                        sha,
                        new ByteArrayInputStream(input),
                        () -> {
                          throw new AssertionError("Invalid file must not read the clock");
                        }))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  private byte[] invalidProjectText(String owner, String name) throws Exception {
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty(owner, Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("projects", 1);
    ((com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("projects"))
        .addObject()
        .put("id", "00000000-0000-0000-0000-000000000126")
        .put("name", name)
        .put("description", "")
        .put("status", "idea")
        .put("version", "0")
        .put("createdAt", "2025-01-01T00:00:00.000001Z")
        .put("updatedAt", "2025-01-01T00:00:00.000001Z");
    return json.writeValueAsBytes(file);
  }

  @Test
  void s20_hashMismatchPrecedesJsonbRejectionOfEscapedNul() throws Exception {
    var owner = "nul-before-hash";
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty(owner, Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("projects", 1);
    ((com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("projects"))
        .addObject()
        .put("id", "00000000-0000-0000-0000-000000000125")
        .put("name", "Proyecto" + (char) 0)
        .put("description", "")
        .put("status", "idea")
        .put("version", "0")
        .put("createdAt", "2025-01-01T00:00:00.000001Z")
        .put("updatedAt", "2025-01-01T00:00:00.000001Z");
    var input = json.writeValueAsBytes(file);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .apply(
                        owner,
                        java.util.UUID.randomUUID(),
                        "0".repeat(64),
                        new ByteArrayInputStream(input),
                        () -> {
                          throw new AssertionError("Invalid file must not read the clock");
                        }))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s8_projectVersionMustRemainATextualBigint() throws Exception {
    var owner = "invalid-project-version";
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty(owner, Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var file = json.readTree(bytes.toByteArray());
    ((com.fasterxml.jackson.databind.node.ObjectNode) file.get("counts")).put("projects", 1);
    ((com.fasterxml.jackson.databind.node.ArrayNode) file.get("data").get("projects"))
        .addObject()
        .put("id", "00000000-0000-0000-0000-000000000124")
        .put("name", "Proyecto")
        .put("description", "")
        .put("status", "idea")
        .put("version", 9007199254740993L)
        .put("createdAt", "2025-01-01T00:00:00.000001Z")
        .put("updatedAt", "2025-01-01T00:00:00.000001Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(json.writeValueAsBytes(file))))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_unknownProjectFieldIsInvalidBeforeDestinationComparison() throws Exception {
    var owner = "invalid-project-field";
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .prepare(
            owner,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            (collection, json) -> {
              if (!collection.equals("projects")) return 0;
              json.writeStartObject();
              json.writeStringField("id", "00000000-0000-0000-0000-000000000123");
              json.writeStringField("name", "Proyecto");
              json.writeStringField("description", "");
              json.writeStringField("status", "idea");
              json.writeStringField("version", "0");
              json.writeStringField("createdAt", "2025-01-01T00:00:00.000001Z");
              json.writeStringField("updatedAt", "2025-01-01T00:00:00.000001Z");
              json.writeStringField("unexpected", "private payload");
              json.writeEndObject();
              return 1;
            })
        .writeTo(bytes);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(bytes.toByteArray())))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class)
        .hasMessageNotContaining("private payload");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s3_existingProjectApplyKeepsPhysicalRowAndRecordsNoChange() throws Exception {
    var owner = "apply-identical-project";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,'nota','idea',9223372036854775807,'2025-01-01T00:00:00.000001Z','2025-01-02T00:00:00.000002Z')",
        id,
        owner,
        " Proyecto intacto ");
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
            id);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var store = new PostgresImportDataStore(jdbc, manager);
    var receipt =
        store.apply(
            owner,
            java.util.UUID.randomUUID(),
            sha,
            new ByteArrayInputStream(input),
            () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(receipt.outcome()).isEqualTo("NO_CHANGE");
    assertThat(receipt.identicalCounts())
        .isEqualTo(new ImportCounts(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(receipt.insertedCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
                id))
        .isEqualTo(before);
    assertThat(store.find(owner, receipt.requestKey())).contains(receipt);
  }

  @Test
  void s2_absentProjectIsInsertedWithItsOriginalFactsAndAtomicReceipt() throws Exception {
    var owner = "insert-project";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000888");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,'nota original','idea',9007199254740993,'2025-01-01T00:00:00.000001Z','2025-01-02T00:00:00.000002Z')",
        id,
        owner,
        " Proyecto histórico ñ ");
    var original =
        jdbc.queryForMap("SELECT row_to_json(p)::text AS contents FROM projects p WHERE id=?", id);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    jdbc.update("DELETE FROM projects WHERE id=?", id);
    var input = bytes.toByteArray();
    var sha =
        java.util.HexFormat.of()
            .formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input));
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000889");
    var store = new PostgresImportDataStore(jdbc, manager);
    var receipt =
        store.apply(
            owner,
            key,
            sha,
            new ByteArrayInputStream(input),
            () -> Instant.parse("2026-09-08T02:03:04.123456Z"));
    assertThat(receipt.outcome()).isEqualTo("IMPORTED");
    assertThat(receipt.insertedCounts())
        .isEqualTo(new ImportCounts(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(receipt.identicalCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT row_to_json(p)::text AS contents FROM projects p WHERE id=?", id))
        .isEqualTo(original);
    assertThat(store.find(owner, key)).contains(receipt);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s4_previewRejectsChangedProjectWithoutChoosingTheNewerVersion() throws Exception {
    var owner = "changed-project";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000777");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,'nota','idea',1,'2025-01-01T00:00:00.000001Z','2025-01-02T00:00:00.000002Z')",
        id,
        owner,
        " Proyecto original ");
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    jdbc.update("UPDATE projects SET name='Proyecto cambiado',version=2 WHERE id=?", id);
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
            id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview(owner, new ByteArrayInputStream(bytes.toByteArray())))
        .isInstanceOf(com.apptolast.organization.application.ImportConflictException.class);
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
                id))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s3_s18_identicalProjectIsCountedWithoutUpdatingTheExistingRow() throws Exception {
    var owner = "identical-project";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000666");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,'nota','idea',9007199254740993,'2025-01-01T00:00:00.000001Z','2025-01-02T00:00:00.000002Z')",
        id,
        owner,
        "  Proyecto ñ  ");
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
            id);
    var bytes = new ByteArrayOutputStream();
    new PostgresExportDataQueries(jdbc, manager)
        .prepare(owner, () -> Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .preview(owner, new ByteArrayInputStream(bytes.toByteArray()));
    assertThat(result.counts().projects()).isEqualTo(1);
    assertThat(result.identicalCounts()).isEqualTo(result.counts());
    assertThat(result.insertCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(p)::text AS contents FROM projects p WHERE id=?",
                id))
        .isEqualTo(before);
  }

  @Test
  void s18_absentProjectIsPlannedWithoutWritingOrNormalizingItsHistory() throws Exception {
    var owner = "project-preview";
    var id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000555");
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .prepare(
            owner,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            (collection, json) -> {
              if (!collection.equals("projects")) return 0;
              json.writeStartObject();
              json.writeStringField("id", id.toString());
              json.writeStringField("name", "  Histórico ñ  ");
              json.writeStringField("description", "nota conservada");
              json.writeStringField("status", "idea");
              json.writeStringField("version", "9007199254740993");
              json.writeStringField("createdAt", "2025-01-01T00:00:00.000001Z");
              json.writeStringField("updatedAt", "2025-01-02T00:00:00.000002Z");
              json.writeEndObject();
              return 1;
            })
        .writeTo(bytes);
    var result =
        new PostgresImportDataStore(jdbc, manager)
            .preview(owner, new ByteArrayInputStream(bytes.toByteArray()));
    assertThat(result.counts().projects()).isEqualTo(1);
    assertThat(result.insertCounts()).isEqualTo(result.counts());
    assertThat(result.identicalCounts())
        .isEqualTo(new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=?", Integer.class, id))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

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
  void s23_suppressedReceiptInsertCannotReportAFalseSuccess() throws Exception {
    var owner = "receipt-suppressed";
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    jdbc.execute(
        "CREATE FUNCTION suppress_import_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_import_receipt BEFORE INSERT ON import_receipts FOR EACH ROW WHEN (NEW.owner_id='receipt-suppressed') EXECUTE FUNCTION suppress_import_receipt()");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .apply(
                          owner,
                          java.util.UUID.randomUUID(),
                          sha,
                          new ByteArrayInputStream(bytes.toByteArray()),
                          () -> timestamp))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER suppress_import_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION suppress_import_receipt()");
    }
  }

  @Test
  void s23_receiptInsertFailureIsStorageUnavailableAndLeavesNoCommittedReceipt() throws Exception {
    var owner = "receipt-failure";
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    jdbc.execute(
        "CREATE FUNCTION reject_import_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled receipt failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_import_receipt BEFORE INSERT ON import_receipts FOR EACH ROW WHEN (NEW.owner_id='receipt-failure') EXECUTE FUNCTION reject_import_receipt()");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .apply(
                          owner,
                          java.util.UUID.randomUUID(),
                          sha,
                          new ByteArrayInputStream(bytes.toByteArray()),
                          () -> timestamp))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_import_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION reject_import_receipt()");
    }
  }

  @Test
  void s21_reusingACommittedKeyWithDifferentValidBytesCannotReturnItsReceipt() throws Exception {
    var owner = "reused-key";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000444");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var digest = java.security.MessageDigest.getInstance("SHA-256");
    var sha = java.util.HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var original =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    bytes.write(' ');
    var otherSha = java.util.HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                store.apply(
                    owner,
                    key,
                    otherSha,
                    new ByteArrayInputStream(bytes.toByteArray()),
                    () -> {
                      throw new AssertionError("Reused key must not consult Clock");
                    }))
        .isInstanceOf(com.apptolast.organization.application.ImportKeyReusedException.class);
    assertThat(store.find(owner, key)).contains(original);
  }

  @Test
  void s21_replayReturnsTheOriginalReceiptWithoutClockOrPhysicalRewrite() throws Exception {
    var owner = "replay-owner";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000333");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var original =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(r)::text AS contents FROM import_receipts r WHERE owner_id=? AND request_key=?",
            owner,
            key);
    var replay =
        store.apply(
            owner,
            key,
            sha,
            new ByteArrayInputStream(bytes.toByteArray()),
            () -> {
              throw new AssertionError("Replay must not consult Clock");
            });
    assertThat(replay).isEqualTo(original);
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(r)::text AS contents FROM import_receipts r WHERE owner_id=? AND request_key=?",
                owner,
                key))
        .isEqualTo(before);
  }

  @Test
  void s20_hashMismatchPrecedesOwnerValidationAndDoesNotCreateAReceipt() throws Exception {
    var owner = "hash-mismatch";
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("different-file-owner", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .apply(
                        owner,
                        java.util.UUID.randomUUID(),
                        "b".repeat(64),
                        new ByteArrayInputStream(bytes.toByteArray()),
                        () -> {
                          throw new AssertionError("Rejected request must not consult Clock");
                        }))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s22_committedReceiptCanBeReadWithoutTheOriginalFile() throws Exception {
    var owner = "read-receipt";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000222");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var receipt =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    var result =
        new com.apptolast.organization.application.ReadImportReceipt(store).find(owner, key);
    assertThat(result).contains(receipt);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isEqualTo(1);
  }

  @Test
  void s1_emptyApplyLocksTheDurableTablesAndCommitsOnlyItsOperationalReceipt() throws Exception {
    var owner = "empty-apply";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000111");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var clock = org.mockito.Mockito.mock(java.time.Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenAnswer(
            invocation -> {
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("read committed");
              assertThat(jdbc.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("2s");
              assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class))
                  .isEqualTo("10s");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory' AND granted",
                          Integer.class))
                  .isEqualTo(2);
              assertThat(
                      jdbc.queryForList(
                          "SELECT c.relname FROM pg_locks l JOIN pg_class c ON c.oid=l.relation WHERE l.pid=pg_backend_pid() AND l.mode='ExclusiveLock' AND l.granted ORDER BY c.relname",
                          String.class))
                  .containsExactly(
                      "appearance_preferences",
                      "availability_preferences",
                      "block_changes",
                      "block_projections",
                      "customization_preferences",
                      "import_receipts",
                      "planned_blocks",
                      "project_custom_field_values",
                      "projects",
                      "task_custom_field_values",
                      "task_status_history",
                      "tasks",
                      "work_session_changes",
                      "work_session_intervals",
                      "work_sessions");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM import_receipts WHERE owner_id=?",
                          Integer.class,
                          owner))
                  .isZero();
              return timestamp.plusNanos(789);
            });
    var receipt =
        new com.apptolast.organization.application.ApplyImportData(
                new PostgresImportDataStore(jdbc, manager), clock)
            .apply(owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()));
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    assertThat(receipt)
        .isEqualTo(
            new com.apptolast.organization.application.ImportReceipt(
                key, sha, bytes.size(), timestamp, "NO_CHANGE", zero, zero));
    assertThat(
            jdbc.queryForObject(
                "SELECT file_sha256 FROM import_receipts WHERE owner_id=? AND request_key=?",
                String.class,
                owner,
                key))
        .isEqualTo(sha);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id=?", Integer.class, owner))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
    org.mockito.Mockito.verify(clock).instant();
  }

  @Test
  void s7_previewCannotAdoptTheOwnerFromTheFile() throws Exception {
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("private-other", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview("authenticated", new ByteArrayInputStream(bytes.toByteArray())))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class)
        .hasMessage("El archivo de importación no es válido.");
  }

  @Test
  void s1_s18_emptyPreviewUsesARepeatableSnapshotAndDoesNotCreateDefaultsOrOutbox()
      throws Exception {
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty("empty-preview", timestamp).writeTo(bytes);
    var body =
        new ByteArrayInputStream(bytes.toByteArray()) {
          @Override
          public synchronized int read(byte[] buffer, int offset, int length) {
            assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                .isEqualTo("repeatable read");
            return super.read(buffer, offset, length);
          }
        };
    var result =
        new PreviewImportData(new PostgresImportDataStore(jdbc, manager))
            .preview("empty-preview", body);
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    assertThat(result.owner()).isEqualTo("empty-preview");
    assertThat(result.exportedAt()).isEqualTo(timestamp);
    assertThat(result.counts()).isEqualTo(zero);
    assertThat(result.insertCounts()).isEqualTo(zero);
    assertThat(result.identicalCounts()).isEqualTo(zero);
    assertThat(result.runningSessions()).isEmpty();
    for (var table :
        java.util.List.of(
            "projects",
            "tasks",
            "availability_preferences",
            "appearance_preferences",
            "customization_preferences",
            "outbox_events")) {
      String sql =
          table.equals("tasks")
              ? "SELECT count(*) FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=?"
              : "SELECT count(*) FROM " + table + " WHERE owner_id=?";
      assertThat(jdbc.queryForObject(sql, Integer.class, "empty-preview")).isZero();
    }
  }
}
