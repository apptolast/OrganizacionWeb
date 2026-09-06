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

public final class PostgresWorkSessionStore implements WorkSessionStarting {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;

  public PostgresWorkSessionStore(
      JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(manager);
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
    return transaction.execute(
        status -> {
          var projectStatus =
              jdbc.queryForObject(
                  "SELECT status FROM projects WHERE owner_id=? AND id=? FOR SHARE",
                  String.class,
                  owner,
                  project);
          var taskStatus =
              jdbc.queryForObject(
                  "SELECT status FROM tasks WHERE project_id=? AND id=? FOR SHARE",
                  String.class,
                  project,
                  task);
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
              session.zoneId());
          var event = change.event();
          try {
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
                json.writeValueAsString(event));
          } catch (JsonProcessingException error) {
            throw new IllegalStateException(error);
          }
          return new WorkSessionConfirmation(session, false);
        });
  }
}
