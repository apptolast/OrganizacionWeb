package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.function.BiFunction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresProjectStatusEditing implements ProjectStatusEditing {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final PostgresProjectQueries queries;
  private final ObjectMapper json;

  public PostgresProjectStatusEditing(
      JdbcTemplate jdbc,
      TransactionTemplate transaction,
      PostgresProjectQueries queries,
      ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.queries = queries;
    this.json = json;
  }

  public ProjectSnapshot update(
      String ownerId, UUID id, BiFunction<ProjectSnapshot, Long, ProjectStatusChange> operation) {
    try {
      return transaction.execute(
          status -> {
            // ponytail: one global transaction lock serializes brief state changes; split by owner
            // only if measured contention warrants it.
            // READ_COMMITTED (the deployment default) lets the later count observe the preceding
            // lock holder's commit.
            jdbc.execute("SELECT pg_advisory_xact_lock(749189081)");
            var previous =
                queries.findForUpdate(ownerId, id).orElseThrow(ProjectNotFoundException::new);
            var change =
                operation.apply(
                    previous,
                    jdbc.queryForObject(
                        "SELECT count(*) FROM projects WHERE owner_id=? AND status='active'",
                        Long.class,
                        ownerId));
            if (change.event() == null) return previous;
            var project = change.snapshot().project();
            int updated =
                jdbc.update(
                    "UPDATE projects SET status=?,updated_at=?,version=? WHERE owner_id=? AND id=? AND version=?",
                    project.status(),
                    java.sql.Timestamp.from(project.updatedAt()),
                    change.snapshot().version(),
                    ownerId,
                    id,
                    previous.version());
            if (updated != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Project write did not affect one row"));
            var event = change.event();
            int inserted =
                jdbc.update(
                    "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
                    event.eventId(),
                    event.aggregateId(),
                    event.ownerId(),
                    event.type(),
                    event.schemaVersion(),
                    java.sql.Timestamp.from(event.occurredAt()),
                    serialize(event));
            if (inserted != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Event write did not affect one row"));
            return change.snapshot();
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private String serialize(ProjectStatusChanged event) {
    try {
      return json.writeValueAsString(event);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("Event serialization failed", error);
    }
  }
}
