package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresTaskCommit implements TaskCommit, SubtaskCommit {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;

  public PostgresTaskCommit(JdbcTemplate jdbc, TransactionTemplate transaction, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.json = json;
  }

  public Task save(String owner, UUID project, Function<String, TaskCreation> operation) {
    return save(owner, project, null, operation);
  }

  public Task save(
      String owner, UUID project, UUID parent, Function<String, TaskCreation> operation) {
    try {
      return transaction.execute(
          status -> {
            // The shared project row serializes task creation with state changes under
            // READ_COMMITTED. No global capacity lock is needed for a child insertion.
            var states =
                jdbc.queryForList(
                    "SELECT status FROM projects WHERE owner_id=? AND id=? FOR UPDATE",
                    String.class,
                    owner,
                    project);
            if (states.isEmpty()) throw new ResourceNotFoundException();
            if (parent != null
                && !Boolean.TRUE.equals(
                    jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM tasks WHERE project_id=? AND id=?)",
                        Boolean.class,
                        project,
                        parent))) throw new ResourceNotFoundException();
            var creation = operation.apply(states.getFirst());
            var task = creation.task();
            int insertedTask =
                jdbc.update(
                    "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,created_at,updated_at,parent_id) VALUES (?,?,?,?,?,?,?,?,?)",
                    task.id(),
                    project,
                    task.title(),
                    task.completionCriterion(),
                    task.estimatedMinutes(),
                    task.status(),
                    Timestamp.from(task.createdAt()),
                    Timestamp.from(task.updatedAt()),
                    parent);
            if (insertedTask != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Task write did not affect one row"));
            var event = creation.event();
            int insertedEvent =
                jdbc.update(
                    "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
                    event.eventId(),
                    project,
                    owner,
                    event.type(),
                    event.schemaVersion(),
                    Timestamp.from(event.occurredAt()),
                    serialize(event));
            if (insertedEvent != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Event write did not affect one row"));
            return task;
          });
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private String serialize(TaskCreationEvent event) {
    try {
      return json.writeValueAsString(event);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Event serialization failed", error);
    }
  }
}
