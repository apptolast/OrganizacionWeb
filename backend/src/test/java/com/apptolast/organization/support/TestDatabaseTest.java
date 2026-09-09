package com.apptolast.organization.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * La regresión que tumbó la CI: la V25 añadió una tabla que apunta a {@code tasks} y ninguna lista
 * de fixtures escrita a mano la nombraba, así que PostgreSQL rechazó el {@code TRUNCATE} entero.
 * Estas pruebas fijan que la limpieza no dependa de ninguna lista.
 */
@Testcontainers
class TestDatabaseTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final String LATE_ARRIVAL = "late_arrival";

  static JdbcTemplate jdbc;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
  }

  @AfterEach
  void dropTheLateArrival() {
    jdbc.execute("DROP TABLE IF EXISTS " + LATE_ARRIVAL);
  }

  @Test
  void emptiesATableThatArrivedAfterTheFixturesWereWritten() {
    var task = seedProjectAndTask();
    jdbc.execute(
        "CREATE TABLE " + LATE_ARRIVAL + "(id UUID PRIMARY KEY, task_id UUID REFERENCES tasks(id))");
    jdbc.update("INSERT INTO " + LATE_ARRIVAL + " VALUES (?,?)", UUID.randomUUID(), task);

    TestDatabase.empty(jdbc);

    assertThat(rowsIn(LATE_ARRIVAL)).isZero();
    assertThat(rowsIn("tasks")).isZero();
    assertThat(rowsIn("projects")).isZero();
  }

  @Test
  void keepsTheFlywayHistorySoTheNextClassDoesNotMigrateAgain() {
    var migrations = rowsIn("flyway_schema_history");

    TestDatabase.empty(jdbc);

    assertThat(migrations).isPositive();
    assertThat(rowsIn("flyway_schema_history")).isEqualTo(migrations);
  }

  private UUID seedProjectAndTask() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'owner','P','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    return task;
  }

  private int rowsIn(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }
}
