package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AutomationEventProjects;
import com.apptolast.organization.application.AutomationEventTail;
import com.apptolast.organization.application.AutomationFacts;
import com.apptolast.organization.application.AutomationLoopGuard;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.AutomationEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Everything the rules read about the owner's own world: the tail of the outbox, the references an
 * event points at, the names in force right now and the loop guard.
 */
public final class PostgresAutomationEvents
    implements AutomationEventTail, AutomationEventProjects, AutomationFacts, AutomationLoopGuard {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate reading;

  public PostgresAutomationEvents(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
    this.reading = new TransactionTemplate(transactions);
    this.reading.setReadOnly(true);
    this.reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  @Override
  public List<AutomationEvent> recent(String owner, int limit) {
    return read(
        () ->
            jdbc.query(
                "SELECT event_id, aggregate_id, owner_id, event_type, occurred_at, payload"
                    + " FROM outbox_events WHERE owner_id = ? AND status <> 'blocked'"
                    + " ORDER BY occurred_at DESC, event_id DESC LIMIT ?",
                this::event,
                owner,
                limit));
  }

  @Override
  public Optional<UUID> projectOfTask(String owner, UUID taskId) {
    return one(
        "SELECT t.project_id FROM tasks t JOIN projects p ON p.id = t.project_id"
            + " WHERE t.id = ? AND p.owner_id = ?",
        UUID.class,
        taskId,
        owner);
  }

  @Override
  public Optional<UUID> taskOfWorkSession(String owner, UUID sessionId) {
    return one(
        "SELECT task_id FROM work_sessions WHERE id = ? AND owner_id = ?",
        UUID.class,
        sessionId,
        owner);
  }

  @Override
  public Optional<String> projectName(String owner, UUID projectId) {
    return one(
        "SELECT name FROM projects WHERE id = ? AND owner_id = ?", String.class, projectId, owner);
  }

  @Override
  public Optional<String> taskTitle(String owner, UUID taskId) {
    return one(
        "SELECT t.title FROM tasks t JOIN projects p ON p.id = t.project_id"
            + " WHERE t.id = ? AND p.owner_id = ?",
        String.class,
        taskId,
        owner);
  }

  @Override
  public boolean projectCompleted(String owner, UUID projectId) {
    return one(
            "SELECT status FROM projects WHERE id = ? AND owner_id = ?",
            String.class,
            projectId,
            owner)
        .filter("completed"::equals)
        .isPresent();
  }

  @Override
  public boolean createdByAutomation(String owner, UUID taskId) {
    return one(
            "SELECT id FROM automation_runs WHERE owner_id = ? AND created_task_id = ? LIMIT 1",
            UUID.class,
            owner,
            taskId)
        .isPresent();
  }

  private <T> Optional<T> one(String sql, Class<T> type, Object... arguments) {
    return read(() -> jdbc.queryForList(sql, type, arguments).stream().findFirst());
  }

  private AutomationEvent event(ResultSet row, int index) throws SQLException {
    return new AutomationEvent(
        row.getObject("event_id", UUID.class),
        row.getString("owner_id"),
        row.getString("event_type"),
        row.getObject("aggregate_id", UUID.class),
        row.getTimestamp("occurred_at").toInstant(),
        payload(row.getString("payload")));
  }

  private Map<String, Object> payload(String raw) {
    try {
      return json.readValue(raw, new TypeReference<Map<String, Object>>() {});
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("El payload almacenado no se pudo leer.", error);
    }
  }

  private <T> T read(java.util.function.Supplier<T> work) {
    try {
      return reading.execute(status -> work.get());
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
