package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ResourceNotFoundException;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.application.TaskStatusChange;
import com.apptolast.organization.application.TaskStatusChanged;
import com.apptolast.organization.application.TaskStatusEditing;
import com.apptolast.organization.application.TaskStatusQueries;
import com.apptolast.organization.domain.TaskSnapshot;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public final class PostgresTaskStatusStore implements TaskStatusQueries, TaskStatusEditing {
  private final JdbcTemplate jdbc;
  private final org.springframework.transaction.support.TransactionTemplate transaction;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public PostgresTaskStatusStore(
      JdbcTemplate jdbc,
      org.springframework.transaction.support.TransactionTemplate transaction,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.json = json;
  }

  private static final RowMapper<TaskSnapshot> MAPPER =
      (row, n) ->
          new TaskSnapshot(
              PostgresTaskQueries.MAPPER.mapRow(row, n),
              row.getLong("version"),
              row.getTimestamp("completed_at") == null
                  ? null
                  : row.getTimestamp("completed_at").toInstant());

  public TaskSnapshot status(String owner, UUID project, UUID id) {
    try {
      return jdbc
          .query(
              "SELECT t.* FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? AND t.project_id=? AND t.id=?",
              MAPPER,
              owner,
              project,
              id)
          .stream()
          .findFirst()
          .orElseThrow(ResourceNotFoundException::new);
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  public TaskSnapshot update(
      String owner,
      UUID project,
      UUID id,
      java.util.function.Function<TaskSnapshot, TaskStatusChange> operation) {
    try {
      return transaction.execute(
          status -> {
            // Only non-key task columns change. This lock serializes writers under READ_COMMITTED
            // while permitting the KEY SHARE needed by concurrent child-insertion foreign keys.
            var previous =
                jdbc
                    .query(
                        "SELECT t.* FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? AND t.project_id=? AND t.id=? FOR NO KEY UPDATE OF t",
                        MAPPER,
                        owner,
                        project,
                        id)
                    .stream()
                    .findFirst()
                    .orElseThrow(ResourceNotFoundException::new);
            var change = operation.apply(previous);
            if (change.event() == null) return previous;
            var next = change.snapshot();
            var event = change.event();
            int updated =
                jdbc.update(
                    "UPDATE tasks SET status=?,completed_at=?,updated_at=?,version=? WHERE project_id=? AND id=? AND version=?",
                    next.task().status(),
                    next.completedAt() == null ? null : java.sql.Timestamp.from(next.completedAt()),
                    java.sql.Timestamp.from(next.task().updatedAt()),
                    next.version(),
                    project,
                    id,
                    previous.version());
            if (updated != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Task write did not affect one row"));
            int insertedHistory =
                jdbc.update(
                    "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,?,?,?,?)",
                    event.eventId(),
                    project,
                    id,
                    next.version(),
                    event.fromStatus(),
                    event.toStatus(),
                    java.sql.Timestamp.from(event.occurredAt()));
            if (insertedHistory != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("History write did not affect one row"));
            int insertedEvent =
                jdbc.update(
                    "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
                    event.eventId(),
                    project,
                    owner,
                    event.type(),
                    event.schemaVersion(),
                    java.sql.Timestamp.from(event.occurredAt()),
                    serialize(event));
            if (insertedEvent != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Event write did not affect one row"));
            return next;
          });
    } catch (DataAccessException | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private String serialize(TaskStatusChanged event) {
    try {
      return json.writeValueAsString(event);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("Event serialization failed", error);
    }
  }
}
