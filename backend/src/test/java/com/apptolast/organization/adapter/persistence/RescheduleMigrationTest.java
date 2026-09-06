package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.BlockChangeReceipt;
import com.apptolast.organization.domain.PlannedBlock;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RescheduleMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  DriverManagerDataSource source;
  JdbcTemplate jdbc;
  String schema;
  UUID project, task, block;

  @BeforeEach
  void databaseAtPublishedVersionTwelve() {
    schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
    String url = postgres.getJdbcUrl();
    source =
        new DriverManagerDataSource(
            url + (url.contains("?") ? "&" : "?") + "currentSchema=" + schema,
            postgres.getUsername(),
            postgres.getPassword());
    Flyway.configure().dataSource(source).schemas(schema).target("12").load().migrate();
    jdbc = new JdbcTemplate(source);
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    block = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'owner','Project','','idea',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Task','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,'Original',TIMESTAMP '2030-01-07 10:00',TIMESTAMP '2030-01-07"
            + " 11:00','Historical/Removed','Z','Z',false,TIMESTAMPTZ '2030-01-07"
            + " 10:00Z',TIMESTAMPTZ '2030-01-07 11:00Z',60,TIMESTAMPTZ '2030-01-06 10:00Z')",
        block,
        project,
        task,
        UUID.randomUUID());
  }

  void upgrade() {
    Flyway.configure().dataSource(source).schemas(schema).load().migrate();
  }

  @Test
  void rejectsNonPositiveProjectionRevisionAfterUpgrade() {
    upgrade();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
                        + " (?,0,'cancelled',now())",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @CsvSource({"cancelled,2", "planned,9223372036854775807"})
  void preservesMetadataOnlyProjectionDuringUpgrade(String status, long version) {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,?,?,TIMESTAMPTZ '2030-01-07 09:00Z')",
        block,
        version,
        status);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var projections = jdbc.queryForList("SELECT * FROM block_projections");
    upgrade();
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projections);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void rejectsUnknownProjectionStatus() {
    upgrade();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
                        + " (?,2,'finished',now())",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsPartiallyCopiedInterval() {
    upgrade();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO block_projections(block_id,version,status,updated_at,start_at)"
                        + " VALUES (?,2,'planned',now(),TIMESTAMPTZ '2030-01-07 12:00Z')",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  void completeProjection() {
    jdbc.update(
        "INSERT INTO block_projections SELECT"
            + " id,2,'planned',created_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes"
            + " FROM planned_blocks WHERE id=?",
        block);
  }

  @Test
  void rejectsDurationDifferentFromInstants() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET duration_minutes=59 WHERE block_id=?", block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsZeroMinuteInterval() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET"
                        + " end_at=start_at,end_local=start_local,duration_minutes=0 WHERE"
                        + " block_id=?",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsFractionalSecondsInProjection() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET start_at=start_at+INTERVAL '0.1"
                        + " seconds',end_at=end_at+INTERVAL '0.1 seconds' WHERE block_id=?",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsResolvedYearBeyond9999() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET start_at=TIMESTAMPTZ '10000-01-07"
                        + " 10:00Z',end_at=TIMESTAMPTZ '10000-01-07 11:00Z' WHERE block_id=?",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsLocalSeconds() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET start_local=start_local+INTERVAL '1 second' WHERE"
                        + " block_id=?",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsLocalYearBeyond9999() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE block_projections SET end_local=TIMESTAMP '10000-01-07 11:00' WHERE"
                        + " block_id=?",
                    block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsEmptyHistoricalZone() {
    upgrade();
    completeProjection();
    assertThatThrownBy(
            () -> jdbc.update("UPDATE block_projections SET zone_id='' WHERE block_id=?", block))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  void change(String kind, long version, UUID contextTask) throws Exception {
    change(kind, version, contextTask, null);
  }

  void change(String kind, long version, UUID contextTask, PlannedBlock after) throws Exception {
    var original =
        jdbc.queryForObject(
            "SELECT * FROM planned_blocks WHERE id=?", PostgresBlockStore.MAPPER, block);
    var id = UUID.randomUUID();
    var receipt =
        new BlockChangeReceipt(
            id, block, kind, version, Instant.parse("2030-01-07T09:00:00Z"), original, after);
    var payload =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .writeValueAsString(receipt);
    jdbc.update(
        "INSERT INTO"
            + " block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)"
            + " VALUES (?,?,?,?,?,?,?,TIMESTAMPTZ '2030-01-07 09:00Z',?::jsonb)",
        id,
        project,
        contextTask,
        block,
        UUID.randomUUID(),
        kind,
        version,
        payload);
  }

  @Test
  void rejectsNonPositiveChangeRevision() {
    upgrade();
    assertThatThrownBy(() -> change("CANCELLED", 0, task))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsUnknownChangeKind() {
    upgrade();
    assertThatThrownBy(() -> change("UPDATED", 2, task))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsTwoChangesForTheSameBlockRevision() throws Exception {
    upgrade();
    change("CANCELLED", 2, task);
    assertThatThrownBy(() -> change("CANCELLED", 2, task))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
  }

  @Test
  void rejectsChangeWhoseTaskDoesNotOwnTheBlock() {
    upgrade();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        project);
    assertThatThrownBy(() -> change("CANCELLED", 2, otherTask))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
  }

  @Test
  void rejectsReceiptThatIsNotAJsonObject() throws Exception {
    upgrade();
    change("CANCELLED", 2, task);
    var stored = jdbc.queryForList("SELECT * FROM block_changes");
    assertThatThrownBy(
            () ->
                jdbc.update("UPDATE block_changes SET receipt='[]'::jsonb WHERE block_id=?", block))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(jdbc.queryForList("SELECT * FROM block_changes")).isEqualTo(stored);
  }

  @Test
  void doesNotBackfillOriginalBlocksDuringUpgrade() {
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    upgrade();
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void preservesFullCancelledProjectionAndInternalReceiptDuringUpgrade() throws Exception {
    completeProjection();
    jdbc.update("UPDATE block_projections SET status='cancelled' WHERE block_id=?", block);
    change("CANCELLED", 2, task);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var projections = jdbc.queryForList("SELECT * FROM block_projections");
    var changes = jdbc.queryForList("SELECT * FROM block_changes");
    upgrade();
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projections);
    assertThat(jdbc.queryForList("SELECT * FROM block_changes")).isEqualTo(changes);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void preservesMoveWithReversedLocalTimesAndHistoricalZoneDuringUpgrade() throws Exception {
    completeProjection();
    jdbc.update(
        "UPDATE block_projections SET start_local=TIMESTAMP '2030-10-27 02:45',end_local=TIMESTAMP"
            + " '2030-10-27 02:15',start_offset='+02:00',end_offset='+01:00',start_at=TIMESTAMPTZ"
            + " '2030-10-27 00:45Z',end_at=TIMESTAMPTZ '2030-10-27 01:15Z',duration_minutes=30"
            + " WHERE block_id=?",
        block);
    var after =
        jdbc.queryForObject(
            "SELECT"
                + " b.id,b.project_id,b.task_id,b.objective,p.start_local,p.end_local,p.zone_id,p.start_offset,p.end_offset,b.allow_over_budget,p.start_at,p.end_at,p.duration_minutes,b.created_at"
                + " FROM planned_blocks b JOIN block_projections p ON p.block_id=b.id WHERE b.id=?",
            PostgresBlockStore.MAPPER,
            block);
    change("RESCHEDULED", 2, task, after);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var projections = jdbc.queryForList("SELECT * FROM block_projections");
    var changes = jdbc.queryForList("SELECT * FROM block_changes");
    upgrade();
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projections);
    assertThat(jdbc.queryForList("SELECT * FROM block_changes")).isEqualTo(changes);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void rollsBackTheWholeMigrationWhenALateConstraintRejectsLegacyData() throws Exception {
    completeProjection();
    change("CANCELLED", 2, task);
    jdbc.update("UPDATE block_changes SET receipt='[]'::jsonb WHERE block_id=?", block);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var projections = jdbc.queryForList("SELECT * FROM block_projections");
    var changes = jdbc.queryForList("SELECT * FROM block_changes");
    var history = jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank");
    var constraints =
        jdbc.queryForList(
            "SELECT conname FROM pg_constraint WHERE connamespace=?::regnamespace ORDER BY conname",
            String.class,
            schema);
    assertThatThrownBy(this::upgrade).isInstanceOf(FlywayException.class);
    assertThat(
            jdbc.queryForList(
                "SELECT conname FROM pg_constraint WHERE connamespace=?::regnamespace ORDER BY"
                    + " conname",
                String.class,
                schema))
        .isEqualTo(constraints);
    assertThat(jdbc.queryForList("SELECT * FROM flyway_schema_history ORDER BY installed_rank"))
        .isEqualTo(history);
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projections);
    assertThat(jdbc.queryForList("SELECT * FROM block_changes")).isEqualTo(changes);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }
}
