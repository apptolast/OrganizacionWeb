package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.IssueImportInProgressException;
import com.apptolast.organization.application.IssueImportReceiptStore;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.IssueImportReceipt;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Recibos de importación. Empezar una importación es la sección crítica del conector: en la misma
 * transacción se cierra el recibo abandonado, si lo hay, y se intenta insertar el nuevo. El índice
 * único parcial sobre los recibos en curso es quien arbitra, así que dos peticiones simultáneas
 * reales no pueden dejar dos recibos {@code running} del mismo propietario.
 */
@Component
public final class PostgresIssueImportReceiptStore implements IssueImportReceiptStore {
  private static final String COLUMNS =
      "id, source, project_id, project_path, status, created, skipped, failed, truncated, error_code,"
          + " started_at, finished_at";
  private static final String INTERRUPTED = "INTERRUPTED";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresIssueImportReceiptStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.transaction = transaction;
  }

  @Override
  public IssueImportReceipt begin(
      String ownerId,
      UUID projectId,
      String source,
      String projectPath,
      Instant startedAt,
      Instant staleBefore) {
    var receipt =
        new IssueImportReceipt(
            UUID.randomUUID(),
            source,
            projectId,
            projectPath,
            IssueImportReceipt.RUNNING,
            0,
            0,
            0,
            false,
            null,
            startedAt,
            null);
    try {
      return transaction.execute(
          status -> {
            interruptAbandoned(ownerId, staleBefore, startedAt);
            insert(ownerId, receipt);
            return receipt;
          });
    } catch (DuplicateKeyException error) {
      throw new IssueImportInProgressException();
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  /**
   * Un recibo en curso más viejo que el plazo dejó de existir de verdad: se cierra y cede el turno.
   */
  private void interruptAbandoned(String ownerId, Instant staleBefore, Instant finishedAt) {
    jdbc.update(
        "UPDATE issue_import_receipts SET status='failed', error_code=?, finished_at=?"
            + " WHERE owner_id=? AND status='running' AND started_at < ?",
        INTERRUPTED,
        Timestamp.from(finishedAt),
        ownerId,
        Timestamp.from(staleBefore));
  }

  private void insert(String ownerId, IssueImportReceipt receipt) {
    jdbc.update(
        "INSERT INTO issue_import_receipts(id,owner_id,source,project_id,project_path,status,created,"
            + "skipped,failed,truncated,error_code,started_at,finished_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
        receipt.id(),
        ownerId,
        receipt.source(),
        receipt.projectId(),
        receipt.projectPath(),
        receipt.status(),
        receipt.created(),
        receipt.skipped(),
        receipt.failed(),
        receipt.truncated(),
        receipt.errorCode(),
        Timestamp.from(receipt.startedAt()),
        null);
  }

  @Override
  public void progress(String ownerId, UUID importId, int created, int skipped, int failed) {
    guarded(
        () ->
            jdbc.update(
                "UPDATE issue_import_receipts SET created=?, skipped=?, failed=?"
                    + " WHERE id=? AND owner_id=?",
                created,
                skipped,
                failed,
                importId,
                ownerId));
  }

  @Override
  public IssueImportReceipt finish(
      String ownerId,
      UUID importId,
      String status,
      String errorCode,
      boolean truncated,
      Instant finishedAt) {
    return guarded(
        () -> {
          jdbc.update(
              "UPDATE issue_import_receipts SET status=?, error_code=?, truncated=?,"
                  + " finished_at=GREATEST(?, started_at) WHERE id=? AND owner_id=?",
              status,
              errorCode,
              truncated,
              Timestamp.from(finishedAt),
              importId,
              ownerId);
          return read(ownerId, importId).orElseThrow(() -> missing(importId));
        });
  }

  @Override
  public Optional<IssueImportReceipt> find(String ownerId, UUID importId) {
    return guarded(() -> read(ownerId, importId));
  }

  @Override
  public Optional<IssueImportReceipt> latest(String ownerId, String source) {
    return guarded(
        () ->
            jdbc
                .query(
                    "SELECT "
                        + COLUMNS
                        + " FROM issue_import_receipts WHERE owner_id=? AND source=?"
                        + " ORDER BY started_at DESC, id DESC LIMIT 1",
                    mapper(),
                    ownerId,
                    source)
                .stream()
                .findFirst());
  }

  @Override
  public boolean importing(String ownerId, Instant staleBefore) {
    return guarded(
        () ->
            Boolean.TRUE.equals(
                jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM issue_import_receipts"
                        + " WHERE owner_id=? AND status='running' AND started_at >= ?)",
                    Boolean.class,
                    ownerId,
                    Timestamp.from(staleBefore))));
  }

  private Optional<IssueImportReceipt> read(String ownerId, UUID importId) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM issue_import_receipts WHERE id=? AND owner_id=?",
            mapper(),
            importId,
            ownerId)
        .stream()
        .findFirst();
  }

  private static RowMapper<IssueImportReceipt> mapper() {
    return (row, index) ->
        new IssueImportReceipt(
            row.getObject("id", UUID.class),
            row.getString("source"),
            row.getObject("project_id", UUID.class),
            row.getString("project_path"),
            row.getString("status"),
            row.getInt("created"),
            row.getInt("skipped"),
            row.getInt("failed"),
            row.getBoolean("truncated"),
            row.getString("error_code"),
            row.getTimestamp("started_at").toInstant(),
            instant(row, "finished_at"));
  }

  private static Instant instant(ResultSet row, String column) throws SQLException {
    var value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static StorageUnavailableException missing(UUID importId) {
    return new StorageUnavailableException(
        new IllegalStateException("The receipt vanished while it was being closed: " + importId));
  }

  private <T> T guarded(java.util.function.Supplier<T> work) {
    try {
      return transaction.execute(status -> work.get());
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
