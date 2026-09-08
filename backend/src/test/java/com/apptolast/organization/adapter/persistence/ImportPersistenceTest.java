package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.PreviewImportData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
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
class ImportPersistenceTest {
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
  void s23_suppressedReceiptInsertCannotReportAFalseSuccess() throws Exception {
    var owner = "receipt-suppressed";
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    jdbc.execute(
        "CREATE FUNCTION suppress_import_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_import_receipt BEFORE INSERT ON import_receipts FOR EACH ROW WHEN (NEW.owner_id='receipt-suppressed') EXECUTE FUNCTION suppress_import_receipt()");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .apply(
                          owner,
                          java.util.UUID.randomUUID(),
                          sha,
                          new ByteArrayInputStream(bytes.toByteArray()),
                          () -> timestamp))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER suppress_import_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION suppress_import_receipt()");
    }
  }

  @Test
  void s23_receiptInsertFailureIsStorageUnavailableAndLeavesNoCommittedReceipt() throws Exception {
    var owner = "receipt-failure";
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    jdbc.execute(
        "CREATE FUNCTION reject_import_receipt() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'controlled receipt failure'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_import_receipt BEFORE INSERT ON import_receipts FOR EACH ROW WHEN (NEW.owner_id='receipt-failure') EXECUTE FUNCTION reject_import_receipt()");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new PostgresImportDataStore(jdbc, manager)
                      .apply(
                          owner,
                          java.util.UUID.randomUUID(),
                          sha,
                          new ByteArrayInputStream(bytes.toByteArray()),
                          () -> timestamp))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
          .isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_import_receipt ON import_receipts");
      jdbc.execute("DROP FUNCTION reject_import_receipt()");
    }
  }

  @Test
  void s21_reusingACommittedKeyWithDifferentValidBytesCannotReturnItsReceipt() throws Exception {
    var owner = "reused-key";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000444");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var digest = java.security.MessageDigest.getInstance("SHA-256");
    var sha = java.util.HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var original =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    bytes.write(' ');
    var otherSha = java.util.HexFormat.of().formatHex(digest.digest(bytes.toByteArray()));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                store.apply(
                    owner,
                    key,
                    otherSha,
                    new ByteArrayInputStream(bytes.toByteArray()),
                    () -> {
                      throw new AssertionError("Reused key must not consult Clock");
                    }))
        .isInstanceOf(com.apptolast.organization.application.ImportKeyReusedException.class);
    assertThat(store.find(owner, key)).contains(original);
  }

  @Test
  void s21_replayReturnsTheOriginalReceiptWithoutClockOrPhysicalRewrite() throws Exception {
    var owner = "replay-owner";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000333");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var original =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    var before =
        jdbc.queryForMap(
            "SELECT xmin::text,ctid::text,row_to_json(r)::text AS contents FROM import_receipts r WHERE owner_id=? AND request_key=?",
            owner,
            key);
    var replay =
        store.apply(
            owner,
            key,
            sha,
            new ByteArrayInputStream(bytes.toByteArray()),
            () -> {
              throw new AssertionError("Replay must not consult Clock");
            });
    assertThat(replay).isEqualTo(original);
    assertThat(
            jdbc.queryForMap(
                "SELECT xmin::text,ctid::text,row_to_json(r)::text AS contents FROM import_receipts r WHERE owner_id=? AND request_key=?",
                owner,
                key))
        .isEqualTo(before);
  }

  @Test
  void s20_hashMismatchPrecedesOwnerValidationAndDoesNotCreateAReceipt() throws Exception {
    var owner = "hash-mismatch";
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("different-file-owner", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .apply(
                        owner,
                        java.util.UUID.randomUUID(),
                        "b".repeat(64),
                        new ByteArrayInputStream(bytes.toByteArray()),
                        () -> {
                          throw new AssertionError("Rejected request must not consult Clock");
                        }))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }

  @Test
  void s22_committedReceiptCanBeReadWithoutTheOriginalFile() throws Exception {
    var owner = "read-receipt";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000222");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var store = new PostgresImportDataStore(jdbc, manager);
    var receipt =
        store.apply(
            owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()), () -> timestamp);
    var result =
        new com.apptolast.organization.application.ReadImportReceipt(store).find(owner, key);
    assertThat(result).contains(receipt);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isEqualTo(1);
  }

  @Test
  void s1_emptyApplyLocksTheDurableTablesAndCommitsOnlyItsOperationalReceipt() throws Exception {
    var owner = "empty-apply";
    var key = java.util.UUID.fromString("00000000-0000-0000-0000-000000000111");
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty(owner, timestamp).writeTo(bytes);
    var sha =
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
    var clock = org.mockito.Mockito.mock(java.time.Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenAnswer(
            invocation -> {
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("read committed");
              assertThat(jdbc.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("2s");
              assertThat(jdbc.queryForObject("SHOW statement_timeout", String.class))
                  .isEqualTo("10s");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='advisory' AND granted",
                          Integer.class))
                  .isEqualTo(2);
              assertThat(
                      jdbc.queryForList(
                          "SELECT c.relname FROM pg_locks l JOIN pg_class c ON c.oid=l.relation WHERE l.pid=pg_backend_pid() AND l.mode='ExclusiveLock' AND l.granted ORDER BY c.relname",
                          String.class))
                  .containsExactly(
                      "appearance_preferences",
                      "availability_preferences",
                      "block_changes",
                      "block_projections",
                      "customization_preferences",
                      "import_receipts",
                      "planned_blocks",
                      "project_custom_field_values",
                      "projects",
                      "task_custom_field_values",
                      "task_status_history",
                      "tasks",
                      "work_session_changes",
                      "work_session_intervals",
                      "work_sessions");
              assertThat(
                      jdbc.queryForObject(
                          "SELECT count(*) FROM import_receipts WHERE owner_id=?",
                          Integer.class,
                          owner))
                  .isZero();
              return timestamp.plusNanos(789);
            });
    var receipt =
        new com.apptolast.organization.application.ApplyImportData(
                new PostgresImportDataStore(jdbc, manager), clock)
            .apply(owner, key, sha, new ByteArrayInputStream(bytes.toByteArray()));
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    assertThat(receipt)
        .isEqualTo(
            new com.apptolast.organization.application.ImportReceipt(
                key, sha, bytes.size(), timestamp, "NO_CHANGE", zero, zero));
    assertThat(
            jdbc.queryForObject(
                "SELECT file_sha256 FROM import_receipts WHERE owner_id=? AND request_key=?",
                String.class,
                owner,
                key))
        .isEqualTo(sha);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    org.mockito.Mockito.verify(clock).instant();
  }

  @Test
  void s7_previewCannotAdoptTheOwnerFromTheFile() throws Exception {
    var bytes = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("private-other", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(bytes);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PostgresImportDataStore(jdbc, manager)
                    .preview("authenticated", new ByteArrayInputStream(bytes.toByteArray())))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class)
        .hasMessage("El archivo de importación no es válido.");
  }

  @Test
  void s1_s18_emptyPreviewUsesARepeatableSnapshotAndDoesNotCreateDefaultsOrOutbox()
      throws Exception {
    var bytes = new ByteArrayOutputStream();
    var timestamp = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty("empty-preview", timestamp).writeTo(bytes);
    var body =
        new ByteArrayInputStream(bytes.toByteArray()) {
          @Override
          public synchronized int read(byte[] buffer, int offset, int length) {
            assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                .isEqualTo("repeatable read");
            return super.read(buffer, offset, length);
          }
        };
    var result =
        new PreviewImportData(new PostgresImportDataStore(jdbc, manager))
            .preview("empty-preview", body);
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    assertThat(result.owner()).isEqualTo("empty-preview");
    assertThat(result.exportedAt()).isEqualTo(timestamp);
    assertThat(result.counts()).isEqualTo(zero);
    assertThat(result.insertCounts()).isEqualTo(zero);
    assertThat(result.identicalCounts()).isEqualTo(zero);
    assertThat(result.runningSessions()).isEmpty();
    for (var table :
        java.util.List.of(
            "projects",
            "tasks",
            "availability_preferences",
            "appearance_preferences",
            "customization_preferences",
            "outbox_events")) {
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class)).isZero();
    }
  }
}
