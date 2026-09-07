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
}
