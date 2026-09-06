package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BlockChangeQueriesPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;
  static ObjectMapper json;
  UUID project;
  UUID task;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
    json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @BeforeEach
  void context() {
    jdbc.execute(
        "TRUNCATE"
            + " block_changes,block_projections,planned_blocks,task_status_history,tasks,outbox_events,projects");
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'owner','P','','completed',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO"
            + " tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,completed_at)"
            + " VALUES (?,?,'T','','completed',now(),now(),now())",
        task,
        project);
  }

  @Test
  void s16_s18_readsDurableCancellationWithoutOutboxOrAvailability() throws Exception {
    var receipt = cancellation(UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00.123456Z"));
    var queries = new PostgresBlockChangeQueries(jdbc, manager, json);
    assertThat(queries.list("owner", project, task, null)).containsExactly(receipt);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s18_readsInsideReadOnlyRepeatableReadTransaction() {
    var observed = new java.util.concurrent.atomic.AtomicBoolean();
    var checkedJdbc =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> java.util.List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
            assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                .isEqualTo("on");
            assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                .isEqualTo("repeatable read");
            observed.set(true);
            return super.query(sql, mapper, args);
          }
        };
    assertThat(
            new PostgresBlockChangeQueries(checkedJdbc, manager, json)
                .list("owner", project, task, null))
        .isEmpty();
    assertThat(observed).isTrue();
  }

  @Test
  void s18_foreignContextIsNotAnEmptyHistory() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresBlockChangeQueries(jdbc, manager, json)
                    .list("other-owner", project, task, null))
        .isInstanceOf(com.apptolast.organization.application.ResourceNotFoundException.class);
  }

  BlockChangeReceipt cancellation(UUID changeId, Instant occurredAt) throws Exception {
    var blockId = UUID.randomUUID();
    var created = Instant.parse("2026-09-01T00:00:00Z");
    var start = LocalDateTime.parse("2026-09-02T10:00");
    var end = start.plusHours(1);
    var request =
        new BlockRequest(
            "Objetivo histórico", start, end, "UTC", ZoneOffset.UTC, ZoneOffset.UTC, false);
    var before =
        new PlannedBlock(
            blockId,
            project,
            task,
            request,
            new ResolvedBlockTime(
                start.toInstant(ZoneOffset.UTC),
                end.toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                60),
            created);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,?,?,?,'UTC','Z','Z',false,?,?,60,?)",
        blockId,
        project,
        task,
        UUID.randomUUID(),
        request.objective(),
        start,
        end,
        Timestamp.from(before.time().startAt()),
        Timestamp.from(before.time().endAt()),
        Timestamp.from(created));
    var receipt =
        new BlockChangeReceipt(changeId, blockId, "CANCELLED", 2, occurredAt, before, null);
    jdbc.update(
        "INSERT INTO"
            + " block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)"
            + " VALUES (?,?,?,?,?,'CANCELLED',2,?,?::jsonb)",
        receipt.id(),
        project,
        task,
        blockId,
        UUID.randomUUID(),
        Timestamp.from(receipt.occurredAt()),
        json.writeValueAsString(receipt));
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,2,'cancelled',?)",
        blockId,
        Timestamp.from(receipt.occurredAt()));
    return receipt;
  }

  @Test
  void s16_limitsLookaheadToTwentyOneAndOrdersByTimeThenUuidDescending() throws Exception {
    var expected = new java.util.ArrayList<BlockChangeReceipt>();
    for (int n = 0; n < 22; n++) {
      expected.add(
          cancellation(
              new UUID(0, n + 1),
              Instant.parse(n == 0 ? "2026-09-02T10:00:00Z" : "2026-09-03T10:00:00.123456Z")));
    }
    java.util.Collections.reverse(expected);
    var before =
        jdbc.queryForMap(
            "SELECT (SELECT count(*) FROM block_changes) AS changes, (SELECT count(*) FROM"
                + " block_projections) AS projections, (SELECT count(*) FROM planned_blocks) AS"
                + " blocks, (SELECT count(*) FROM outbox_events) AS events");
    assertThat(
            new PostgresBlockChangeQueries(jdbc, manager, json).list("owner", project, task, null))
        .containsExactlyElementsOf(expected.subList(0, 21));
    assertThat(
            jdbc.queryForMap(
                "SELECT (SELECT count(*) FROM block_changes) AS changes, (SELECT count(*) FROM"
                    + " block_projections) AS projections, (SELECT count(*) FROM planned_blocks) AS"
                    + " blocks, (SELECT count(*) FROM outbox_events) AS events"))
        .isEqualTo(before);
  }

  @Test
  void s16_continuesStrictlyAfterTimeAndUuidWithoutRepeatingBoundary() throws Exception {
    var at = Instant.parse("2026-09-03T10:00:00.123456Z");
    cancellation(new UUID(0, 4), at);
    var boundary = cancellation(new UUID(0, 3), at);
    var tied = cancellation(new UUID(0, 2), at);
    var older = cancellation(new UUID(0, 9), at.minusSeconds(1));
    assertThat(
            new PostgresBlockChangeQueries(jdbc, manager, json)
                .list(
                    "owner",
                    project,
                    task,
                    new BlockChangePosition(boundary.occurredAt(), boundary.id())))
        .containsExactly(tied, older);
  }

  @Test
  void s18_translatesFailureAfterReadOnlyTransactionCommit() {
    var failingManager =
        new DataSourceTransactionManager(jdbc.getDataSource()) {
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
                new PostgresBlockChangeQueries(jdbc, failingManager, json)
                    .list("owner", project, task, null))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
        .hasCauseInstanceOf(org.springframework.transaction.TransactionSystemException.class);
  }

  @Test
  void s18_translatesSqlFailureWithoutReturningAnEmptyHistory() {
    jdbc.execute("ALTER TABLE block_changes RENAME TO unavailable_block_changes");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresBlockChangeQueries(jdbc, manager, json)
                      .list("owner", project, task, null))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class)
          .hasCauseInstanceOf(org.springframework.dao.DataAccessException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_block_changes RENAME TO block_changes");
    }
  }

  @Test
  void s18_readsReceiptByItsOwnIdWithoutChangingTheHistoricalSnapshot() throws Exception {
    var receipt = cancellation(UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00.123456Z"));
    var queries = new PostgresBlockChangeQueries(jdbc, manager, json);
    assertThat(queries.detail("owner", project, task, receipt.id())).isEqualTo(receipt);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }

  @Test
  void s25_recoversOriginalMoveByKeyAfterLaterCancellationAndNewAdapter() throws Exception {
    var initial = cancellation(UUID.randomUUID(), Instant.parse("2026-09-01T11:00:00Z"));
    var key =
        jdbc.queryForObject(
            "SELECT request_key FROM block_changes WHERE id=?", UUID.class, initial.id());
    var original = initial.before();
    var start = original.request().startLocal().plusHours(1);
    var end = start.plusHours(1);
    var moved =
        new PlannedBlock(
            original.id(),
            project,
            task,
            new BlockRequest(
                original.request().objective(),
                start,
                end,
                "UTC",
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                false),
            new ResolvedBlockTime(
                start.toInstant(ZoneOffset.UTC),
                end.toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                60),
            original.createdAt());
    var historical =
        new BlockChangeReceipt(
            initial.id(), original.id(), "RESCHEDULED", 2, initial.occurredAt(), original, moved);
    jdbc.update(
        "UPDATE block_changes SET kind='RESCHEDULED',receipt=?::jsonb WHERE id=?",
        json.writeValueAsString(historical),
        historical.id());
    var cancelled =
        new BlockChangeReceipt(
            UUID.randomUUID(),
            original.id(),
            "CANCELLED",
            3,
            Instant.parse("2026-09-03T00:00:00Z"),
            moved,
            null);
    jdbc.update(
        "INSERT INTO"
            + " block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)"
            + " VALUES (?,?,?,?,?,'CANCELLED',3,?,?::jsonb)",
        cancelled.id(),
        project,
        task,
        original.id(),
        UUID.randomUUID(),
        Timestamp.from(cancelled.occurredAt()),
        json.writeValueAsString(cancelled));
    jdbc.update(
        "UPDATE block_projections SET"
            + " version=3,updated_at=?,start_local=?,end_local=?,zone_id='UTC',start_offset='Z',end_offset='Z',start_at=?,end_at=?,duration_minutes=60"
            + " WHERE block_id=?",
        Timestamp.from(cancelled.occurredAt()),
        start,
        end,
        Timestamp.from(moved.time().startAt()),
        Timestamp.from(moved.time().endAt()),
        original.id());
    var counts =
        jdbc.queryForMap(
            "SELECT (SELECT count(*) FROM block_changes) AS changes,(SELECT count(*) FROM"
                + " outbox_events) AS events");
    assertThat(
            new PostgresBlockChangeQueries(jdbc, manager, json)
                .byRequest("owner", project, task, key))
        .isEqualTo(historical);
    assertThat(
            jdbc.queryForMap(
                "SELECT (SELECT count(*) FROM block_changes) AS changes,(SELECT count(*) FROM"
                    + " outbox_events) AS events"))
        .isEqualTo(counts);
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM block_projections WHERE block_id=?",
                Long.class,
                original.id()))
        .isEqualTo(3L);
  }

  @Test
  void s18_missingReceiptIdInOwnContextHasItsDistinctFailure() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresBlockChangeQueries(jdbc, manager, json)
                    .detail("owner", project, task, UUID.randomUUID()))
        .isInstanceOf(com.apptolast.organization.application.BlockChangeNotFoundException.class);
  }

  @Test
  void s18_missingRequestKeyInOwnContextHasItsDistinctFailure() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresBlockChangeQueries(jdbc, manager, json)
                    .byRequest("owner", project, task, UUID.randomUUID()))
        .isInstanceOf(com.apptolast.organization.application.BlockChangeNotFoundException.class);
  }
}
