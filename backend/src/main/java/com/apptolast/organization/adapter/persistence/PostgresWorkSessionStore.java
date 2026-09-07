package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresWorkSessionStore implements WorkSessionStarting, WorkSessionQueries {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final TransactionTemplate readOnly;
  private final ObjectMapper json;

  public PostgresWorkSessionStore(
      JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    this.readOnly = new TransactionTemplate(manager);
    this.readOnly.setReadOnly(true);
    this.readOnly.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
    this.json = json;
  }

  @Override
  public WorkSessionConfirmation commit(
      String owner,
      UUID project,
      UUID task,
      UUID key,
      int minutes,
      Function<WorkSessionContext, WorkSessionChange> operation) {
    return storage(() -> commitAttempt(owner, project, task, key, minutes, operation));
  }

  private WorkSessionConfirmation commitAttempt(
      String owner,
      UUID project,
      UUID task,
      UUID key,
      int minutes,
      Function<WorkSessionContext, WorkSessionChange> operation) {
    try {
      return transaction.execute(
          status -> {
            var projectStatus =
                jdbc
                    .query(
                        "SELECT status FROM projects WHERE owner_id=? AND id=? FOR SHARE",
                        (row, n) -> row.getString(1),
                        owner,
                        project)
                    .stream()
                    .findFirst()
                    .orElseThrow(ResourceNotFoundException::new);
            var taskStatus =
                jdbc
                    .query(
                        "SELECT status FROM tasks WHERE project_id=? AND id=? FOR SHARE",
                        (row, n) -> row.getString(1),
                        project,
                        task)
                    .stream()
                    .findFirst()
                    .orElseThrow(ResourceNotFoundException::new);
            var prior = replay(owner, project, task, key, minutes);
            if (prior.isPresent()) return prior.orElseThrow();
            var active = activeId(owner);
            if (active.isPresent())
              throw new WorkSessionAlreadyActiveException(active.orElseThrow());
            WorkSessionContext.requireEligible(projectStatus, taskStatus);
            var zone =
                jdbc
                    .query(
                        "SELECT zone_id FROM availability_preferences WHERE owner_id=? FOR SHARE",
                        (row, n) -> row.getString(1),
                        owner)
                    .stream()
                    .findFirst();
            var change = operation.apply(new WorkSessionContext(projectStatus, taskStatus, zone));
            var session = change.session();
            try {
              requireOne(
                  jdbc.update(
                      "INSERT INTO"
                          + " work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status)"
                          + " VALUES (?,?,?,?,?,?,?,?,?,'running')",
                      session.id(),
                      owner,
                      project,
                      task,
                      key,
                      Timestamp.from(session.startedAt()),
                      minutes,
                      Timestamp.from(session.plannedEndAt()),
                      session.zoneId()));
            } catch (org.springframework.dao.DuplicateKeyException collision) {
              throw new SessionInsertCollision(collision);
            }
            var event = change.event();
            try {
              requireOne(
                  jdbc.update(
                      "INSERT INTO"
                          + " outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload)"
                          + " VALUES (?,?,?,?,?,?,?::jsonb)",
                      event.eventId(),
                      event.aggregateId(),
                      owner,
                      event.type(),
                      event.schemaVersion(),
                      Timestamp.from(event.occurredAt()),
                      json.writeValueAsString(event)));
            } catch (JsonProcessingException error) {
              throw new IllegalStateException(error);
            }
            return new WorkSessionConfirmation(session, false);
          });
    } catch (SessionInsertCollision collision) {
      return transaction.execute(
          status -> {
            var prior = replay(owner, project, task, key, minutes);
            if (prior.isPresent()) return prior.orElseThrow();
            var active = activeId(owner);
            if (active.isPresent())
              throw new WorkSessionAlreadyActiveException(active.orElseThrow());
            throw new StorageUnavailableException(collision);
          });
    }
  }

  private static final org.springframework.jdbc.core.RowMapper<
          com.apptolast.organization.domain.SessionStart>
      MAPPER =
          (row, n) ->
              new com.apptolast.organization.domain.SessionStart(
                  row.getObject("id", UUID.class),
                  row.getObject("project_id", UUID.class),
                  row.getObject("task_id", UUID.class),
                  row.getTimestamp("started_at").toInstant(),
                  row.getInt("planned_minutes"),
                  row.getTimestamp("planned_end_at").toInstant(),
                  row.getString("zone_id"));

  private java.util.Optional<WorkSessionConfirmation> replay(
      String owner, UUID project, UUID task, UUID key, int minutes) {
    return jdbc
        .query("SELECT * FROM work_sessions WHERE owner_id=? AND request_key=?", MAPPER, owner, key)
        .stream()
        .findFirst()
        .map(
            prior -> {
              if (!prior.projectId().equals(project)
                  || !prior.taskId().equals(task)
                  || prior.plannedMinutes() != minutes)
                throw new WorkSessionIdempotencyConflictException();
              return new WorkSessionConfirmation(prior, true);
            });
  }

  private java.util.Optional<UUID> activeId(String owner) {
    return jdbc
        .query(
            "SELECT id FROM work_sessions WHERE owner_id=? AND status='running'",
            (row, n) -> row.getObject(1, UUID.class),
            owner)
        .stream()
        .findFirst();
  }

  public java.util.Optional<com.apptolast.organization.domain.SessionStart> active(String owner) {
    return read("SELECT * FROM work_sessions WHERE owner_id=? AND status='running'", owner);
  }

  public java.util.Optional<com.apptolast.organization.domain.SessionStart> detail(
      String owner, UUID id) {
    return read("SELECT * FROM work_sessions WHERE owner_id=? AND id=?", owner, id);
  }

  public java.util.Optional<com.apptolast.organization.domain.SessionStart> byRequest(
      String owner, UUID key) {
    return read("SELECT * FROM work_sessions WHERE owner_id=? AND request_key=?", owner, key);
  }

  private java.util.Optional<com.apptolast.organization.domain.SessionStart> read(
      String sql, Object... args) {
    try {
      return readOnly.execute(status -> jdbc.query(sql, MAPPER, args).stream().findFirst());
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private <T> T storage(java.util.function.Supplier<T> operation) {
    try {
      return operation.get();
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private static void requireOne(int count) {
    if (count != 1)
      throw new StorageUnavailableException(
          new IllegalStateException("Expected one persisted row"));
  }

  private static final class SessionInsertCollision extends RuntimeException {
    SessionInsertCollision(Throwable cause) {
      super(cause);
    }
  }
}
