package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.CustomizationScope;
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
class CustomizationPersistenceTest {
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
