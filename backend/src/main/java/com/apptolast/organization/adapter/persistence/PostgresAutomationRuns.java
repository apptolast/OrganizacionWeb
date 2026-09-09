package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AutomationRunCursor;
import com.apptolast.organization.application.AutomationRunStore;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.AutomationRun;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** The history of one rule, newest first. A deleted rule keeps no readable history. */
public final class PostgresAutomationRuns implements AutomationRunStore {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate reading;

  public PostgresAutomationRuns(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
    this.jdbc = jdbc;
    this.reading = new TransactionTemplate(transactions);
    this.reading.setReadOnly(true);
    this.reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  @Override
  public List<AutomationRun> page(String owner, UUID ruleId, AutomationRunCursor after, int limit) {
    try {
      return reading.execute(
          status ->
              after == null
                  ? jdbc.query(first(), PostgresAutomationRuns::read, owner, ruleId, limit)
                  : jdbc.query(
                      following(),
                      PostgresAutomationRuns::read,
                      owner,
                      ruleId,
                      Timestamp.from(after.executedAt()),
                      Timestamp.from(after.executedAt()),
                      after.id(),
                      limit));
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private static String first() {
    return select() + " ORDER BY executed_at DESC, id DESC LIMIT ?";
  }

  private static String following() {
    return select()
        + " AND (executed_at < ? OR (executed_at = ? AND id < ?))"
        + " ORDER BY executed_at DESC, id DESC LIMIT ?";
  }

  private static String select() {
    return "SELECT id, rule_id, owner_id, event_id, event_type, occurred_at, attempt, status,"
        + " created_task_id, delivery_id, error_code, executed_at FROM automation_runs"
        + " WHERE owner_id = ? AND rule_id = ?";
  }

  private static AutomationRun read(ResultSet row, int index) throws SQLException {
    return new AutomationRun(
        row.getObject("id", UUID.class),
        row.getObject("rule_id", UUID.class),
        row.getString("owner_id"),
        row.getObject("event_id", UUID.class),
        row.getString("event_type"),
        row.getTimestamp("occurred_at").toInstant(),
        row.getInt("attempt"),
        row.getString("status"),
        row.getObject("created_task_id", UUID.class),
        row.getObject("delivery_id", UUID.class),
        row.getString("error_code"),
        row.getTimestamp("executed_at").toInstant());
  }
}
