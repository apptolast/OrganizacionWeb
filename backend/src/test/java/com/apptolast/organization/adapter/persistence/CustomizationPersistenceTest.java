package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.SaveCustomizationView;
import com.apptolast.organization.domain.CustomizationRevision;
import com.apptolast.organization.domain.CustomizationScope;
import java.time.*;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomizationPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  @Test
  void s9_twoFirstWritesWaitOnTheOwnerScopeLockBeforeClockAndOnlyOneWins() throws Exception {
    var store =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.parse("2026-09-07T20:00:00Z"));
    var save = new SaveCustomizationView(store, clock);
    try (var connection = manager.getDataSource().getConnection();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      connection.setAutoCommit(false);
      try (var lock =
          connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")) {
        lock.setString(1, "customization:owner-a:PROJECT");
        lock.executeQuery().close();
      }
      java.util.concurrent.Callable<Object> change =
          () -> {
            try {
              return save.save(
                  "owner-a",
                  CustomizationScope.PROJECT,
                  new CustomizationRevision(null, 0),
                  List.of());
            } catch (RuntimeException error) {
              return error;
            }
          };
      var one = executor.submit(change);
      var two = executor.submit(change);
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
      assertThat(
              outcomes.stream()
                  .filter(com.apptolast.organization.domain.Customization.class::isInstance)
                  .count())
          .isEqualTo(1);
      assertThat(
              outcomes.stream()
                  .filter(
                      com.apptolast.organization.application.CustomizationConflictException.class
                          ::isInstance)
                  .count())
          .isEqualTo(1);
      org.mockito.Mockito.verify(clock).instant();
      assertThat(
              jdbc.queryForObject("SELECT count(*) FROM customization_preferences", Integer.class))
          .isEqualTo(1);
    }
  }

  @Test
  void s21_currentNoOpDoesNotWriteTheDatabaseRow() {
    var store =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    var save =
        new SaveCustomizationView(
            store, Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC));
    var initial =
        save.save(
            "owner-a", CustomizationScope.PROJECT, new CustomizationRevision(null, 0), List.of());
    var tuple =
        jdbc.queryForObject(
            "SELECT xmin::text || ':' || ctid::text FROM customization_preferences", String.class);
    var unchanged =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(initial.id(), 0),
            List.of());
    assertThat(unchanged).isEqualTo(initial);
    assertThat(
            jdbc.queryForObject(
                "SELECT xmin::text || ':' || ctid::text FROM customization_preferences",
                String.class))
        .isEqualTo(tuple);
  }

  @Test
  void s3_s7_definitionCreationAndDeactivationUpdateTheSameDurableConfiguration() {
    var store =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    var clock = Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC);
    var save = new SaveCustomizationView(store, clock);
    var initial =
        save.save(
            "owner-a", CustomizationScope.PROJECT, new CustomizationRevision(null, 0), List.of());
    var create = new com.apptolast.organization.application.CreateCustomField(store, clock);
    var created =
        create.create(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(initial.id(), 0),
            "Dato",
            com.apptolast.organization.domain.CustomFieldType.TEXT);
    var field = created.customFields().getFirst();
    var update = new com.apptolast.organization.application.UpdateCustomField(store, clock);
    var changed =
        update.update(
            "owner-a",
            CustomizationScope.PROJECT,
            field.id(),
            new CustomizationRevision(initial.id(), 1),
            "Nombre nuevo",
            false);
    assertThat(store.find("owner-a", CustomizationScope.PROJECT)).contains(changed);
    assertThat(changed.id()).isEqualTo(initial.id());
    assertThat(changed.version()).isEqualTo(2);
    assertThat(changed.customFields().getFirst().id()).isEqualTo(field.id());
    assertThat(changed.customFields().getFirst().active()).isFalse();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM customization_preferences", Integer.class))
        .isEqualTo(1);
  }

  @BeforeEach
  void clearOwnDatabasePreferences() {
    jdbc.update("DELETE FROM customization_preferences");
  }

  @Test
  void s2_savedViewIsDurableAcrossStoreInstancesWithoutBusinessEvents() {
    int events = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
    var store =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    var save =
        new SaveCustomizationView(
            store, Clock.fixed(Instant.parse("2026-09-07T20:00:00.123456Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.TASK,
            new CustomizationRevision(null, 0),
            List.of("estimatedMinutes"));
    var restored =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    assertThat(restored.find("owner-a", CustomizationScope.TASK)).contains(result);
    assertThat(restored.find("owner-a", CustomizationScope.PROJECT)).isEmpty();
    assertThat(restored.find("owner-b", CustomizationScope.TASK)).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM customization_preferences", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(events);
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
  void s1_readsBothAbsentScopesWithoutInsertingConfigurationValuesOrEvents() {
    int events = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
    var store =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    assertThat(store.find("owner-a", CustomizationScope.PROJECT)).isEmpty();
    assertThat(store.find("owner-a", CustomizationScope.TASK)).isEmpty();
    for (var table :
        java.util.List.of(
            "customization_preferences", "project_custom_field_values", "task_custom_field_values"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(events);
  }
}
