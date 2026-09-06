package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.BlockChangeQueries;
import com.apptolast.organization.application.ResourceNotFoundException;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.BlockChangePosition;
import com.apptolast.organization.domain.BlockChangeReceipt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@org.springframework.stereotype.Component
public final class PostgresBlockChangeQueries implements BlockChangeQueries {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate transaction;

  public PostgresBlockChangeQueries(
      JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
    this.transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public List<BlockChangeReceipt> list(
      String owner, UUID project, UUID task, BlockChangePosition after) {
    return read(
        owner,
        project,
        task,
        () ->
            jdbc.query(
                "SELECT c.receipt::text FROM block_changes c JOIN projects p ON p.id=c.project_id"
                    + " WHERE p.owner_id=? AND c.project_id=? AND c.task_id=? AND (?::timestamptz"
                    + " IS NULL OR (c.occurred_at,c.id)<(?::timestamptz,?::uuid)) ORDER BY"
                    + " c.occurred_at DESC,c.id DESC LIMIT 21",
                this::receipt,
                owner,
                project,
                task,
                after == null ? null : Timestamp.from(after.occurredAt()),
                after == null ? null : Timestamp.from(after.occurredAt()),
                after == null ? null : after.id()));
  }

  public BlockChangeReceipt detail(String owner, UUID project, UUID task, UUID changeId) {
    return read(
        owner,
        project,
        task,
        () ->
            jdbc
                .query(
                    "SELECT receipt::text FROM block_changes WHERE project_id=? AND task_id=? AND"
                        + " id=?",
                    this::receipt,
                    project,
                    task,
                    changeId)
                .stream()
                .findFirst()
                .orElseThrow(
                    com.apptolast.organization.application.BlockChangeNotFoundException::new));
  }

  public BlockChangeReceipt byRequest(String owner, UUID project, UUID task, UUID key) {
    return read(
        owner,
        project,
        task,
        () ->
            jdbc
                .query(
                    "SELECT receipt::text FROM block_changes WHERE project_id=? AND task_id=? AND"
                        + " request_key=?",
                    this::receipt,
                    project,
                    task,
                    key)
                .stream()
                .findFirst()
                .orElseThrow(
                    com.apptolast.organization.application.BlockChangeNotFoundException::new));
  }

  private BlockChangeReceipt receipt(ResultSet row, int index) throws SQLException {
    try {
      return json.readValue(row.getString(1), BlockChangeReceipt.class);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Stored receipt is invalid", error);
    }
  }

  private <T> T read(String owner, UUID project, UUID task, Supplier<T> query) {
    try {
      return transaction.execute(
          status -> {
            if (!Boolean.TRUE.equals(
                jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM tasks t JOIN projects p ON p.id=t.project_id WHERE"
                        + " p.owner_id=? AND p.id=? AND t.id=?)",
                    Boolean.class,
                    owner,
                    project,
                    task))) throw new ResourceNotFoundException();
            return query.get();
          });
    } catch (TransactionException | DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
