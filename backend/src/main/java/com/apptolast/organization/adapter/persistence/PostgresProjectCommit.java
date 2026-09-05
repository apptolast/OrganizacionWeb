package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ProjectCommit;
import com.apptolast.organization.application.ProjectCreated;
import com.apptolast.organization.domain.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresProjectCommit implements ProjectCommit {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;

  public PostgresProjectCommit(
      JdbcTemplate jdbc, TransactionTemplate transaction, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.json = json;
  }

  @Override
  public void save(Project project, ProjectCreated event) {
    try {
      transaction.executeWithoutResult(
          status -> {
            jdbc.update(
                "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
                project.id(),
                project.ownerId(),
                project.name(),
                project.description(),
                project.status(),
                Timestamp.from(project.createdAt()),
                Timestamp.from(project.updatedAt()));
            jdbc.update(
                "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
                event.eventId(),
                event.aggregateId(),
                event.ownerId(),
                event.type(),
                event.schemaVersion(),
                Timestamp.from(event.occurredAt()),
                serialize(event));
          });
    } catch (RuntimeException exception) {
      if (unavailable(exception))
        throw new com.apptolast.organization.application.StorageUnavailableException(exception);
      throw exception;
    }
  }

  private boolean unavailable(Throwable error) {
    if (error == null) return false;
    if (error instanceof org.springframework.dao.DataAccessResourceFailureException
        || error instanceof org.springframework.transaction.CannotCreateTransactionException)
      return true;
    if (error instanceof java.sql.SQLException sql
        && sql.getSQLState() != null
        && sql.getSQLState().startsWith("08")) return true;
    if (error instanceof org.springframework.transaction.TransactionSystemException transactionError
        && transactionError.getApplicationException() != null
        && unavailable(transactionError.getApplicationException())) return true;
    return unavailable(error.getCause());
  }

  private String serialize(ProjectCreated event) {
    try {
      return json.writeValueAsString(event);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Event serialization failed", exception);
    }
  }
}
