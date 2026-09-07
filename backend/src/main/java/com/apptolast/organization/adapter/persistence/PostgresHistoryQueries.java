package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class PostgresHistoryQueries implements HistoryQueries {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final org.springframework.transaction.support.TransactionTemplate transaction;

  public PostgresHistoryQueries(
      JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager manager,
      ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
    transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
    transaction.setReadOnly(true);
    transaction.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public List<HistoryEntry<?>> list(String owner, HistoryFilters filters, HistoryCursor cursor) {
    try {
      return transaction.execute(status -> read(owner, filters, cursor));
    } catch (org.springframework.transaction.TransactionException
        | org.springframework.dao.DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private List<HistoryEntry<?>> read(String owner, HistoryFilters filters, HistoryCursor cursor) {
    if (filters.projectId() != null
        && !Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM projects WHERE owner_id=? AND id=?)",
                Boolean.class,
                owner,
                filters.projectId()))) throw new ResourceNotFoundException();
    if (filters.taskId() != null
        && !Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM tasks WHERE project_id=? AND id=?)",
                Boolean.class,
                filters.projectId(),
                filters.taskId()))) throw new ResourceNotFoundException();
    if (cursor != null
        && (!owner.equals(cursor.owner())
            || !filters.equals(cursor.filters())
            || compare(cursor.upper(), cursor.after()) < 0))
      throw new ValidationException(
          List.of(
              new FieldError(
                  "cursor", "INVALID_VALUE", "El cursor no corresponde a esta consulta.")));
    var sql =
        """
      WITH context AS (
        SELECT t.id task_id,t.project_id,t.title,p.name FROM tasks t
        JOIN projects p ON p.id=t.project_id WHERE p.owner_id=?
      ), facts AS (
        SELECT s.id,'SESSION_STARTED'::text type,s.started_at occurred_at,s.project_id,s.task_id,
          jsonb_build_object('id',s.id,'projectId',s.project_id,'taskId',s.task_id,'startedAt',s.started_at,
          'plannedMinutes',s.planned_minutes,'plannedEndAt',s.planned_end_at,'zoneId',s.zone_id) detail
        FROM work_sessions s JOIN context c ON (c.project_id,c.task_id)=(s.project_id,s.task_id) WHERE s.owner_id=?
        UNION ALL
        SELECT h.id,'TASK_STATUS_CHANGED',h.occurred_at,h.project_id,h.task_id,
          jsonb_build_object('id',h.id,'taskVersion',h.task_version,'fromStatus',h.from_status,'toStatus',h.to_status,'occurredAt',h.occurred_at)
        FROM task_status_history h JOIN context c ON (c.project_id,c.task_id)=(h.project_id,h.task_id)
        UNION ALL
        SELECT w.id,'SESSION_CHANGED',w.occurred_at,s.project_id,s.task_id,w.receipt
        FROM work_session_changes w JOIN work_sessions s ON s.id=w.session_id
        JOIN context c ON (c.project_id,c.task_id)=(s.project_id,s.task_id)
        WHERE w.owner_id=? AND s.owner_id=?
        UNION ALL
        SELECT b.id,'BLOCK_PLANNED',b.created_at,b.project_id,b.task_id,
          jsonb_build_object('id',b.id,'projectId',b.project_id,'taskId',b.task_id,'createdAt',b.created_at,
            'request',jsonb_build_object('objective',b.objective,'startLocal',b.start_local,'endLocal',b.end_local,
              'zoneId',b.zone_id,'startOffset',b.start_offset,'endOffset',b.end_offset,'allowOverBudget',b.allow_over_budget),
            'time',jsonb_build_object('startAt',b.start_at,'endAt',b.end_at,'startOffset',b.start_offset,'endOffset',b.end_offset,'durationMinutes',b.duration_minutes))
        FROM planned_blocks b JOIN context c ON (c.project_id,c.task_id)=(b.project_id,b.task_id)
        UNION ALL
        SELECT b.id,'BLOCK_CHANGED',b.occurred_at,b.project_id,b.task_id,b.receipt
        FROM block_changes b JOIN context c ON (c.project_id,c.task_id)=(b.project_id,b.task_id)
      ), ranked AS (SELECT facts.*,CASE type WHEN 'BLOCK_PLANNED' THEN 0 WHEN 'BLOCK_CHANGED' THEN 1 WHEN 'TASK_STATUS_CHANGED' THEN 2 WHEN 'SESSION_STARTED' THEN 3 WHEN 'SESSION_CHANGED' THEN 4 END AS source_rank FROM facts) SELECT f.*,c.name,c.title FROM ranked f JOIN context c ON (c.project_id,c.task_id)=(f.project_id,f.task_id) WHERE (?::uuid IS NULL OR f.project_id=?) AND (?::uuid IS NULL OR f.task_id=?) AND (?::text IS NULL OR (?='sessions' AND f.type IN ('SESSION_STARTED','SESSION_CHANGED')) OR (?='planning' AND f.type IN ('BLOCK_PLANNED','BLOCK_CHANGED')) OR (?='task-status' AND f.type='TASK_STATUS_CHANGED')) AND (?::date IS NULL OR (f.occurred_at AT TIME ZONE 'UTC')::date >= ?::date) AND (?::date IS NULL OR (f.occurred_at AT TIME ZONE 'UTC')::date <= ?::date) AND (?::boolean OR ((f.occurred_at,f.source_rank,f.id) <= (?::timestamptz,?::integer,?::uuid) AND (f.occurred_at,f.source_rank,f.id) < (?::timestamptz,?::integer,?::uuid))) ORDER BY f.occurred_at DESC,f.source_rank DESC,f.id DESC LIMIT 21
      """;
    return jdbc.query(
        sql,
        (row, n) -> {
          var type = row.getString("type");
          Object detail;
          Class<?> detailClass =
              switch (type) {
                case "SESSION_STARTED" -> SessionStart.class;
                case "TASK_STATUS_CHANGED" -> TaskHistoryEntry.class;
                case "BLOCK_PLANNED" -> PlannedBlock.class;
                case "BLOCK_CHANGED" -> BlockChangeReceipt.class;
                default -> WorkSessionTransitionReceipt.class;
              };
          try {
            detail = json.readValue(row.getString("detail"), detailClass);
          } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new java.sql.SQLException("Invalid history fact", error);
          }
          return new HistoryEntry<>(
              row.getObject("id", UUID.class),
              type,
              row.getTimestamp("occurred_at").toInstant(),
              row.getObject("project_id", UUID.class),
              row.getString("name"),
              row.getObject("task_id", UUID.class),
              row.getString("title"),
              detail);
        },
        owner,
        owner,
        owner,
        owner,
        filters.projectId(),
        filters.projectId(),
        filters.taskId(),
        filters.taskId(),
        filters.category(),
        filters.category(),
        filters.category(),
        filters.category(),
        filters.from(),
        filters.from(),
        filters.to(),
        filters.to(),
        cursor == null,
        cursor == null ? null : java.sql.Timestamp.from(cursor.upper().occurredAt()),
        cursor == null ? null : rank(cursor.upper().type()),
        cursor == null ? null : cursor.upper().id(),
        cursor == null ? null : java.sql.Timestamp.from(cursor.after().occurredAt()),
        cursor == null ? null : rank(cursor.after().type()),
        cursor == null ? null : cursor.after().id());
  }

  private static int rank(String type) {
    return List.of(
            "BLOCK_PLANNED",
            "BLOCK_CHANGED",
            "TASK_STATUS_CHANGED",
            "SESSION_STARTED",
            "SESSION_CHANGED")
        .indexOf(type);
  }

  private static int compare(HistoryPosition left, HistoryPosition right) {
    return java.util.Comparator.comparing(HistoryPosition::occurredAt)
        .thenComparingInt(p -> rank(p.type()))
        .thenComparing(p -> p.id().toString())
        .compare(left, right);
  }
}
