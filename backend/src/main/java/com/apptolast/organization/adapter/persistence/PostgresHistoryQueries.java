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
          jsonb_build_object('id',s.id,'projectId',s.project_id,'taskId',s.task_id,'startedAt',to_char(s.started_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
          'plannedMinutes',s.planned_minutes,'plannedEndAt',to_char(s.planned_end_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),'zoneId',s.zone_id) detail,NULL::uuid subject_id,NULL::text decision_kind,NULL::bigint decision_revision
        FROM work_sessions s JOIN context c ON (c.project_id,c.task_id)=(s.project_id,s.task_id) WHERE s.owner_id=?
        UNION ALL
        SELECT h.id,'TASK_STATUS_CHANGED',h.occurred_at,h.project_id,h.task_id,
          jsonb_build_object('id',h.id,'taskVersion',h.task_version,'fromStatus',h.from_status,'toStatus',h.to_status,'occurredAt',to_char(h.occurred_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"')),NULL::uuid,NULL::text,NULL::bigint
        FROM task_status_history h JOIN context c ON (c.project_id,c.task_id)=(h.project_id,h.task_id)
        UNION ALL
        SELECT w.id,'SESSION_CHANGED',w.occurred_at,s.project_id,s.task_id,w.receipt,s.id,w.action,w.expected_revision
        FROM work_session_changes w JOIN work_sessions s ON s.id=w.session_id
        JOIN context c ON (c.project_id,c.task_id)=(s.project_id,s.task_id)
        WHERE w.owner_id=? AND s.owner_id=?
        UNION ALL
        SELECT b.id,'BLOCK_PLANNED',b.created_at,b.project_id,b.task_id,
          jsonb_build_object('id',b.id,'projectId',b.project_id,'taskId',b.task_id,'createdAt',to_char(b.created_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),
            'request',jsonb_build_object('objective',b.objective,'startLocal',b.start_local,'endLocal',b.end_local,
              'zoneId',b.zone_id,'startOffset',b.start_offset,'endOffset',b.end_offset,'allowOverBudget',b.allow_over_budget),
            'time',jsonb_build_object('startAt',to_char(b.start_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),'endAt',to_char(b.end_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"'),'startOffset',b.start_offset,'endOffset',b.end_offset,'durationMinutes',b.duration_minutes)),NULL::uuid,NULL::text,NULL::bigint
        FROM planned_blocks b JOIN context c ON (c.project_id,c.task_id)=(b.project_id,b.task_id)
        UNION ALL
        SELECT b.id,'BLOCK_CHANGED',b.occurred_at,b.project_id,b.task_id,b.receipt,b.block_id,b.kind,b.version
        FROM block_changes b JOIN context c ON (c.project_id,c.task_id)=(b.project_id,b.task_id)
      ), ranked AS (
        SELECT facts.*,CASE type
          WHEN 'BLOCK_PLANNED' THEN 0 WHEN 'BLOCK_CHANGED' THEN 1
          WHEN 'TASK_STATUS_CHANGED' THEN 2 WHEN 'SESSION_STARTED' THEN 3
          WHEN 'SESSION_CHANGED' THEN 4 END AS source_rank
        FROM facts
      )
      SELECT f.*,c.name,c.title FROM ranked f
      JOIN context c ON (c.project_id,c.task_id)=(f.project_id,f.task_id)
      WHERE (?::uuid IS NULL OR f.project_id=?) AND (?::uuid IS NULL OR f.task_id=?)
        AND (?::text IS NULL
          OR (?='sessions' AND f.type IN ('SESSION_STARTED','SESSION_CHANGED'))
          OR (?='planning' AND f.type IN ('BLOCK_PLANNED','BLOCK_CHANGED'))
          OR (?='task-status' AND f.type='TASK_STATUS_CHANGED'))
        AND (?::date IS NULL OR (f.occurred_at AT TIME ZONE 'UTC')::date >= ?::date)
        AND (?::date IS NULL OR (f.occurred_at AT TIME ZONE 'UTC')::date <= ?::date)
        AND (?::boolean OR (
          (f.occurred_at,f.source_rank,f.id) <= (?::timestamptz,?::integer,?::uuid)
          AND (f.occurred_at,f.source_rank,f.id) < (?::timestamptz,?::integer,?::uuid)))
      ORDER BY f.occurred_at DESC,f.source_rank DESC,f.id DESC LIMIT 21
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
          if (detail == null) throw new java.sql.SQLException("Invalid history fact");
          if (detail instanceof BlockChangeReceipt receipt
              && (!sameFact(row, receipt.id(), receipt.occurredAt())
                  || !blockContext(
                      receipt.before(),
                      receipt.blockId(),
                      row.getObject("project_id", UUID.class),
                      row.getObject("task_id", UUID.class))
                  || (receipt.after() != null
                      && !blockContext(
                          receipt.after(),
                          receipt.blockId(),
                          row.getObject("project_id", UUID.class),
                          row.getObject("task_id", UUID.class)))))
            throw new java.sql.SQLException("Invalid stored history receipt identity");
          if (detail instanceof WorkSessionTransitionReceipt receipt
              && (!sameFact(row, receipt.id(), receipt.occurredAt())
                  || !sessionContext(
                      receipt.before(),
                      receipt.sessionId(),
                      row.getObject("project_id", UUID.class),
                      row.getObject("task_id", UUID.class))
                  || !sessionContext(
                      receipt.after(),
                      receipt.sessionId(),
                      row.getObject("project_id", UUID.class),
                      row.getObject("task_id", UUID.class))))
            throw new java.sql.SQLException("Invalid stored session context");
          if (detail instanceof WorkSessionTransitionReceipt receipt && !validTransition(receipt))
            throw new java.sql.SQLException("Invalid stored session transition");
          if (detail instanceof BlockChangeReceipt receipt
              && (!sourceMatches(row, receipt.blockId(), receipt.kind(), receipt.version())
                  || ("CANCELLED".equals(receipt.kind())
                      ? receipt.after() != null
                      : receipt.after() == null)))
            throw new java.sql.SQLException("Receipt differs from durable source");
          if (detail instanceof WorkSessionTransitionReceipt receipt
              && !sourceMatches(
                  row, receipt.sessionId(), receipt.action(), receipt.before().revision()))
            throw new java.sql.SQLException("Receipt differs from durable source");
          return new HistoryEntry<>(
              row.getObject("id", UUID.class),
              type,
              row.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant(),
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
        cursor == null ? null : cursor.upper().occurredAt().atOffset(java.time.ZoneOffset.UTC),
        cursor == null ? null : rank(cursor.upper().type()),
        cursor == null ? null : cursor.upper().id(),
        cursor == null ? null : cursor.after().occurredAt().atOffset(java.time.ZoneOffset.UTC),
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

  private static boolean blockContext(PlannedBlock block, UUID id, UUID project, UUID task) {
    return block != null
        && id != null
        && id.equals(block.id())
        && project.equals(block.projectId())
        && task.equals(block.taskId());
  }

  private static boolean sessionContext(
      WorkSessionState state, UUID session, UUID project, UUID task) {
    return state != null
        && state.session() != null
        && session != null
        && session.equals(state.session().id())
        && project.equals(state.session().projectId())
        && task.equals(state.session().taskId());
  }

  private static boolean sameFact(java.sql.ResultSet row, UUID id, java.time.Instant occurredAt)
      throws java.sql.SQLException {
    return row.getObject("id", UUID.class).equals(id)
        && row.getObject("occurred_at", java.time.OffsetDateTime.class)
            .toInstant()
            .equals(occurredAt);
  }

  private static boolean validTransition(WorkSessionTransitionReceipt receipt)
      throws java.sql.SQLException {
    try {
      if (!validBefore(receipt.before())) return false;
      if ("EXTEND".equals(receipt.action())) {
        var before = receipt.before();
        before.requireClose(before.revision());
        before.requireTime(receipt.occurredAt());
        var extension = receipt.extension();
        if (extension == null
            || receipt.closure() != null
            || extension.additionalMinutes() < 1
            || extension.additionalMinutes() > 1440
            || extension.previousEndAt().isBefore(before.session().plannedEndAt())) return false;
        var base =
            receipt.occurredAt().isAfter(extension.previousEndAt())
                ? receipt.occurredAt()
                : extension.previousEndAt();
        var end = base.plus(extension.additionalMinutes(), java.time.temporal.ChronoUnit.MINUTES);
        before.requireTime(end);
        return extension.effectiveEndAt().equals(end)
            && receipt
                .after()
                .equals(
                    new WorkSessionState(
                        before.session(),
                        before.status(),
                        before.revision() + 1,
                        before.changedAt(),
                        before.workedMicroseconds(),
                        before.runningSince()));
      }
      if ("CLOSE".equals(receipt.action())) {
        var before = receipt.before();
        before.requireClose(before.revision());
        before.requireTime(receipt.occurredAt());
        var worked =
            before.workedMicroseconds()
                + (before.status().equals("running")
                    ? java.time.temporal.ChronoUnit.MICROS.between(
                        before.runningSince(), receipt.occurredAt())
                    : 0);
        return validClosure(receipt.closure())
            && receipt.extension() == null
            && receipt
                .after()
                .equals(
                    new WorkSessionState(
                        before.session(),
                        "closed",
                        before.revision() + 1,
                        receipt.occurredAt(),
                        worked,
                        null));
      }
      if (!"PAUSE".equals(receipt.action()) && !"RESUME".equals(receipt.action())) return false;
      var pause = "PAUSE".equals(receipt.action());
      var before = receipt.before();
      before.requireTransition(before.revision(), pause);
      before.requireTime(receipt.occurredAt());
      var worked =
          before.workedMicroseconds()
              + (pause
                  ? java.time.temporal.ChronoUnit.MICROS.between(
                      before.runningSince(), receipt.occurredAt())
                  : 0);
      return receipt.closure() == null
          && receipt.extension() == null
          && receipt
              .after()
              .equals(
                  new WorkSessionState(
                      before.session(),
                      pause ? "paused" : "running",
                      before.revision() + 1,
                      receipt.occurredAt(),
                      worked,
                      pause ? null : receipt.occurredAt()));
    } catch (RuntimeException invalid) {
      throw new java.sql.SQLException("Invalid stored session transition", invalid);
    }
  }

  private static boolean sourceMatches(
      java.sql.ResultSet row, UUID subject, String kind, long revision)
      throws java.sql.SQLException {
    return row.getObject("subject_id", UUID.class).equals(subject)
        && row.getString("decision_kind").equals(kind)
        && row.getLong("decision_revision") == revision;
  }

  private static boolean validBefore(WorkSessionState state) {
    var start = state.session();
    return state.revision() > 0
        && instantInRange(start.startedAt())
        && instantInRange(start.plannedEndAt())
        && instantInRange(state.changedAt())
        && start.plannedMinutes() >= 1
        && start.plannedMinutes() <= 1440
        && start
            .plannedEndAt()
            .equals(
                start
                    .startedAt()
                    .plus(start.plannedMinutes(), java.time.temporal.ChronoUnit.MINUTES))
        && start.zoneId() != null
        && !start.zoneId().isBlank()
        && !state.changedAt().isBefore(start.startedAt())
        && state.workedMicroseconds() >= 0
        && state.workedMicroseconds()
            <= java.time.temporal.ChronoUnit.MICROS.between(start.startedAt(), state.changedAt())
        && ("running".equals(state.status())
            ? state.changedAt().equals(state.runningSince())
            : "paused".equals(state.status()) && state.runningSince() == null);
  }

  private static boolean instantInRange(java.time.Instant value) {
    return value != null
        && value.getNano() % 1000 == 0
        && !value.isBefore(java.time.Instant.parse("0001-01-01T00:00:00Z"))
        && value.isBefore(java.time.Instant.parse("+10000-01-01T00:00:00Z"));
  }

  private static boolean validClosure(WorkSessionClosure closure) {
    if (closure == null
        || closure.workDate() == null
        || closure.workDate().getYear() < 1
        || closure.workDate().getYear() > 9999
        || closure.closeZoneId() == null
        || closure.closeZoneId().isBlank()
        || closure.progressNote() == null
        || closure.nextStep() == null) return false;
    new WorkSessionCloseNotes(closure.progressNote(), closure.nextStep());
    return true;
  }
}
