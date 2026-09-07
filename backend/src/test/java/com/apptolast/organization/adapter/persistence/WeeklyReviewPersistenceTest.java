package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.apptolast.organization.application.*;
import java.time.*;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WeeklyReviewPersistenceTest {
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
  void s1_emptyWeekReadsPostgresInReadOnlyRepeatableRead() {
    var clock = mock(Clock.class);
    when(clock.instant())
        .thenAnswer(
            call -> {
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("repeatable read");
              assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                  .isEqualTo("on");
              return Instant.parse("2026-09-09T12:00:00.123456789Z");
            });
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC"));
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(manager)), jdbc, manager);
    var result = new ReadWeeklyReview(queries, clock, catalog).get("owner", null, null);
    assertThat(result.weekStart()).isEqualTo(LocalDate.parse("2026-09-07"));
    assertThat(result.serverNow()).isEqualTo(Instant.parse("2026-09-09T12:00:00.123456Z"));
    assertThat(result.days())
        .hasSize(7)
        .allSatisfy(
            day -> {
              assertThat(day.plannedMicroseconds()).isZero();
              assertThat(day.workedMicroseconds()).isZero();
              assertThat(day.capacityMicroseconds()).isNull();
            });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    verify(clock).instant();
  }
}
