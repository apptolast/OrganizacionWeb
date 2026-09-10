package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * La retirada de las features 27, 29 y 30 también es esquema. Esta prueba parte de una base que ya
 * vivió con las tres dentro y comprueba las dos mitades de V32: las siete tablas que se lleva y las
 * de las features supervivientes, que no puede rozar.
 */
@Testcontainers
class RetirementMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final String BEFORE_THE_RETIREMENT = "31";
  private static final List<String> RETIRED =
      List.of(
          "task_external_links",
          "issue_import_receipts",
          "connector_connections",
          "gitlab_connections",
          "automation_runs",
          "automation_rules",
          "automation_cursors");

  /** Lo que sigue vivo: webhooks (25), calendario externo (28) y la importación de datos. */
  private static final List<String> SURVIVORS =
      List.of(
          "webhook_endpoints",
          "webhook_deliveries",
          "external_calendar_subscriptions",
          "import_receipts",
          "tasks",
          "projects");

  @Test
  void v32TakesTheSevenTablesOfTheThreeRetiredFeaturesAndNothingElse() {
    var jdbc = migratedUpTo(BEFORE_THE_RETIREMENT);
    assertThat(existing(jdbc, RETIRED)).containsExactlyElementsOf(RETIRED);

    migrateToHead();

    assertThat(existing(jdbc, RETIRED)).isEmpty();
    assertThat(existing(jdbc, SURVIVORS)).containsExactlyElementsOf(SURVIVORS);
  }

  private static JdbcTemplate migratedUpTo(String version) {
    Flyway.configure().dataSource(dataSource()).target(version).load().migrate();
    return new JdbcTemplate(dataSource());
  }

  private static void migrateToHead() {
    Flyway.configure().dataSource(dataSource()).load().migrate();
  }

  private static DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  /** De los nombres dados, los que existen de verdad, en el mismo orden en que se preguntaron. */
  private static List<String> existing(JdbcTemplate jdbc, List<String> tables) {
    return tables.stream().filter(table -> exists(jdbc, table)).toList();
  }

  private static boolean exists(JdbcTemplate jdbc, String table) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables"
                + " WHERE table_schema='public' AND table_name=?)",
            Boolean.class,
            table));
  }
}
