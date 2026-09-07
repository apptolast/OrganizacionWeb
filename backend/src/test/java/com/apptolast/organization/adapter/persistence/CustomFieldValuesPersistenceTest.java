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
