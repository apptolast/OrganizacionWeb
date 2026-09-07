package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomFieldValuesPersistenceTest {
  @Test
  void s20_failedUpdatePreservesTheWholePreviousCollectionIncludingInactiveValues()
      throws Exception {
    var project = UUID.randomUUID();
    var textId = UUID.randomUUID();
    var numberId = UUID.randomUUID();
    var hiddenId = UUID.randomUUID();
    var schemaId = UUID.randomUUID();
    var collectionId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var json = new ObjectMapper();
    var definitions =
        List.of(
            new CustomFieldDefinition(textId, "Texto", CustomFieldType.TEXT, true),
            new CustomFieldDefinition(numberId, "Número", CustomFieldType.NUMBER, true),
            new CustomFieldDefinition(hiddenId, "Oculto", CustomFieldType.TEXT, false));
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]',?::jsonb,4,'2026-09-07T20:00:00Z')",
        schemaId,
        json.writeValueAsString(definitions));
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,2,'2026-09-07T20:00:00Z')",
        collectionId,
        project,
        json.writeValueAsString(Map.of(textId, "anterior", numberId, 1, hiddenId, "preservado")));
    var store = new PostgresCustomizationStore(jdbc, manager, json);
    var before = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    var rowsBefore =
        jdbc.queryForList("SELECT xmin::text,ctid::text,* FROM project_custom_field_values");
    var schemaBefore = jdbc.queryForList("SELECT * FROM customization_preferences");
    var projectBefore = jdbc.queryForList("SELECT * FROM projects");
    jdbc.execute(
        "CREATE FUNCTION fail_customization_update() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled failure after whole values UPDATE'; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_customization AFTER UPDATE ON project_custom_field_values FOR EACH ROW EXECUTE FUNCTION fail_customization_update()");
    try {
      assertThatThrownBy(
              () ->
                  new SaveCustomFieldValues(
                          store, Clock.fixed(Instant.parse("2026-09-07T21:00:00Z"), ZoneOffset.UTC))
                      .save(
                          "owner-a",
                          CustomizationScope.PROJECT,
                          project,
                          project,
                          new CustomFieldValuesRevision(
                              CustomizationScope.PROJECT,
                              project,
                              before.schema(),
                              before.revision()),
                          List.of(
                              new CustomFieldInput(textId, "nuevo"),
                              new CustomFieldInput(numberId, new java.math.BigDecimal("2")))))
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(store.find("owner-a", CustomizationScope.PROJECT, project, project))
          .isEqualTo(before);
      assertThat(
              jdbc.queryForList("SELECT xmin::text,ctid::text,* FROM project_custom_field_values"))
          .isEqualTo(rowsBefore);
      assertThat(jdbc.queryForList("SELECT * FROM customization_preferences"))
          .isEqualTo(schemaBefore);
      assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projectBefore);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_customization ON project_custom_field_values");
      jdbc.execute("DROP FUNCTION fail_customization_update()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"definition", "key"})
  void s20_persistedUuidTextCannotUseAnAbbreviatedAlias(String target) {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var abbreviated = "1-1-1-1-1";
    var canonical = UUID.fromString(abbreviated).toString();
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]',?::jsonb,0,now())",
        UUID.randomUUID(),
        "[{\"id\":\""
            + (target.equals("definition") ? abbreviated : canonical)
            + "\",\"label\":\"Dato\",\"type\":\"TEXT\",\"active\":true}]");
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,0,now())",
        UUID.randomUUID(),
        project,
        "{\"" + (target.equals("key") ? abbreviated : canonical) + "\":\"valor\"}");
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    assertThatThrownBy(() -> store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "configuration,null",
    "configuration,object",
    "configuration,scalar",
    "configuration,thirteen",
    "project,null",
    "project,array",
    "project,scalar",
    "task,null",
    "task,array",
    "task,scalar"
  })
  void s20_selectedJsonRootAndDefinitionLimitAreStrictEvenAcrossAControlledProjection(
      String target, String defect) throws Exception {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var table =
        target.equals("configuration")
            ? "customization_preferences"
            : target.equals("project") ? "project_custom_field_values" : "task_custom_field_values";
    if (target.equals("configuration"))
      jdbc.update(
          "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]','[]',0,now())",
          UUID.randomUUID());
    else
      jdbc.update(
          "INSERT INTO " + table + " VALUES (?,'owner-a',?,'{}',0,now())",
          UUID.randomUUID(),
          target.equals("project") ? project : task);
    var invalid =
        switch (defect) {
          case "object" -> "{}";
          case "array" -> "[]";
          case "scalar" -> "1";
          case "thirteen" ->
              new ObjectMapper()
                  .writeValueAsString(
                      java.util.stream.IntStream.range(0, 13)
                          .mapToObj(
                              index ->
                                  new CustomFieldDefinition(
                                      UUID.randomUUID(),
                                      "Dato " + index,
                                      CustomFieldType.TEXT,
                                      true))
                          .toList());
          default -> "null";
        };
    var projection =
        target.equals("configuration")
            ? "scope,visible_fields,'" + invalid + "'::jsonb AS custom_fields"
            : (target.equals("project") ? "project_id" : "task_id")
                + ",'"
                + invalid
                + "'::jsonb AS field_values";
    jdbc.execute("ALTER TABLE " + table + " RENAME TO customization_corrupt_row");
    try {
      jdbc.execute(
          "CREATE VIEW "
              + table
              + " AS SELECT id,owner_id,"
              + projection
              + ",version,updated_at FROM customization_corrupt_row");
      var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
      assertThatThrownBy(
              () -> {
                if (target.equals("configuration"))
                  store.find("owner-a", CustomizationScope.PROJECT);
                else
                  store.find(
                      "owner-a",
                      target.equals("project")
                          ? CustomizationScope.PROJECT
                          : CustomizationScope.TASK,
                      project,
                      target.equals("project") ? project : task);
              })
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP VIEW IF EXISTS " + table);
      jdbc.execute("ALTER TABLE customization_corrupt_row RENAME TO " + table);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "configuration,id-null",
    "configuration,version-null",
    "configuration,version-negative",
    "project,id-null",
    "project,version-null",
    "project,version-negative",
    "task,id-null",
    "task,version-null",
    "task,version-negative"
  })
  void s20_selectedMetadataCannotCoerceNullIntoAnUnconfiguredRevision(
      String target, String defect) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var table =
        target.equals("configuration")
            ? "customization_preferences"
            : target.equals("project") ? "project_custom_field_values" : "task_custom_field_values";
    if (target.equals("configuration"))
      jdbc.update(
          "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]','[]',0,now())",
          UUID.randomUUID());
    else
      jdbc.update(
          "INSERT INTO " + table + " VALUES (?,'owner-a',?,'{}',0,now())",
          UUID.randomUUID(),
          target.equals("project") ? project : task);
    var middle =
        target.equals("configuration")
            ? "owner_id,scope,visible_fields,custom_fields"
            : target.equals("project")
                ? "owner_id,project_id,field_values"
                : "owner_id,task_id,field_values";
    jdbc.execute("ALTER TABLE " + table + " RENAME TO customization_corrupt_row");
    try {
      jdbc.execute(
          "CREATE VIEW "
              + table
              + " AS SELECT "
              + (defect.equals("id-null") ? "NULL::uuid" : "id")
              + " AS id,"
              + middle
              + ","
              + (defect.equals("version-null")
                  ? "NULL::bigint"
                  : defect.equals("version-negative") ? "-1::bigint" : "version")
              + " AS version,updated_at FROM customization_corrupt_row");
      var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
      assertThatThrownBy(
              () -> {
                if (target.equals("configuration"))
                  store.find("owner-a", CustomizationScope.PROJECT);
                else
                  store.find(
                      "owner-a",
                      target.equals("project")
                          ? CustomizationScope.PROJECT
                          : CustomizationScope.TASK,
                      project,
                      target.equals("project") ? project : task);
              })
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP VIEW IF EXISTS " + table);
      jdbc.execute("ALTER TABLE customization_corrupt_row RENAME TO " + table);
    }
  }

  @Test
  void s11_s19_completedProjectAcceptsAThousandUnicodePointsWithoutChangingBusinessFacts() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at,version) VALUES (?,'owner-a','P','','completed','2026-09-07T20:00:00Z','2026-09-07T21:00:00Z',4)",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T22:00:00Z"), ZoneOffset.UTC);
    var schema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Nota",
                CustomFieldType.TEXT);
    var field = schema.customFields().getFirst().id();
    var before = jdbc.queryForList("SELECT * FROM projects");
    var text = "\ud83d\ude00".repeat(1000);
    var saved =
        new SaveCustomFieldValues(store, clock)
            .save(
                "owner-a",
                CustomizationScope.PROJECT,
                project,
                project,
                new CustomFieldValuesRevision(
                    CustomizationScope.PROJECT,
                    project,
                    new CustomizationRevision(schema.id(), 0),
                    new CustomizationRevision(null, 0)),
                List.of(new CustomFieldInput(field, text)));
    assertThat(saved.values().getFirst().value()).isEqualTo(text);
    assertThat(store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isEqualTo(saved);
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"deactivate", "reactivate", "rename", "restore-view"})
  void s7_s21_viewAndDefinitionChangesPreserveHiddenValuesAtMaximumRevision(String action) {
    var project = UUID.randomUUID();
    var field = UUID.randomUUID();
    var schemaId = UUID.randomUUID();
    var valuesId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[\"updatedAt\"]',?::jsonb,2,'2026-09-07T20:00:00Z')",
        schemaId,
        "[{\"id\":\""
            + field
            + "\",\"label\":\"Dato\",\"type\":\"TEXT\",\"active\":"
            + !action.equals("reactivate")
            + "}]");
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,?,'2026-09-07T20:00:00Z')",
        valuesId,
        project,
        "{\"" + field + "\":\"anotación\"}",
        Long.MAX_VALUE);
    var before =
        jdbc.queryForList("SELECT xmin::text,ctid::text,* FROM project_custom_field_values");
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T21:00:00Z"), ZoneOffset.UTC);
    Customization result;
    if (action.equals("restore-view"))
      result =
          new SaveCustomizationView(store, clock)
              .save(
                  "owner-a",
                  CustomizationScope.PROJECT,
                  new CustomizationRevision(schemaId, 2),
                  List.of("createdAt"));
    else
      result =
          new UpdateCustomField(store, clock)
              .update(
                  "owner-a",
                  CustomizationScope.PROJECT,
                  field,
                  new CustomizationRevision(schemaId, 2),
                  "Nombre actual",
                  !action.equals("deactivate"));
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.customFields().getFirst().id()).isEqualTo(field);
    assertThat(result.customFields().getFirst().type()).isEqualTo(CustomFieldType.TEXT);
    assertThat(jdbc.queryForList("SELECT xmin::text,ctid::text,* FROM project_custom_field_values"))
        .isEqualTo(before);
    var values = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    assertThat(values.revision()).isEqualTo(new CustomizationRevision(valuesId, Long.MAX_VALUE));
    if (action.equals("deactivate")) assertThat(values.values()).isEmpty();
    else
      assertThat(values.values())
          .containsExactly(
              new CustomFieldValue(
                  field,
                  action.equals("restore-view") ? "Dato" : "Nombre actual",
                  CustomFieldType.TEXT,
                  "anotación"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"configuration", "project", "task"})
  void s20_suppressedInsertMustNotConfirmAnUndurableAggregate(String target) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var table =
        target.equals("configuration")
            ? "customization_preferences"
            : target.equals("project") ? "project_custom_field_values" : "task_custom_field_values";
    jdbc.execute(
        "CREATE FUNCTION suppress_customization_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_customization BEFORE INSERT ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION suppress_customization_write()");
    try {
      assertThatThrownBy(
              () -> {
                if (target.equals("configuration"))
                  new SaveCustomizationView(store, clock)
                      .save(
                          "owner-a",
                          CustomizationScope.PROJECT,
                          new CustomizationRevision(null, 0),
                          List.of());
                else {
                  var scope =
                      target.equals("project")
                          ? CustomizationScope.PROJECT
                          : CustomizationScope.TASK;
                  var entity = target.equals("project") ? project : task;
                  new SaveCustomFieldValues(store, clock)
                      .save(
                          "owner-a",
                          scope,
                          project,
                          entity,
                          new CustomFieldValuesRevision(
                              scope,
                              entity,
                              new CustomizationRevision(null, 0),
                              new CustomizationRevision(null, 0)),
                          List.of());
                }
              })
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER suppress_customization ON " + table);
      jdbc.execute("DROP FUNCTION suppress_customization_write()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "configuration,false",
    "configuration,true",
    "values,false",
    "values,true"
  })
  void s9_s16_existingRevisionsSerializeChangesButAllowBothNoOps(String target, boolean noop)
      throws Exception {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var time = Instant.parse("2026-09-07T20:00:00Z");
    var schema =
        new CreateCustomField(store, Clock.fixed(time, ZoneOffset.UTC))
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Número",
                CustomFieldType.NUMBER);
    var field = schema.customFields().getFirst().id();
    var valuesId = UUID.randomUUID();
    jdbc.update("UPDATE customization_preferences SET version=4");
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,2,?)",
        valuesId,
        project,
        "{\"" + field + "\":1}",
        time.atOffset(ZoneOffset.UTC));
    var before =
        jdbc.queryForList(
            "SELECT xmin::text,ctid::text,* FROM "
                + (target.equals("configuration")
                    ? "customization_preferences"
                    : "project_custom_field_values"));
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenReturn(time);
    java.util.function.IntFunction<Object> action =
        index -> {
          try {
            if (target.equals("configuration")) {
              if (noop || index == 0)
                return new SaveCustomizationView(store, clock)
                    .save(
                        "owner-a",
                        CustomizationScope.PROJECT,
                        new CustomizationRevision(schema.id(), 4),
                        noop ? List.of("createdAt") : List.of("updatedAt"));
              return new CreateCustomField(store, clock)
                  .create(
                      "owner-a",
                      CustomizationScope.PROJECT,
                      new CustomizationRevision(schema.id(), 4),
                      "Segundo",
                      CustomFieldType.TEXT);
            }
            return new SaveCustomFieldValues(store, clock)
                .save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    project,
                    project,
                    new CustomFieldValuesRevision(
                        CustomizationScope.PROJECT,
                        project,
                        new CustomizationRevision(schema.id(), 4),
                        new CustomizationRevision(valuesId, 2)),
                    List.of(
                        new CustomFieldInput(
                            field,
                            new java.math.BigDecimal(
                                noop ? "1.00" : Integer.toString(index + 2)))));
          } catch (RuntimeException error) {
            return error;
          }
        };
    try (var connection = manager.getDataSource().getConnection();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      connection.setAutoCommit(false);
      try (var lock =
          connection.prepareStatement(
              "SELECT pg_advisory_xact_lock(hashtextextended('customization:owner-a:PROJECT',0))")) {
        lock.executeQuery().close();
      }
      var one = executor.submit(() -> action.apply(0));
      var two = executor.submit(() -> action.apply(1));
      try {
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event='advisory'",
                                Integer.class))
                        .isEqualTo(2));
        org.mockito.Mockito.verifyNoInteractions(clock);
      } finally {
        connection.rollback();
      }
      var results =
          List.of(
              one.get(10, java.util.concurrent.TimeUnit.SECONDS),
              two.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(results.stream().filter(CustomizationConflictException.class::isInstance).count())
          .isEqualTo(noop ? 0 : 1);
      Object durable =
          target.equals("configuration")
              ? store.find("owner-a", CustomizationScope.PROJECT).orElseThrow()
              : store.find("owner-a", CustomizationScope.PROJECT, project, project);
      assertThat(results.stream().filter(result -> !(result instanceof RuntimeException)).toList())
          .hasSize(noop ? 2 : 1)
          .allSatisfy(result -> assertThat(result).isEqualTo(durable));
      if (noop) {
        assertThat(
                jdbc.queryForList(
                    "SELECT xmin::text,ctid::text,* FROM "
                        + (target.equals("configuration")
                            ? "customization_preferences"
                            : "project_custom_field_values")))
            .isEqualTo(before);
        org.mockito.Mockito.verifyNoInteractions(clock);
      } else org.mockito.Mockito.verify(clock).instant();
      assertThat(store.find("owner-a", CustomizationScope.PROJECT).orElseThrow().version())
          .isEqualTo(target.equals("configuration") && !noop ? 5 : 4);
      assertThat(
              store
                  .find("owner-a", CustomizationScope.PROJECT, project, project)
                  .revision()
                  .version())
          .isEqualTo(target.equals("values") && !noop ? 3 : 2);
    }
  }

  @Test
  void s15_schemaCommitWhileValuesWaitCauses412BeforeTypedValidation() throws Exception {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var time = Instant.parse("2026-09-07T20:00:00Z");
    var schema =
        new CreateCustomField(store, Clock.fixed(time, ZoneOffset.UTC))
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Número",
                CustomFieldType.NUMBER);
    var field = schema.customFields().getFirst().id();
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenAnswer(
            invocation -> {
              entered.countDown();
              if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                throw new AssertionError("schema release");
              return time;
            });
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var schemaWrite =
          executor.submit(
              () ->
                  new UpdateCustomField(store, clock)
                      .update(
                          "owner-a",
                          CustomizationScope.PROJECT,
                          field,
                          new CustomizationRevision(schema.id(), 0),
                          "Renombrado",
                          true));
      try {
        assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var valuesWrite =
            executor.submit(
                () -> {
                  try {
                    return (Object)
                        new SaveCustomFieldValues(store, clock)
                            .save(
                                "owner-a",
                                CustomizationScope.PROJECT,
                                project,
                                project,
                                new CustomFieldValuesRevision(
                                    CustomizationScope.PROJECT,
                                    project,
                                    new CustomizationRevision(schema.id(), 0),
                                    new CustomizationRevision(null, 0)),
                                List.of(new CustomFieldInput(field, "wrong type")));
                  } catch (RuntimeException error) {
                    return error;
                  }
                });
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event='advisory'",
                                Integer.class))
                        .isEqualTo(1));
        release.countDown();
        assertThat(schemaWrite.get(10, java.util.concurrent.TimeUnit.SECONDS).version())
            .isEqualTo(1);
        assertThat(valuesWrite.get(10, java.util.concurrent.TimeUnit.SECONDS))
            .isInstanceOf(CustomizationConflictException.class);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM project_custom_field_values", Integer.class))
            .isZero();
        org.mockito.Mockito.verify(clock).instant();
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void s10_readOnlyRepeatableReadKeepsOwnershipSchemaAndValuesInOneSnapshot() {
    var project = UUID.randomUUID();
    var field = UUID.randomUUID();
    var schemaId = UUID.randomUUID();
    var valuesId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var oldDefinition =
        "[{\"id\":\"" + field + "\",\"label\":\"Antes\",\"type\":\"TEXT\",\"active\":true}]";
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]',?::jsonb,0,'2026-09-07T20:00:00Z')",
        schemaId,
        oldDefinition);
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,0,'2026-09-07T20:00:00Z')",
        valuesId,
        project,
        "{\"" + field + "\":\"antes\"}");
    var writerSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var writer = new JdbcTemplate(writerSource);
    var committed = new java.util.concurrent.atomic.AtomicBoolean();
    var observing =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> T queryForObject(String sql, Class<T> type, Object... args) {
            var result = super.queryForObject(sql, type, args);
            if (committed.compareAndSet(false, true)) {
              var connection = DataSourceUtils.getConnection(getDataSource());
              try {
                assertThat(connection.isReadOnly()).isTrue();
                assertThat(connection.getTransactionIsolation())
                    .isEqualTo(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
              } catch (java.sql.SQLException error) {
                throw new AssertionError(error);
              }
              new org.springframework.transaction.support.TransactionTemplate(
                      new DataSourceTransactionManager(writerSource))
                  .execute(
                      status -> {
                        writer.update(
                            "UPDATE customization_preferences SET custom_fields=?::jsonb,version=1 WHERE id=?",
                            oldDefinition.replace("Antes", "Después"),
                            schemaId);
                        writer.update(
                            "UPDATE project_custom_field_values SET field_values=?::jsonb,version=1 WHERE id=?",
                            "{\"" + field + "\":\"después\"}",
                            valuesId);
                        return null;
                      });
            }
            return result;
          }
        };
    var snapshot =
        new PostgresCustomizationStore(observing, manager, new ObjectMapper())
            .find("owner-a", CustomizationScope.PROJECT, project, project);
    assertThat(committed).isTrue();
    assertThat(snapshot.schema()).isEqualTo(new CustomizationRevision(schemaId, 0));
    assertThat(snapshot.revision()).isEqualTo(new CustomizationRevision(valuesId, 0));
    assertThat(snapshot.values())
        .containsExactly(new CustomFieldValue(field, "Antes", CustomFieldType.TEXT, "antes"));
    var latest =
        new PostgresCustomizationStore(jdbc, manager, new ObjectMapper())
            .find("owner-a", CustomizationScope.PROJECT, project, project);
    assertThat(latest.schema()).isEqualTo(new CustomizationRevision(schemaId, 1));
    assertThat(latest.revision()).isEqualTo(new CustomizationRevision(valuesId, 1));
    assertThat(latest.values())
        .containsExactly(new CustomFieldValue(field, "Después", CustomFieldType.TEXT, "después"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(CustomizationScope.class)
  void s20_unavailableValuesTableBecomes503RatherThanAnEmptyCollection(CustomizationScope scope) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var table =
        scope == CustomizationScope.PROJECT
            ? "project_custom_field_values"
            : "task_custom_field_values";
    jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_values");
    try {
      var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
      assertThatThrownBy(
              () ->
                  store.find(
                      "owner-a",
                      scope,
                      project,
                      scope == CustomizationScope.PROJECT ? project : task))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_values RENAME TO " + table);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"configuration", "project", "task"})
  void s20_databaseWriteOrDeferredCommitFailureRollsBackAndBecomesStorageUnavailable(
      String target) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var table =
        target.equals("configuration")
            ? "customization_preferences"
            : target.equals("project") ? "project_custom_field_values" : "task_custom_field_values";
    jdbc.execute(
        "CREATE FUNCTION fail_customization_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled storage failure'; END $$");
    jdbc.execute(
        (target.equals("configuration") ? "CREATE CONSTRAINT TRIGGER" : "CREATE TRIGGER")
            + " fail_customization AFTER INSERT ON "
            + table
            + (target.equals("configuration") ? " DEFERRABLE INITIALLY DEFERRED" : "")
            + " FOR EACH ROW EXECUTE FUNCTION fail_customization_write()");
    var projects = jdbc.queryForList("SELECT * FROM projects");
    var tasks = jdbc.queryForList("SELECT * FROM tasks");
    try {
      assertThatThrownBy(
              () -> {
                if (target.equals("configuration"))
                  new CreateCustomField(store, clock)
                      .create(
                          "owner-a",
                          CustomizationScope.PROJECT,
                          new CustomizationRevision(null, 0),
                          "Dato",
                          CustomFieldType.TEXT);
                else {
                  var scope =
                      target.equals("project")
                          ? CustomizationScope.PROJECT
                          : CustomizationScope.TASK;
                  var entity = target.equals("project") ? project : task;
                  new SaveCustomFieldValues(store, clock)
                      .save(
                          "owner-a",
                          scope,
                          project,
                          entity,
                          new CustomFieldValuesRevision(
                              scope,
                              entity,
                              new CustomizationRevision(null, 0),
                              new CustomizationRevision(null, 0)),
                          List.of());
                }
              })
          .isInstanceOf(StorageUnavailableException.class);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).isZero();
      assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projects);
      assertThat(jdbc.queryForList("SELECT * FROM tasks")).isEqualTo(tasks);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER fail_customization ON " + table);
      jdbc.execute("DROP FUNCTION fail_customization_write()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "configuration,0",
    "configuration,10000",
    "values,0",
    "values,10000"
  })
  void s20_durableTimestampsOutsideThePublicCalendarFailWithoutWriting(String target, int year) {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var invalid = OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    if (target.equals("configuration"))
      jdbc.update(
          "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]','[]',0,?)",
          UUID.randomUUID(),
          invalid);
    else
      jdbc.update(
          "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,'{}',0,?)",
          UUID.randomUUID(),
          project,
          invalid);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var before = jdbc.queryForList("SELECT * FROM project_custom_field_values");
    assertThatThrownBy(() -> store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isInstanceOf(StorageUnavailableException.class);
    assertThat(jdbc.queryForList("SELECT * FROM project_custom_field_values")).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"1.00000000000000000001", "1000000001", "\"1\"", "true", "{}", "[]"})
  void s20_storedNumberIsValidatedExactlyBeforeAnyFloatingPointCoercion(String value) {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','idea',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var schema =
        new CreateCustomField(
                store, Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC))
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Número",
                CustomFieldType.NUMBER);
    var field = schema.customFields().getFirst().id();
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,0,'2026-09-07T20:00:00Z')",
        UUID.randomUUID(),
        project,
        "{\"" + field + "\":" + value + "}");
    assertThatThrownBy(() -> store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  @Test
  void s18_completedTaskWritePreservesBusinessFactsAndSeparateSubtaskValues() throws Exception {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var child = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,created_at,updated_at) VALUES (?,?,'T','Criterio',25,'pending','2026-09-07T19:00:00Z','2026-09-07T19:00:00Z')",
        task,
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,?,'S','','pending',now(),now())",
        child,
        project,
        task);
    var json =
        new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var store = new PostgresCustomizationStore(jdbc, manager, json);
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var sessions = new PostgresWorkSessionStore(jdbc, manager, json);
    var started =
        new StartWorkSession(sessions, clock, () -> Set.of("UTC"))
            .start("owner-a", project, task, UUID.randomUUID(), 25)
            .session();
    new ChangeWorkSession(
            sessions, Clock.fixed(Instant.parse("2026-09-07T20:01:00Z"), ZoneOffset.UTC))
        .close(
            "owner-a",
            started.id(),
            UUID.randomUUID(),
            new WorkSessionRevision(started.id(), 1),
            new WorkSessionCloseNotes("Avance previo", "Continuar"));
    new ChangeTaskStatus(
            new PostgresTaskStatusStore(
                jdbc,
                new org.springframework.transaction.support.TransactionTemplate(manager),
                json),
            Clock.fixed(Instant.parse("2026-09-07T20:02:00Z"), ZoneOffset.UTC))
        .execute("owner-a", project, task, new TaskRevision(task, 0), "completed");
    var projectSchema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Proyecto",
                CustomFieldType.TEXT);
    new SaveCustomFieldValues(store, clock)
        .save(
            "owner-a",
            CustomizationScope.PROJECT,
            project,
            project,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                project,
                new CustomizationRevision(projectSchema.id(), 0),
                new CustomizationRevision(null, 0)),
            List.of(
                new CustomFieldInput(
                    projectSchema.customFields().getFirst().id(), "valor proyecto")));
    var schema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.TASK,
                new CustomizationRevision(null, 0),
                "Dato",
                CustomFieldType.TEXT);
    var field = schema.customFields().getFirst();
    jdbc.update(
        "INSERT INTO task_custom_field_values VALUES (?,'owner-a',?,?::jsonb,3,'2026-09-07T20:00:00Z')",
        UUID.randomUUID(),
        child,
        json.writeValueAsString(Map.of(field.id().toString(), "subtarea")));
    var tasksBefore = jdbc.queryForList("SELECT * FROM tasks ORDER BY id");
    var projectsBefore = jdbc.queryForList("SELECT * FROM projects");
    var childBefore = store.find("owner-a", CustomizationScope.TASK, project, child);
    var projectValuesBefore = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    var projectRowsBefore = jdbc.queryForList("SELECT * FROM project_custom_field_values");
    var historyBefore = new LinkedHashMap<String, List<Map<String, Object>>>();
    for (var table :
        List.of(
            "work_sessions",
            "work_session_intervals",
            "work_session_changes",
            "task_status_history",
            "outbox_events")) {
      var rows = jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1,2");
      assertThat(rows).as(table + " populated before customization").isNotEmpty();
      historyBefore.put(table, rows);
    }
    var saved =
        new SaveCustomFieldValues(store, clock)
            .save(
                "owner-a",
                CustomizationScope.TASK,
                project,
                task,
                new CustomFieldValuesRevision(
                    CustomizationScope.TASK,
                    task,
                    new CustomizationRevision(schema.id(), 0),
                    new CustomizationRevision(null, 0)),
                List.of(new CustomFieldInput(field.id(), "padre")));
    assertThat(store.find("owner-a", CustomizationScope.TASK, project, task)).isEqualTo(saved);
    assertThat(saved.values().getFirst().value()).isEqualTo("padre");
    assertThat(store.find("owner-a", CustomizationScope.TASK, project, child))
        .isEqualTo(childBefore);
    assertThat(jdbc.queryForList("SELECT * FROM tasks ORDER BY id")).isEqualTo(tasksBefore);
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projectsBefore);
    assertThat(store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isEqualTo(projectValuesBefore);
    assertThat(jdbc.queryForList("SELECT * FROM project_custom_field_values"))
        .isEqualTo(projectRowsBefore);
    historyBefore.forEach(
        (table, rows) ->
            assertThat(jdbc.queryForList("SELECT * FROM " + table + " ORDER BY 1,2"))
                .as(table)
                .isEqualTo(rows));
  }

  @Test
  void s14_s42_emptyNoOpWithOnlyInactiveValuesAtMaximumRevisionDoesNotWriteOrReadClock() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var schema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Dato",
                CustomFieldType.TEXT);
    var field = schema.customFields().getFirst();
    new SaveCustomFieldValues(store, clock)
        .save(
            "owner-a",
            CustomizationScope.PROJECT,
            project,
            project,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                project,
                new CustomizationRevision(schema.id(), 0),
                new CustomizationRevision(null, 0)),
            List.of(new CustomFieldInput(field.id(), "preservado")));
    new UpdateCustomField(store, clock)
        .update(
            "owner-a",
            CustomizationScope.PROJECT,
            field.id(),
            new CustomizationRevision(schema.id(), 0),
            "Dato",
            false);
    jdbc.update("UPDATE project_custom_field_values SET version=?", Long.MAX_VALUE);
    var previous = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    var tuple =
        jdbc.queryForObject(
            "SELECT xmin::text || ':' || ctid::text FROM project_custom_field_values",
            String.class);
    var forbiddenClock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(forbiddenClock.instant())
        .thenThrow(new AssertionError("No clock for empty no-op"));
    var result =
        new SaveCustomFieldValues(store, forbiddenClock)
            .save(
                "owner-a",
                CustomizationScope.PROJECT,
                project,
                project,
                new CustomFieldValuesRevision(
                    CustomizationScope.PROJECT, project, previous.schema(), previous.revision()),
                List.of());
    assertThat(result).isEqualTo(previous);
    assertThat(result.values()).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT xmin::text || ':' || ctid::text FROM project_custom_field_values",
                String.class))
        .isEqualTo(tuple);
    assertThat(
            jdbc.queryForObject(
                "SELECT field_values ->> ? FROM project_custom_field_values",
                String.class,
                field.id().toString()))
        .isEqualTo("preservado");
    org.mockito.Mockito.verifyNoInteractions(forbiddenClock);
  }

  @Test
  void s16_s42_twoEmptyFirstValueWritesWaitOnScopeLockAndOnlyCreateOneCollection()
      throws Exception {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-09-07T20:00:00Z"));
    var save = new SaveCustomFieldValues(store, clock);
    try (var connection = manager.getDataSource().getConnection();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      connection.setAutoCommit(false);
      try (var lock =
          connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
        lock.setString(1, "customization:owner-a:PROJECT");
        lock.executeQuery().close();
      }
      java.util.concurrent.Callable<Object> action =
          () -> {
            try {
              return save.save(
                  "owner-a",
                  CustomizationScope.PROJECT,
                  project,
                  project,
                  new CustomFieldValuesRevision(
                      CustomizationScope.PROJECT,
                      project,
                      new CustomizationRevision(null, 0),
                      new CustomizationRevision(null, 0)),
                  List.of());
            } catch (RuntimeException error) {
              return error;
            }
          };
      var one = executor.submit(action);
      var two = executor.submit(action);
      try {
        org.awaitility.Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event='advisory'",
                                Integer.class))
                        .isEqualTo(2));
        org.mockito.Mockito.verifyNoInteractions(clock);
      } finally {
        connection.rollback();
      }
      var outcomes =
          List.of(
              one.get(10, java.util.concurrent.TimeUnit.SECONDS),
              two.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(outcomes.stream().filter(CustomFieldValues.class::isInstance).count())
          .isEqualTo(1);
      assertThat(outcomes.stream().filter(CustomizationConflictException.class::isInstance).count())
          .isEqualTo(1);
      var saved =
          outcomes.stream()
              .filter(CustomFieldValues.class::isInstance)
              .map(CustomFieldValues.class::cast)
              .findFirst()
              .orElseThrow();
      assertThat(saved.revision().id()).isNotNull();
      assertThat(saved.revision().version()).isZero();
      assertThat(saved.schema()).isEqualTo(new CustomizationRevision(null, 0));
      assertThat(saved.values()).isEmpty();
      assertThat(
              jdbc.queryForObject("SELECT count(*) FROM customization_preferences", Integer.class))
          .isZero();
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM project_custom_field_values", Integer.class))
          .isEqualTo(1);
      org.mockito.Mockito.verify(clock).instant();
    }
  }

  @Test
  void s17_foreignProjectWriteReturnsNotFoundBeforeComparingPrivateRevisions() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'other-owner','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = org.mockito.Mockito.mock(Clock.class);
    var save = new SaveCustomFieldValues(store, clock);
    assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    project,
                    project,
                    new CustomFieldValuesRevision(
                        CustomizationScope.PROJECT,
                        project,
                        new CustomizationRevision(UUID.randomUUID(), 7),
                        new CustomizationRevision(null, 0)),
                    List.of()))
        .isInstanceOf(ResourceNotFoundException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM project_custom_field_values", Integer.class))
        .isZero();
  }

  @Test
  void s11_subsequentProjectValueWriteUpdatesOneExistingCollection() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var schema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Dato",
                CustomFieldType.TEXT);
    var field = schema.customFields().getFirst();
    var save = new SaveCustomFieldValues(store, clock);
    var first =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            project,
            project,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                project,
                new CustomizationRevision(schema.id(), 0),
                new CustomizationRevision(null, 0)),
            List.of(new CustomFieldInput(field.id(), "primero")));
    var changed =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            project,
            project,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT, project, first.schema(), first.revision()),
            List.of(new CustomFieldInput(field.id(), "segundo")));
    assertThat(changed.revision()).isEqualTo(new CustomizationRevision(first.revision().id(), 1));
    assertThat(changed.values().getFirst().value()).isEqualTo("segundo");
    assertThat(store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isEqualTo(changed);
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM project_custom_field_values", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s11_firstProjectValuesWritePersistsExplicitNullAndItsExactConfirmation() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00.123456Z"), ZoneOffset.UTC);
    var schema =
        new CreateCustomField(store, clock)
            .create(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(null, 0),
                "Dato",
                CustomFieldType.TEXT);
    var field = schema.customFields().getFirst();
    var before = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    var saved =
        new SaveCustomFieldValues(store, clock)
            .save(
                "owner-a",
                CustomizationScope.PROJECT,
                project,
                project,
                new CustomFieldValuesRevision(
                    CustomizationScope.PROJECT, project, before.schema(), before.revision()),
                List.of(new CustomFieldInput(field.id(), null)));
    assertThat(saved.revision().id()).isNotNull();
    assertThat(saved.revision().version()).isZero();
    assertThat(saved.updatedAt()).isEqualTo(clock.instant());
    assertThat(store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isEqualTo(saved);
    assertThat(
            jdbc.queryForObject(
                "SELECT field_values -> ? = 'null'::jsonb FROM project_custom_field_values",
                Boolean.class,
                field.id().toString()))
        .isTrue();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }

  @Test
  void s10_readsTaskValuesFromItsOwnCollectionAndTaskSchema() throws Exception {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var configurationId = UUID.randomUUID();
    var valuesId = UUID.randomUUID();
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Texto", CustomFieldType.TEXT, true);
    var json = new ObjectMapper();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','TASK','[]'::jsonb,?::jsonb,2,'2026-09-07T20:00:00Z')",
        configurationId,
        json.writeValueAsString(List.of(field)));
    jdbc.update(
        "INSERT INTO task_custom_field_values VALUES (?,'owner-a',?,?::jsonb,3,'2026-09-07T20:00:00Z')",
        valuesId,
        task,
        json.writeValueAsString(Map.of(field.id().toString(), "Propio")));
    var result =
        new PostgresCustomizationStore(jdbc, manager, json)
            .find("owner-a", CustomizationScope.TASK, project, task);
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(valuesId, 3));
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(configurationId, 2));
    assertThat(result.values())
        .containsExactly(new CustomFieldValue(field.id(), "Texto", CustomFieldType.TEXT, "Propio"));
  }

  @Test
  void s10_savedValuesKeepZeroFalseAndStoredRevisionWhileInactiveValuesStayHidden()
      throws Exception {
    var project = UUID.randomUUID();
    var configurationId = UUID.randomUUID();
    var valuesId = UUID.randomUUID();
    var number =
        new CustomFieldDefinition(UUID.randomUUID(), "Numero", CustomFieldType.NUMBER, true);
    var bool = new CustomFieldDefinition(UUID.randomUUID(), "Hecho", CustomFieldType.BOOLEAN, true);
    var hidden =
        new CustomFieldDefinition(UUID.randomUUID(), "Oculto", CustomFieldType.TEXT, false);
    var json = new ObjectMapper();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?,'owner-a','PROJECT','[]'::jsonb,?::jsonb,7,'2026-09-07T20:00:00Z')",
        configurationId,
        json.writeValueAsString(List.of(number, bool, hidden)));
    jdbc.update(
        "INSERT INTO project_custom_field_values VALUES (?,'owner-a',?,?::jsonb,3,'0001-01-01T00:00:00.123456Z')",
        valuesId,
        project,
        json.writeValueAsString(
            Map.of(
                number.id().toString(),
                0,
                bool.id().toString(),
                false,
                hidden.id().toString(),
                "privado")));
    var store = new PostgresCustomizationStore(jdbc, manager, json);
    var result = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(configurationId, 7));
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(valuesId, 3));
    assertThat(result.updatedAt()).isEqualTo("0001-01-01T00:00:00.123456Z");
    assertThat(result.values())
        .containsExactly(
            new CustomFieldValue(number.id(), number.label(), number.type(), 0),
            new CustomFieldValue(bool.id(), bool.label(), bool.type(), false));
    assertThat(
            jdbc.queryForObject(
                "SELECT field_values ->> ? FROM project_custom_field_values",
                String.class,
                hidden.id().toString()))
        .isEqualTo("privado");
  }

  @Test
  void s17_taskOfAnotherOwnedProjectIsNotFoundInRequestedContext() {
    var project = UUID.randomUUID();
    var otherProject = UUID.randomUUID();
    var task = UUID.randomUUID();
    for (var id : List.of(project, otherProject))
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
          id);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        otherProject);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    assertThatThrownBy(() -> store.find("owner-a", CustomizationScope.TASK, project, task))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @BeforeEach
  void clearOwnDatabase() {
    for (var table :
        List.of(
            "project_custom_field_values",
            "task_custom_field_values",
            "customization_preferences",
            "work_session_intervals",
            "work_session_changes",
            "work_sessions",
            "task_status_history",
            "outbox_events",
            "tasks",
            "projects")) jdbc.update("DELETE FROM " + table);
  }

  @Test
  void s17_readingAnotherOwnersProjectIsNotFoundWithoutExposingOwnSchema() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'other-owner','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    assertThatThrownBy(() -> store.find("owner-a", CustomizationScope.PROJECT, project, project))
        .isInstanceOf(ResourceNotFoundException.class);
  }

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
  void s10_readsOnlyActiveProjectFieldsAsNullWithoutCreatingValues() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner-a','P','','active',now(),now())",
        project);
    var store = new PostgresCustomizationStore(jdbc, manager, new ObjectMapper());
    var create = new CreateCustomField(store, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    var first =
        create.create(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(null, 0),
            "Texto",
            CustomFieldType.TEXT);
    var second =
        create.create(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(first.id(), 0),
            "Numero",
            CustomFieldType.NUMBER);
    var configuration =
        new UpdateCustomField(store, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
            .update(
                "owner-a",
                CustomizationScope.PROJECT,
                second.customFields().getLast().id(),
                new CustomizationRevision(first.id(), 1),
                "Numero",
                false);
    var result = store.find("owner-a", CustomizationScope.PROJECT, project, project);
    assertThat(result.entityId()).isEqualTo(project);
    assertThat(result.scope()).isEqualTo(CustomizationScope.PROJECT);
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(configuration.id(), 2));
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(null, 0));
    assertThat(result.updatedAt()).isNull();
    assertThat(result.values())
        .containsExactly(
            new CustomFieldValue(
                first.customFields().getFirst().id(), "Texto", CustomFieldType.TEXT, null));
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM project_custom_field_values", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_custom_field_values", Integer.class))
        .isZero();
  }
}
