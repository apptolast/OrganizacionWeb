package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.HistoryFilters;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class HistoryReadTransactionTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
  }

  @Test
  void s22_pageRunsInsideReadOnlyRepeatableRead() {
    var observed = new java.util.concurrent.atomic.AtomicBoolean();
    var checked =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> java.util.List<T> query(String sql, RowMapper<T> mapper, Object... args) {
            assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                .isEqualTo("on");
            assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                .isEqualTo("repeatable read");
            observed.set(true);
            return super.query(sql, mapper, args);
          }
        };
    assertThat(
            new PostgresHistoryQueries(
                    checked,
                    new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()),
                    new ObjectMapper())
                .list("owner", new HistoryFilters(null, null, null, null, null), null))
        .isEmpty();
    assertThat(observed).isTrue();
  }

  @Test
  void s23_failureAtTransactionCloseCannotReturnAnEmptyPage() {
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()) {
          @Override
          protected void doCommit(
              org.springframework.transaction.support.DefaultTransactionStatus status) {
            super.doCommit(status);
            throw new org.springframework.transaction.TransactionSystemException(
                "private storage detail");
          }
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresHistoryQueries(jdbc, manager, new ObjectMapper())
                    .list("owner", new HistoryFilters(null, null, null, null, null), null))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
        .hasCauseInstanceOf(org.springframework.transaction.TransactionSystemException.class);
  }

  @Test
  void s23_sqlFailureCannotBePresentedAsNoFacts() {
    jdbc.execute("ALTER TABLE task_status_history RENAME TO unavailable_history");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresHistoryQueries(
                          jdbc,
                          new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                              jdbc.getDataSource()),
                          new ObjectMapper())
                      .list("owner", new HistoryFilters(null, null, null, null, null), null))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
          .hasCauseInstanceOf(org.springframework.dao.DataAccessException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_history RENAME TO task_status_history");
    }
  }

  @org.junit.jupiter.api.BeforeEach
  void clearFacts() {
    jdbc.execute(
        "TRUNCATE project_custom_field_values,task_custom_field_values, work_session_intervals,work_session_changes,work_sessions,block_changes,block_projections,planned_blocks,task_status_history,tasks,outbox_events,projects");
  }

  @Test
  void s22_committedWriterCannotChangeFactsOrLabelsInsideThePageSnapshot() {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    var original = java.util.UUID.randomUUID();
    var later = java.util.UUID.randomUUID();
    var at = java.time.Instant.parse("2026-09-07T10:00:00Z");
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Anterior','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,completed_at,created_at,updated_at) VALUES (?,?,'Tarea','','completed',1,?,?,?)",
        task,
        project,
        java.sql.Timestamp.from(at),
        java.sql.Timestamp.from(at.minusSeconds(1)),
        java.sql.Timestamp.from(at));
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,1,'pending','completed',?)",
        original,
        project,
        task,
        java.sql.Timestamp.from(at));
    var writerFinished = new java.util.concurrent.atomic.AtomicBoolean();
    var checked =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> java.util.List<T> query(String sql, RowMapper<T> mapper, Object... args) {
            if (sql.stripLeading().startsWith("WITH")) {
              try {
                java.util.concurrent.CompletableFuture.runAsync(
                        () -> {
                          new org.springframework.transaction.support.TransactionTemplate(
                                  new org.springframework.jdbc.datasource
                                      .DataSourceTransactionManager(jdbc.getDataSource()))
                              .executeWithoutResult(
                                  status -> {
                                    jdbc.update(
                                        "UPDATE projects SET name='Actual' WHERE id=?", project);
                                    jdbc.update(
                                        "UPDATE tasks SET status='pending',version=2,completed_at=null,updated_at=? WHERE id=?",
                                        java.sql.Timestamp.from(at.plusSeconds(1)),
                                        task);
                                    jdbc.update(
                                        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,2,'completed','pending',?)",
                                        later,
                                        project,
                                        task,
                                        java.sql.Timestamp.from(at.plusSeconds(1)));
                                  });
                          writerFinished.set(true);
                        })
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
              } catch (Exception error) {
                throw new AssertionError(error);
              }
            }
            return super.query(sql, mapper, args);
          }
        };
    var json =
        new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
    var filters = new HistoryFilters(null, project, task, null, null);
    var page = new PostgresHistoryQueries(checked, manager, json).list("owner", filters, null);
    assertThat(writerFinished).isTrue();
    assertThat(page)
        .extracting(com.apptolast.organization.application.HistoryEntry::id)
        .containsExactly(original);
    assertThat(page)
        .extracting(com.apptolast.organization.application.HistoryEntry::projectName)
        .containsExactly("Anterior");
    var refreshed = new PostgresHistoryQueries(jdbc, manager, json).list("owner", filters, null);
    assertThat(refreshed)
        .extracting(com.apptolast.organization.application.HistoryEntry::id)
        .containsExactly(later, original);
    assertThat(refreshed)
        .extracting(com.apptolast.organization.application.HistoryEntry::projectName)
        .containsExactly("Actual", "Actual");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }
}
