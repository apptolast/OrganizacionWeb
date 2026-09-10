package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * La retirada del conector de GitLab también es esquema. Esta prueba parte de una base que ya vivió
 * con la feature 29 dentro —conexión guardada y enlaces de los dos gestores— y comprueba lo que V32
 * se lleva y, sobre todo, lo que no toca: la forma del recibo que hoy es de la feature 27.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitlabRetirementMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final String OWNER = "owner-a";
  private static final Instant NOW = Instant.parse("2026-09-10T10:00:00.123456Z");
  private static final String BEFORE_THE_RETIREMENT = "31";

  @Test
  @Order(1)
  void theRetirementDropsTheGitlabConnectionAndItsLinksButLeavesGithubStanding() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var jdbc = new JdbcTemplate(source);
    Flyway.configure().dataSource(source).target(BEFORE_THE_RETIREMENT).load().migrate();
    seedAGitlabConnection(jdbc);
    var githubTask = seedTaskWithLink(jdbc, "github", "101");
    seedTaskWithLink(jdbc, "gitlab", "42");

    Flyway.configure().dataSource(source).load().migrate();

    assertThat(exists(jdbc, "gitlab_connections")).isFalse();
    assertThat(linkSources(jdbc)).containsExactly("github");
    assertThat(jdbc.queryForObject("SELECT task_id FROM task_external_links", UUID.class))
        .isEqualTo(githubTask);
    assertThatThrownBy(() -> insertLink(jdbc, seedTask(jdbc), "gitlab", "7"))
        .hasMessageContaining("task_external_links_source_check");
  }

  @Test
  @Order(2)
  void theShapeOfTheReceiptSurvivesBecauseItIsNowTheShapeOfTheGithubConnector() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var jdbc = new JdbcTemplate(source);
    Flyway.configure().dataSource(source).load().migrate();

    assertThat(hasColumn(jdbc, "issue_import_receipts", "project_path")).isTrue();
    assertThat(hasColumn(jdbc, "issue_import_receipts", "source")).isTrue();
    assertThat(hasColumn(jdbc, "issue_import_receipts", "repository")).isFalse();
  }

  private static void seedAGitlabConnection(JdbcTemplate jdbc) {
    jdbc.update(
        "INSERT INTO gitlab_connections(owner_id,project_path,project_id,token_hint,status,"
            + "token_ciphertext,last_activity_at,version) VALUES (?,?,?,?,?,?,?,?)",
        OWNER,
        "grupo/proyecto",
        7L,
        "cdef",
        "connected",
        new byte[40],
        Timestamp.from(NOW),
        1L);
  }

  private static UUID seedTaskWithLink(JdbcTemplate jdbc, String source, String externalId) {
    var task = seedTask(jdbc);
    insertLink(jdbc, task, source, externalId);
    return task;
  }

  private static UUID seedTask(JdbcTemplate jdbc) {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at)"
            + " VALUES (?,?,'Proyecto','','active',now(),now())",
        project,
        OWNER);
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    return task;
  }

  private static void insertLink(JdbcTemplate jdbc, UUID task, String source, String externalId) {
    jdbc.update(
        "INSERT INTO task_external_links(owner_id,source,external_id,task_id,url,linked_at)"
            + " VALUES (?,?,?,?,?,?)",
        OWNER,
        source,
        externalId,
        task,
        "https://x",
        Timestamp.from(NOW));
  }

  private static java.util.List<String> linkSources(JdbcTemplate jdbc) {
    return jdbc.queryForList("SELECT source FROM task_external_links", String.class);
  }

  private static boolean exists(JdbcTemplate jdbc, String table) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.tables"
                + " WHERE table_schema='public' AND table_name=?)",
            Boolean.class,
            table));
  }

  private static boolean hasColumn(JdbcTemplate jdbc, String table, String column) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.columns"
                + " WHERE table_schema='public' AND table_name=? AND column_name=?)",
            Boolean.class,
            table,
            column));
  }
}
