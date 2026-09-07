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
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,completed_at) VALUES (?,?,'T','','completed',now(),now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,?,'S','','pending',now(),now())",
        child,
        project,
        task);
    var json = new ObjectMapper();
    var store = new PostgresCustomizationStore(jdbc, manager, json);
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
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
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM project_custom_field_values", Integer.class))
        .isZero();
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
