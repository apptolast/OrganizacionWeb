package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportedTaskCommit;
import com.apptolast.organization.application.ProjectCompletedException;
import com.apptolast.organization.application.ResourceNotFoundException;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.application.TaskCreation;
import com.apptolast.organization.application.TaskCreationEvent;
import com.apptolast.organization.domain.ExternalIssue;
import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.ValidationException;
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

/**
 * Confirma la tarea, su evento de la outbox y el enlace con la issue en la misma transacción. El
 * enlace es lo que hace que reimportar sea seguro, así que se escribe con los otros dos o no se
 * escribe ninguno.
 */
@Component
public final class PostgresImportedTaskCommit implements ImportedTaskCommit {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;

  public PostgresImportedTaskCommit(
      JdbcTemplate jdbc, TransactionTemplate transaction, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.json = json;
  }

  @Override
  public boolean save(
      String ownerId,
      UUID projectId,
      String source,
      ExternalIssue issue,
      Function<String, TaskCreation> operation) {
    try {
      return Boolean.TRUE.equals(
          transaction.execute(
              status -> {
                // El bloqueo de la fila del proyecto serializa la importación con los cambios de
                // estado del proyecto, igual que hace la creación normal de tareas.
                var states =
                    jdbc.queryForList(
                        "SELECT status FROM projects WHERE owner_id=? AND id=? FOR UPDATE",
                        String.class,
                        ownerId,
                        projectId);
                if (states.isEmpty()) throw new ResourceNotFoundException();
                if (alreadyLinked(ownerId, source, issue.externalId())) return false;
                var creation = operation.apply(states.getFirst());
                insertTask(projectId, creation.task());
                insertEvent(ownerId, projectId, creation.event());
                insertLink(ownerId, source, issue, creation.task());
                return true;
              }));
    } catch (ResourceNotFoundException | ProjectCompletedException | ValidationException error) {
      // Decisiones de negocio: la transacción ya revirtió, pero el motivo no es del almacén.
      throw error;
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private boolean alreadyLinked(String ownerId, String source, String externalId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM task_external_links"
                + " WHERE owner_id=? AND source=? AND external_id=?)",
            Boolean.class,
            ownerId,
            source,
            externalId));
  }

  private void insertTask(UUID projectId, Task task) {
    expectOneRow(
        jdbc.update(
            "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,"
                + "created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",
            task.id(),
            projectId,
            task.title(),
            task.completionCriterion(),
            task.estimatedMinutes(),
            task.status(),
            Timestamp.from(task.createdAt()),
            Timestamp.from(task.updatedAt())),
        "Task");
  }

  private void insertEvent(String ownerId, UUID projectId, TaskCreationEvent event) {
    expectOneRow(
        jdbc.update(
            "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,"
                + "occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
            event.eventId(),
            projectId,
            ownerId,
            event.type(),
            event.schemaVersion(),
            Timestamp.from(event.occurredAt()),
            serialize(event)),
        "Event");
  }

  private void insertLink(String ownerId, String source, ExternalIssue issue, Task task) {
    expectOneRow(
        jdbc.update(
            "INSERT INTO task_external_links(owner_id,source,external_id,task_id,url,linked_at)"
                + " VALUES (?,?,?,?,?,?)",
            ownerId,
            source,
            issue.externalId(),
            task.id(),
            issue.url(),
            Timestamp.from(task.createdAt())),
        "Link");
  }

  private static void expectOneRow(int affected, String what) {
    if (affected != 1)
      throw new StorageUnavailableException(
          new IllegalStateException(what + " write did not affect one row"));
  }

  private String serialize(TaskCreationEvent event) {
    try {
      return json.writeValueAsString(event);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Event serialization failed", error);
    }
  }
}
