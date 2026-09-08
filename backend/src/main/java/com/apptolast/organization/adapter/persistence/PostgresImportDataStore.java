package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportDataQueries;
import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.application.ImportPreview;
import com.apptolast.organization.application.StorageUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresImportDataStore
    implements ImportDataQueries,
        com.apptolast.organization.application.ImportDataCommands,
        com.apptolast.organization.application.ImportReceiptQueries {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate reading;
  private final TransactionTemplate writing;
  private final int maxActiveProjects;

  public PostgresImportDataStore(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
    this(jdbc, transactions, 3);
  }

  public PostgresImportDataStore(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, int maxActiveProjects) {
    if (maxActiveProjects < 1 || maxActiveProjects > 10)
      throw new IllegalArgumentException("Active project quota must be between 1 and 10");
    this.maxActiveProjects = maxActiveProjects;
    this.jdbc = jdbc;
    reading = new TransactionTemplate(transactions);
    reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    writing = new TransactionTemplate(transactions);
    writing.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public ImportPreview preview(String owner, InputStream body) {
    return stored(
        () ->
            reading.execute(
                status -> {
                  jdbc.queryForObject("SELECT pg_current_snapshot()::text", String.class);
                  try {
                    var header = stage(body);
                    if (!owner.equals(header.owner())) throw new ImportInvalidFileException();
                    validate(owner);
                    prepareReceipts();
                    validateHistoricalRelations();
                    var identical = compare(owner);
                    return new ImportPreview(
                        header.fileSha256(),
                        header.byteLength(),
                        header.owner(),
                        header.exportedAt(),
                        header.counts(),
                        header.counts().minus(identical),
                        identical,
                        newRunningSessions());
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                }));
  }

  private ImportJsonReader.Header stage(InputStream body) throws IOException {
    return stage(body, null);
  }

  private List<ImportPreview.RunningSession> newRunningSessions() {
    return jdbc.query(
        """
        SELECT (s.payload->>'id')::uuid AS id,(s.payload->>'runningSince')::timestamptz AS running_since
        FROM import_stage s LEFT JOIN work_sessions r ON r.id::text=s.payload->>'id'
        WHERE s.collection='workSessions' AND s.payload->>'status'='running' AND r.id IS NULL
        ORDER BY s.payload->>'id'
        """,
        (row, index) -> {
          var since = row.getObject("running_since", java.time.OffsetDateTime.class);
          return new ImportPreview.RunningSession(
              row.getObject("id", java.util.UUID.class), since == null ? null : since.toInstant());
        });
  }

  private ImportJsonReader.Header stage(InputStream body, String expectedSha) throws IOException {
    var json =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .enable(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();
    jdbc.execute(
        "CREATE TEMP TABLE import_stage(collection TEXT NOT NULL,raw TEXT NOT NULL,payload JSONB,durable_receipt JSONB,existing_receipt JSONB) ON COMMIT DROP");
    var collections = new java.util.ArrayList<String>();
    var records = new java.util.ArrayList<String>();
    int[] pendingBytes = {0};
    ImportJsonReader.RecordConsumer consumer =
        (collection, row) -> {
          var raw = json.writeValueAsString(row);
          // ASCII escaping makes string length the encoded byte count. One oversized row is flushed
          // alone.
          if (!records.isEmpty()
              && (records.size() == 128 || pendingBytes[0] + (long) raw.length() > 65536)) {
            flushStage(collections, records);
            pendingBytes[0] = 0;
          }
          collections.add(collection);
          records.add(raw);
          pendingBytes[0] += raw.length();
          if (pendingBytes[0] >= 65536) {
            flushStage(collections, records);
            pendingBytes[0] = 0;
          }
        };
    var reader = new ImportJsonReader();
    var header =
        expectedSha == null
            ? reader.read(body, consumer)
            : reader.read(body, consumer, expectedSha);
    flushStage(collections, records);
    return header;
  }

  private void flushStage(java.util.List<String> collections, java.util.List<String> records) {
    if (records.isEmpty()) return;
    jdbc.execute(
        (org.springframework.jdbc.core.ConnectionCallback<Void>)
            connection -> {
              var names = connection.createArrayOf("text", collections.toArray(String[]::new));
              var bodies = connection.createArrayOf("text", records.toArray(String[]::new));
              try (var statement =
                  connection.prepareStatement(
                      "INSERT INTO import_stage(collection,raw) SELECT * FROM unnest(?::text[],?::text[])")) {
                statement.setArray(1, names);
                statement.setArray(2, bodies);
                if (statement.executeUpdate() != records.size())
                  throw new java.sql.SQLException("Staging rows were not persisted");
              } finally {
                names.free();
                bodies.free();
              }
              return null;
            });
    collections.clear();
    records.clear();
  }

  private com.apptolast.organization.application.ImportCounts compare(String owner) {
    long active =
        jdbc.queryForObject(
            """
        SELECT (SELECT count(*) FROM projects WHERE owner_id=? AND status='active') +
          (SELECT count(*) FROM import_stage s WHERE s.collection='projects' AND s.payload->>'status'='active'
            AND NOT EXISTS(SELECT 1 FROM projects p WHERE p.id::text=s.payload->>'id'))
        """,
            Long.class,
            owner);
    if (active > maxActiveProjects)
      throw new com.apptolast.organization.application.ImportConflictException();
    long open =
        jdbc.queryForObject(
            """
        SELECT (SELECT count(*) FROM work_sessions WHERE owner_id=? AND status IN ('running','paused')) +
          (SELECT count(*) FROM import_stage s WHERE s.collection='workSessions' AND s.payload->>'status' IN ('running','paused')
            AND NOT EXISTS(SELECT 1 FROM work_sessions w WHERE w.id::text=s.payload->>'id'))
        """,
            Long.class,
            owner);
    if (open > 1) throw new com.apptolast.organization.application.ImportConflictException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(
          SELECT 1 FROM import_stage s JOIN availability_preferences r ON r.owner_id=?
            WHERE s.collection='availability' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN appearance_preferences r ON r.owner_id=?
            WHERE s.collection='appearance' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN customization_preferences r
            ON r.owner_id=? AND r.scope=s.payload->>'scope'
            WHERE s.collection='customization' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN project_custom_field_values r
            ON r.owner_id=? AND r.project_id::text=s.payload->>'projectId'
            WHERE s.collection='projectCustomFieldValues' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN task_custom_field_values r
            ON r.owner_id=? AND r.task_id::text=s.payload->>'taskId'
            WHERE s.collection='taskCustomFieldValues' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN planned_blocks r
            ON r.task_id::text=s.payload->>'taskId' AND r.request_key::text=s.payload->>'requestKey'
            WHERE s.collection='plannedBlocks' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN block_changes r
            ON (r.task_id::text=s.payload->>'taskId' AND r.request_key::text=s.payload->>'requestKey')
              OR (r.block_id::text=s.payload->>'blockId' AND r.version::text=s.payload->>'version')
            WHERE s.collection='blockChanges' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN task_status_history r
            ON r.task_id::text=s.payload->>'taskId' AND r.task_version::text=s.payload->>'taskVersion'
            WHERE s.collection='taskStatusHistory' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN work_sessions r
            ON r.owner_id=? AND r.request_key::text=s.payload->>'requestKey'
            WHERE s.collection='workSessions' AND r.id::text<>s.payload->>'id'
          UNION ALL SELECT 1 FROM import_stage s JOIN work_session_changes r ON r.owner_id=? AND (
            r.request_key::text=s.payload->>'requestKey' OR
            r.action='CLOSE' AND s.payload->>'action'='CLOSE' AND r.session_id::text=s.payload->>'sessionId')
            WHERE s.collection='workSessionChanges' AND r.id::text<>s.payload->>'id')
        """,
        Boolean.class,
        owner,
        owner,
        owner,
        owner,
        owner,
        owner,
        owner)) throw new com.apptolast.organization.application.ImportConflictException();
    existingReceipts(owner);
    long projects =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN projects p ON p.id::text=s.payload->>'id'
        WHERE s.collection='projects' AND p.owner_id=?
          AND p.name=s.payload->>'name'
          AND p.description IS NOT DISTINCT FROM s.payload->>'description'
          AND p.status=s.payload->>'status'
          AND p.version::text=s.payload->>'version'
          AND p.created_at=(s.payload->>'createdAt')::timestamptz
          AND p.updated_at=(s.payload->>'updatedAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedProjects =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN projects p ON p.id::text=s.payload->>'id' WHERE s.collection='projects'",
            Long.class);
    if (occupiedProjects != projects)
      throw new com.apptolast.organization.application.ImportConflictException();
    long tasks =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN tasks t ON t.id::text=s.payload->>'id'
          JOIN projects p ON p.id=t.project_id
        WHERE s.collection='tasks' AND p.owner_id=?
          AND t.project_id::text=s.payload->>'projectId'
          AND t.parent_id::text IS NOT DISTINCT FROM s.payload->>'parentId'
          AND t.title=s.payload->>'title'
          AND t.completion_criterion=s.payload->>'completionCriterion'
                    AND t.estimated_minutes IS NOT DISTINCT FROM (s.payload->>'estimatedMinutes')::numeric::integer
          AND t.status=s.payload->>'status' AND t.version::text=s.payload->>'version'
          AND t.completed_at IS NOT DISTINCT FROM (s.payload->>'completedAt')::timestamptz
          AND t.created_at=(s.payload->>'createdAt')::timestamptz
          AND t.updated_at=(s.payload->>'updatedAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedTasks =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN tasks t ON t.id::text=s.payload->>'id' WHERE s.collection='tasks'",
            Long.class);
    if (occupiedTasks != tasks)
      throw new com.apptolast.organization.application.ImportConflictException();
    long history =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN task_status_history h ON h.id::text=s.payload->>'id'
          JOIN projects p ON p.id=h.project_id
        WHERE s.collection='taskStatusHistory' AND p.owner_id=?
          AND h.project_id::text=s.payload->>'projectId' AND h.task_id::text=s.payload->>'taskId'
          AND h.task_version::text=s.payload->>'taskVersion'
          AND h.from_status=s.payload->>'fromStatus' AND h.to_status=s.payload->>'toStatus'
          AND h.occurred_at=(s.payload->>'occurredAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedHistory =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN task_status_history h ON h.id::text=s.payload->>'id' WHERE s.collection='taskStatusHistory'",
            Long.class);
    if (occupiedHistory != history)
      throw new com.apptolast.organization.application.ImportConflictException();
    long availability =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN availability_preferences a ON a.id::text=s.payload->>'id'
        WHERE s.collection='availability' AND a.owner_id=? AND a.zone_id=s.payload->>'zoneId'
          AND a.monday_minutes=(s.payload->>'mondayMinutes')::integer
          AND a.tuesday_minutes=(s.payload->>'tuesdayMinutes')::integer
          AND a.wednesday_minutes=(s.payload->>'wednesdayMinutes')::integer
          AND a.thursday_minutes=(s.payload->>'thursdayMinutes')::integer
          AND a.friday_minutes=(s.payload->>'fridayMinutes')::integer
          AND a.saturday_minutes=(s.payload->>'saturdayMinutes')::integer
          AND a.sunday_minutes=(s.payload->>'sundayMinutes')::integer
          AND a.version::text=s.payload->>'version'
          AND a.created_at=(s.payload->>'createdAt')::timestamptz
          AND a.updated_at=(s.payload->>'updatedAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedAvailability =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN availability_preferences a ON a.id::text=s.payload->>'id' WHERE s.collection='availability'",
            Long.class);
    if (occupiedAvailability != availability)
      throw new com.apptolast.organization.application.ImportConflictException();
    long blocks =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE p.owner_id=? AND
          ROW(r.project_id,r.task_id,r.request_key,r.objective,r.start_local,r.end_local,r.zone_id,r.start_offset,r.end_offset,r.allow_over_budget,r.start_at,r.end_at,r.duration_minutes,r.created_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'projectId')::uuid,
              (s.payload->>'taskId')::uuid,
              (s.payload->>'requestKey')::uuid,
              (s.payload->>'objective'),
              (s.payload->>'startLocal')::timestamp,
              (s.payload->>'endLocal')::timestamp,
              (s.payload->>'zoneId'),
              (s.payload->>'startOffset'),
              (s.payload->>'endOffset'),
              (s.payload->>'allowOverBudget')::boolean,
              (s.payload->>'startAt')::timestamptz,
              (s.payload->>'endAt')::timestamptz,
              (s.payload->>'durationMinutes')::numeric::integer,
              (s.payload->>'createdAt')::timestamptz)) AS identical
        FROM import_stage s JOIN planned_blocks r ON r.id::text=s.payload->>'id'
          JOIN projects p ON p.id=r.project_id
        WHERE s.collection='plannedBlocks'
        """,
            owner);
    long projections =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE p.owner_id=? AND
          ROW(r.version,r.status,r.updated_at,r.start_local,r.end_local,r.zone_id,r.start_offset,r.end_offset,r.start_at,r.end_at,r.duration_minutes)
          IS NOT DISTINCT FROM ROW((s.payload->>'version')::bigint,
              (s.payload->>'status'),
              (s.payload->>'updatedAt')::timestamptz,
              (s.payload->>'startLocal')::timestamp,
              (s.payload->>'endLocal')::timestamp,
              (s.payload->>'zoneId'),
              (s.payload->>'startOffset'),
              (s.payload->>'endOffset'),
              (s.payload->>'startAt')::timestamptz,
              (s.payload->>'endAt')::timestamptz,
              (s.payload->>'durationMinutes')::numeric::integer)) AS identical
        FROM import_stage s JOIN block_projections r ON r.block_id::text=s.payload->>'blockId'
          JOIN planned_blocks b ON b.id=r.block_id JOIN projects p ON p.id=b.project_id
        WHERE s.collection='blockProjections'
        """,
            owner);
    long sessions =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.project_id,r.task_id,r.request_key,r.started_at,r.planned_minutes,r.planned_end_at,r.zone_id,r.status,r.revision,r.changed_at,r.worked_microseconds,r.running_since,r.effective_end_at,r.last_decision_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'projectId')::uuid,
              (s.payload->>'taskId')::uuid,
              (s.payload->>'requestKey')::uuid,
              (s.payload->>'startedAt')::timestamptz,
              (s.payload->>'plannedMinutes')::numeric::integer,
              (s.payload->>'plannedEndAt')::timestamptz,
              (s.payload->>'zoneId'),
              (s.payload->>'status'),
              (s.payload->>'revision')::bigint,
              (s.payload->>'changedAt')::timestamptz,
              (s.payload->>'workedMicroseconds')::bigint,
              (s.payload->>'runningSince')::timestamptz,
              (s.payload->>'effectiveEndAt')::timestamptz,
              (s.payload->>'lastDecisionAt')::timestamptz)) AS identical
        FROM import_stage s JOIN work_sessions r ON r.id::text=s.payload->>'id'

        WHERE s.collection='workSessions'
        """,
            owner);
    long intervals =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE w.owner_id=? AND
          ROW(r.revision,r.start_at,r.end_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'revision')::bigint,
              (s.payload->>'startAt')::timestamptz,
              (s.payload->>'endAt')::timestamptz)) AS identical
        FROM import_stage s JOIN work_session_intervals r ON r.session_id::text=s.payload->>'sessionId' AND r.revision=(s.payload->>'revision')::bigint
          JOIN work_sessions w ON w.id=r.session_id
        WHERE s.collection='workSessionIntervals'
        """,
            owner);
    long appearance =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.theme,r.accent_light,r.accent_dark,r.version,r.updated_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'theme'),
              (s.payload->>'accentLight'),
              (s.payload->>'accentDark'),
              (s.payload->>'version')::bigint,
              (s.payload->>'updatedAt')::timestamptz)) AS identical
        FROM import_stage s JOIN appearance_preferences r ON r.id::text=s.payload->>'id'

        WHERE s.collection='appearance'
        """,
            owner);
    long customization =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.scope,r.visible_fields,r.custom_fields,r.version,r.updated_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'scope'),
              s.payload->'visibleFields',
              s.payload->'customFields',
              (s.payload->>'version')::bigint,
              (s.payload->>'updatedAt')::timestamptz)) AS identical
        FROM import_stage s JOIN customization_preferences r ON r.id::text=s.payload->>'id'

        WHERE s.collection='customization'
        """,
            owner);
    long projectValues =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.project_id,r.field_values,r.version,r.updated_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'projectId')::uuid,
              coalesce((SELECT jsonb_object_agg(v->>'fieldId',v->'value') FROM jsonb_array_elements(s.payload->'values') v),'{}'::jsonb),
              (s.payload->>'version')::bigint,
              (s.payload->>'updatedAt')::timestamptz)) AS identical
        FROM import_stage s JOIN project_custom_field_values r ON r.id::text=s.payload->>'id'

        WHERE s.collection='projectCustomFieldValues'
        """,
            owner);
    long taskValues =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.task_id,t.project_id,r.field_values,r.version,r.updated_at)
          IS NOT DISTINCT FROM ROW((s.payload->>'taskId')::uuid,
              (s.payload->>'projectId')::uuid,
              coalesce((SELECT jsonb_object_agg(v->>'fieldId',v->'value') FROM jsonb_array_elements(s.payload->'values') v),'{}'::jsonb),
              (s.payload->>'version')::bigint,
              (s.payload->>'updatedAt')::timestamptz)) AS identical
        FROM import_stage s JOIN task_custom_field_values r ON r.id::text=s.payload->>'id'
          JOIN tasks t ON t.id=r.task_id
        WHERE s.collection='taskCustomFieldValues'
        """,
            owner);
    long blockChanges =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE p.owner_id=? AND
          ROW(r.project_id,r.task_id,r.block_id,r.request_key,r.kind,r.version,r.occurred_at,s.existing_receipt)
          IS NOT DISTINCT FROM ROW((s.payload->>'projectId')::uuid,
              (s.payload->>'taskId')::uuid,
              (s.payload->>'blockId')::uuid,
              (s.payload->>'requestKey')::uuid,
              (s.payload->>'kind'),
              (s.payload->>'version')::bigint,
              (s.payload->>'occurredAt')::timestamptz,
              s.payload->'receipt')) AS identical
        FROM import_stage s JOIN block_changes r ON r.id::text=s.payload->>'id'
          JOIN projects p ON p.id=r.project_id
        WHERE s.collection='blockChanges'
        """,
            owner);
    long sessionChanges =
        matching(
            """
        SELECT count(*) AS occupied,count(*) FILTER(WHERE r.owner_id=? AND
          ROW(r.session_id,r.request_key,r.action,r.expected_revision,r.occurred_at,s.existing_receipt)
          IS NOT DISTINCT FROM ROW((s.payload->>'sessionId')::uuid,
              (s.payload->>'requestKey')::uuid,
              (s.payload->>'action'),
              (s.payload->>'expectedRevision')::bigint,
              (s.payload->>'occurredAt')::timestamptz,
              s.payload->'receipt')) AS identical
        FROM import_stage s JOIN work_session_changes r ON r.id::text=s.payload->>'id'

        WHERE s.collection='workSessionChanges'
        """,
            owner);
    return new com.apptolast.organization.application.ImportCounts(
        projects,
        tasks,
        history,
        availability,
        blocks,
        projections,
        blockChanges,
        sessions,
        intervals,
        sessionChanges,
        appearance,
        customization,
        projectValues,
        taskValues);
  }

  private void existingReceipts(String owner) {
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage s JOIN block_changes c ON c.id::text=s.payload->>'id'
          JOIN projects p ON p.id=c.project_id WHERE s.collection='blockChanges' AND p.owner_id=?
            AND octet_length(c.receipt::text)::bigint+octet_length(c.kind)>33554432)
        OR EXISTS(SELECT 1 FROM import_stage s JOIN work_session_changes c ON c.id::text=s.payload->>'id'
          JOIN work_sessions w ON w.id=c.session_id WHERE s.collection='workSessionChanges' AND c.owner_id=?
            AND octet_length(c.receipt::text)::bigint+octet_length(c.action)+octet_length(w.zone_id)>33554432)
        """,
        Boolean.class,
        owner,
        owner))
      throw new StorageUnavailableException(
          new IllegalArgumentException("Stored receipt exceeds the bounded reader budget"));
    var queries =
        List.of(
            """
        SELECT s.ctid::text AS stage_id,c.id,c.project_id,c.task_id,c.block_id,c.kind,c.version,c.occurred_at,c.receipt
        FROM import_stage s JOIN block_changes c ON c.id::text=s.payload->>'id'
          JOIN projects p ON p.id=c.project_id
        WHERE s.collection='blockChanges' AND p.owner_id=?
        """,
            """
        SELECT s.ctid::text AS stage_id,c.id,c.session_id,c.action,c.expected_revision,c.occurred_at,c.receipt,
          w.project_id,w.task_id,w.started_at,w.planned_minutes,w.planned_end_at,w.zone_id
        FROM import_stage s JOIN work_session_changes c ON c.id::text=s.payload->>'id'
          JOIN work_sessions w ON w.id=c.session_id
        WHERE s.collection='workSessionChanges' AND c.owner_id=?
        """);
    for (int index = 0; index < queries.size(); index++) {
      boolean block = index == 0;
      var sql = queries.get(index);
      jdbc.query(
          connection -> {
            var statement = connection.prepareStatement(sql);
            statement.setString(1, owner);
            statement.setFetchSize(1);
            return statement;
          },
          (org.springframework.jdbc.core.ResultSetExtractor<Void>)
              rows -> {
                var json = new com.fasterxml.jackson.databind.ObjectMapper();
                var writer = new ExportReceiptWriter(json);
                while (rows.next()) {
                  try (var buffer =
                      new com.fasterxml.jackson.databind.util.TokenBuffer(json, false)) {
                    var id = rows.getObject("id", java.util.UUID.class);
                    var at =
                        rows.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant();
                    if (block) {
                      writer.block(
                          buffer,
                          rows.getString("receipt"),
                          id,
                          rows.getObject("block_id", java.util.UUID.class),
                          rows.getObject("project_id", java.util.UUID.class),
                          rows.getObject("task_id", java.util.UUID.class),
                          rows.getString("kind"),
                          rows.getLong("version"),
                          at);
                    } else {
                      var original =
                          new com.apptolast.organization.domain.SessionStart(
                              rows.getObject("session_id", java.util.UUID.class),
                              rows.getObject("project_id", java.util.UUID.class),
                              rows.getObject("task_id", java.util.UUID.class),
                              rows.getObject("started_at", java.time.OffsetDateTime.class)
                                  .toInstant(),
                              rows.getInt("planned_minutes"),
                              rows.getObject("planned_end_at", java.time.OffsetDateTime.class)
                                  .toInstant(),
                              rows.getString("zone_id"));
                      writer.session(
                          buffer,
                          rows.getString("receipt"),
                          id,
                          original,
                          rows.getString("action"),
                          rows.getLong("expected_revision"),
                          at);
                    }
                    var canonical = json.readTree(buffer.asParser());
                    if (jdbc.update(
                            "UPDATE import_stage SET existing_receipt=?::jsonb WHERE ctid=?::tid",
                            json.writeValueAsString(canonical),
                            rows.getString("stage_id"))
                        != 1)
                      throw new StorageUnavailableException(
                          new IllegalStateException("Receipt comparison was not prepared"));
                  } catch (IOException | IllegalArgumentException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                }
                return null;
              });
    }
  }

  private long matching(String sql, String owner) {
    var counts = jdbc.queryForMap(sql, owner);
    long occupied = ((Number) counts.get("occupied")).longValue();
    long identical = ((Number) counts.get("identical")).longValue();
    if (occupied != identical)
      throw new com.apptolast.organization.application.ImportConflictException();
    return identical;
  }

  private void validate(String owner) {
    if (jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM import_stage WHERE NOT pg_input_is_valid(raw,'jsonb'))",
        Boolean.class)) throw new ImportInvalidFileException();
    jdbc.update("UPDATE import_stage SET payload=raw::jsonb");
    long largestRow =
        jdbc.queryForObject(
            "SELECT coalesce(max(octet_length(payload::text)),1) FROM import_stage", Long.class);
    int validationBatch = (int) Math.max(1, Math.min(128, 65536 / (2 * largestRow)));
    jdbc.query(
        connection -> {
          var statement =
              connection.prepareStatement("SELECT collection,payload::text FROM import_stage");
          statement.setFetchSize(validationBatch);
          return statement;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Void>)
            rows -> {
              var json =
                  new com.fasterxml.jackson.databind.ObjectMapper()
                      .enable(
                          com.fasterxml.jackson.databind.DeserializationFeature
                              .USE_BIG_DECIMAL_FOR_FLOATS);
              while (rows.next()) {
                try {
                  var row = json.readTree(rows.getString(2));
                  ImportRecordValidator.row(rows.getString(1), row, owner);
                  switch (rows.getString(1)) {
                    case "appearance" -> ImportCustomizationValidator.appearance(row);
                    case "customization" -> ImportCustomizationValidator.configuration(row);
                    default -> {}
                  }
                } catch (IOException invalidStorage) {
                  throw new StorageUnavailableException(invalidStorage);
                }
              }
              return null;
            });

    for (var field :
        List.of("id", "projectId", "taskId", "parentId", "blockId", "sessionId", "scope"))
      jdbc.execute("CREATE INDEX ON import_stage (collection, (payload->>'" + field + "'))");
    jdbc.execute("ANALYZE import_stage");
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage
          WHERE collection IN ('availability','appearance','customization','projectCustomFieldValues','taskCustomFieldValues',
            'plannedBlocks','blockChanges','workSessions','workSessionChanges','taskStatusHistory')
          GROUP BY collection,CASE
            WHEN collection IN ('availability','appearance') THEN '[]'::jsonb
            WHEN collection='customization' THEN jsonb_build_array(payload->'scope')
            WHEN collection='projectCustomFieldValues' THEN jsonb_build_array(payload->'projectId')
            WHEN collection='taskCustomFieldValues' THEN jsonb_build_array(payload->'taskId')
            WHEN collection IN ('plannedBlocks','blockChanges') THEN jsonb_build_array(payload->'taskId',payload->'requestKey')
            WHEN collection IN ('workSessions','workSessionChanges') THEN jsonb_build_array(payload->'requestKey')
            WHEN collection='taskStatusHistory' THEN jsonb_build_array(payload->'taskId',payload->'taskVersion')
          END HAVING count(*)>1)
        OR EXISTS(SELECT 1 FROM import_stage WHERE collection='blockChanges'
          GROUP BY payload->'blockId',payload->'version' HAVING count(*)>1)
        OR EXISTS(SELECT 1 FROM import_stage WHERE collection='workSessionChanges' AND payload->>'action'='CLOSE'
          GROUP BY payload->'sessionId' HAVING count(*)>1)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage GROUP BY collection,
          CASE collection WHEN 'workSessionIntervals' THEN jsonb_build_array(payload->'sessionId',payload->'revision')
            WHEN 'blockProjections' THEN jsonb_build_array(payload->'blockId')
            ELSE jsonb_build_array(payload->'id') END
          HAVING count(*)>1)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(
          SELECT 1 FROM import_stage child
          JOIN (VALUES ('tasks','parentId','tasks'),('taskStatusHistory','taskId','tasks'),
            ('plannedBlocks','taskId','tasks'),('blockChanges','taskId','tasks'),
            ('blockChanges','blockId','plannedBlocks'),('workSessions','taskId','tasks'),
            ('taskCustomFieldValues','taskId','tasks')) edge(collection,field,target)
            ON edge.collection=child.collection
          JOIN import_stage parent ON parent.collection=edge.target AND parent.payload->>'id'=child.payload->>edge.field
          WHERE child.payload->>'projectId' IS DISTINCT FROM parent.payload->>'projectId'
            OR edge.target='plannedBlocks' AND child.payload->>'taskId' IS DISTINCT FROM parent.payload->>'taskId')
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(
          SELECT 1 FROM import_stage child
          JOIN (VALUES
            ('tasks','projectId','projects'),('tasks','parentId','tasks'),
            ('taskStatusHistory','projectId','projects'),('taskStatusHistory','taskId','tasks'),
            ('plannedBlocks','projectId','projects'),('plannedBlocks','taskId','tasks'),
            ('blockProjections','blockId','plannedBlocks'),
            ('blockChanges','projectId','projects'),('blockChanges','taskId','tasks'),('blockChanges','blockId','plannedBlocks'),
            ('workSessions','projectId','projects'),('workSessions','taskId','tasks'),
            ('workSessionIntervals','sessionId','workSessions'),('workSessionChanges','sessionId','workSessions'),
            ('projectCustomFieldValues','projectId','projects'),
            ('taskCustomFieldValues','projectId','projects'),('taskCustomFieldValues','taskId','tasks')
          ) edge(collection,field,target) ON edge.collection=child.collection
          LEFT JOIN import_stage parent ON parent.collection=edge.target AND parent.payload->>'id'=child.payload->>edge.field
          WHERE child.payload->>edge.field IS NOT NULL AND parent.payload IS NULL)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        WITH RECURSIVE reachable(id) AS (
          SELECT payload->>'id' FROM import_stage WHERE collection='tasks' AND payload->>'parentId' IS NULL
          UNION
          SELECT t.payload->>'id' FROM import_stage t JOIN reachable p ON p.id=t.payload->>'parentId'
            WHERE t.collection='tasks')
        SELECT (SELECT count(*) FROM reachable)<>(SELECT count(*) FROM import_stage WHERE collection='tasks')
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage t LEFT JOIN import_stage p
          ON p.collection='projects' AND p.payload->>'id'=t.payload->>'projectId'
          WHERE t.collection='tasks' AND p.payload IS NULL)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage WHERE collection='projects' AND
          (jsonb_typeof(payload)='object'
            AND jsonb_typeof(payload->'version')='string'
            AND (payload->>'version') ~ '^(0|[1-9][0-9]*)$'
            AND pg_input_is_valid(payload->>'version','bigint')
            AND payload ?& ARRAY['id','name','description','status','version','createdAt','updatedAt']
            AND payload-ARRAY['id','name','description','status','version','createdAt','updatedAt']='{}'::jsonb) IS NOT TRUE)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    jdbc.query(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  SELECT s.collection,s.payload::text,c.payload->'customFields' AS definitions
                  FROM import_stage s LEFT JOIN import_stage c
                    ON c.collection='customization' AND c.payload->>'scope'=CASE s.collection
                      WHEN 'projectCustomFieldValues' THEN 'PROJECT' WHEN 'taskCustomFieldValues' THEN 'TASK' END
                  WHERE s.collection IN ('projectCustomFieldValues','taskCustomFieldValues')
                  """);
          statement.setFetchSize(validationBatch);
          return statement;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Void>)
            rows -> {
              var json =
                  new com.fasterxml.jackson.databind.ObjectMapper()
                      .enable(
                          com.fasterxml.jackson.databind.DeserializationFeature
                              .USE_BIG_DECIMAL_FOR_FLOATS);
              while (rows.next()) {
                try {
                  var row = json.readTree(rows.getString(2));
                  switch (rows.getString(1)) {
                    case "projectCustomFieldValues", "taskCustomFieldValues" ->
                        ImportCustomizationValidator.values(
                            row.path("values"),
                            rows.getString("definitions") == null
                                ? json.createArrayNode()
                                : json.readTree(rows.getString("definitions")));
                    default -> {}
                  }
                } catch (IOException invalidStorage) {
                  throw new StorageUnavailableException(invalidStorage);
                }
              }
              return null;
            });
  }

  private void validateHistoricalRelations() {
    if (jdbc.queryForObject(
        """
        WITH latest_blocks AS (
          SELECT DISTINCT ON (payload->>'blockId') payload FROM import_stage
          WHERE collection='blockChanges' ORDER BY payload->>'blockId',(payload->>'version')::bigint DESC
        ), latest_sessions AS (
          SELECT DISTINCT ON (payload->>'sessionId') payload FROM import_stage
          WHERE collection='workSessionChanges'
          ORDER BY payload->>'sessionId',(payload->>'expectedRevision')::bigint DESC
        )
        SELECT EXISTS(SELECT 1 FROM import_stage c JOIN import_stage b
          ON b.collection='plannedBlocks' AND b.payload->>'id'=c.payload->>'blockId'
          CROSS JOIN LATERAL (VALUES(c.payload#>'{receipt,before}'),(c.payload#>'{receipt,after}')) snapshot(value)
          WHERE c.collection='blockChanges' AND snapshot.value IS NOT NULL AND snapshot.value<>'null'::jsonb AND (
            snapshot.value#>>'{request,objective}' IS DISTINCT FROM b.payload->>'objective'
            OR (snapshot.value->>'createdAt')::timestamptz IS DISTINCT FROM (b.payload->>'createdAt')::timestamptz))
        OR EXISTS(SELECT 1 FROM import_stage p JOIN latest_blocks c ON c.payload->>'blockId'=p.payload->>'blockId'
          WHERE p.collection='blockProjections' AND (
            p.payload->>'status' IS DISTINCT FROM CASE c.payload->>'kind' WHEN 'CANCELLED' THEN 'cancelled' ELSE 'planned' END
            OR (p.payload->>'updatedAt')::timestamptz IS DISTINCT FROM (c.payload->>'occurredAt')::timestamptz))
        OR EXISTS(SELECT 1 FROM import_stage p JOIN latest_blocks c ON c.payload->>'blockId'=p.payload->>'blockId'
          JOIN import_stage b ON b.collection='plannedBlocks' AND b.payload->>'id'=p.payload->>'blockId'
          CROSS JOIN LATERAL (SELECT CASE c.payload->>'kind' WHEN 'CANCELLED'
            THEN c.payload#>'{receipt,before}' ELSE c.payload#>'{receipt,after}' END AS value) snapshot
          WHERE p.collection='blockProjections' AND ROW(
            coalesce(p.payload->>'startLocal',b.payload->>'startLocal')::timestamp,
            coalesce(p.payload->>'endLocal',b.payload->>'endLocal')::timestamp,
            coalesce(p.payload->>'zoneId',b.payload->>'zoneId'),
            coalesce(p.payload->>'startOffset',b.payload->>'startOffset'),
            coalesce(p.payload->>'endOffset',b.payload->>'endOffset'),
            coalesce(p.payload->>'startAt',b.payload->>'startAt')::timestamptz,
            coalesce(p.payload->>'endAt',b.payload->>'endAt')::timestamptz,
            coalesce(p.payload->>'durationMinutes',b.payload->>'durationMinutes')::numeric)
            IS DISTINCT FROM ROW(
            (snapshot.value#>>'{request,startLocal}')::timestamp,(snapshot.value#>>'{request,endLocal}')::timestamp,
            snapshot.value#>>'{request,zoneId}',snapshot.value#>>'{time,startOffset}',snapshot.value#>>'{time,endOffset}',
            (snapshot.value#>>'{time,startAt}')::timestamptz,(snapshot.value#>>'{time,endAt}')::timestamptz,
            (snapshot.value#>>'{time,durationMinutes}')::numeric))
        OR EXISTS(SELECT 1 FROM import_stage s JOIN latest_sessions c ON c.payload->>'sessionId'=s.payload->>'id'
          WHERE s.collection='workSessions' AND ROW(
            s.payload->>'status',(s.payload->>'revision')::bigint,(s.payload->>'changedAt')::timestamptz,
            (s.payload->>'workedMicroseconds')::bigint,(s.payload->>'runningSince')::timestamptz)
            IS DISTINCT FROM ROW(
            c.payload#>>'{receipt,after,status}',(c.payload#>>'{receipt,after,revision}')::bigint,
            (c.payload#>>'{receipt,after,changedAt}')::timestamptz,
            (c.payload#>>'{receipt,after,workedMicroseconds}')::bigint,
            (c.payload#>>'{receipt,after,runningSince}')::timestamptz))
        OR EXISTS(SELECT 1 FROM import_stage s JOIN latest_sessions c ON c.payload->>'sessionId'=s.payload->>'id'
          WHERE s.collection='workSessions' AND s.payload->>'lastDecisionAt' IS NOT NULL
            AND (s.payload->>'lastDecisionAt')::timestamptz IS DISTINCT FROM (c.payload->>'occurredAt')::timestamptz)
        OR EXISTS(SELECT 1 FROM import_stage s JOIN LATERAL (
          SELECT c.payload FROM import_stage c WHERE c.collection='workSessionChanges'
            AND c.payload->>'sessionId'=s.payload->>'id' AND c.payload->>'action'='EXTEND'
          ORDER BY (c.payload->>'expectedRevision')::bigint DESC LIMIT 1) extension ON true
          WHERE s.collection='workSessions' AND
            coalesce(s.payload->>'effectiveEndAt',s.payload->>'plannedEndAt')::timestamptz
              IS DISTINCT FROM (extension.payload#>>'{receipt,extension,effectiveEndAt}')::timestamptz)
        OR EXISTS(SELECT 1 FROM import_stage h JOIN import_stage t
          ON t.collection='tasks' AND t.payload->>'id'=h.payload->>'taskId'
          WHERE h.collection='taskStatusHistory' AND (h.payload->>'taskVersion')::bigint>(t.payload->>'version')::bigint)
        OR EXISTS(SELECT 1 FROM import_stage i JOIN import_stage s
          ON s.collection='workSessions' AND s.payload->>'id'=i.payload->>'sessionId'
          WHERE i.collection='workSessionIntervals' AND (
            (i.payload->>'revision')::bigint>(s.payload->>'revision')::bigint
            OR (i.payload->>'endAt')::timestamptz<(i.payload->>'startAt')::timestamptz
            OR (i.payload->>'startAt')::timestamptz<(s.payload->>'startedAt')::timestamptz))
        OR EXISTS(SELECT 1 FROM import_stage s WHERE s.collection='workSessions' AND
          (s.payload->>'workedMicroseconds')::numeric<>coalesce((
            SELECT sum(extract(epoch FROM ((i.payload->>'endAt')::timestamptz-(i.payload->>'startAt')::timestamptz))*1000000)
            FROM import_stage i WHERE i.collection='workSessionIntervals' AND i.payload->>'sessionId'=s.payload->>'id'),0))
        OR EXISTS(SELECT 1 FROM import_stage p WHERE p.collection='blockProjections' AND
          (p.payload->>'version')::bigint IS DISTINCT FROM (
            SELECT max((c.payload->>'version')::bigint) FROM import_stage c
            WHERE c.collection='blockChanges' AND c.payload->>'blockId'=p.payload->>'blockId'))
        """,
        Boolean.class)) throw new ImportInvalidFileException();
  }

  private void prepareReceipts() {
    jdbc.query(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
          SELECT s.ctid::text,s.collection,s.payload::text,original.payload::text
          FROM import_stage s LEFT JOIN import_stage original
            ON original.collection='workSessions' AND original.payload->>'id'=s.payload->>'sessionId'
          WHERE s.collection IN ('blockChanges','workSessionChanges')
          """);
          statement.setFetchSize(1);
          return statement;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Void>)
            rows -> {
              var json =
                  com.fasterxml.jackson.databind.json.JsonMapper.builder()
                      .enable(
                          com.fasterxml.jackson.databind.DeserializationFeature
                              .USE_BIG_DECIMAL_FOR_FLOATS)
                      .enable(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII)
                      .build();
              var decoder = new ImportReceiptDecoder(json);
              while (rows.next()) {
                try {
                  var row = json.readTree(rows.getString(3));
                  com.fasterxml.jackson.databind.JsonNode durable;
                  if (rows.getString(2).equals("blockChanges")) {
                    durable =
                        decoder.block(
                            row.path("receipt"),
                            java.util.UUID.fromString(row.path("id").asText()),
                            java.util.UUID.fromString(row.path("blockId").asText()),
                            java.util.UUID.fromString(row.path("projectId").asText()),
                            java.util.UUID.fromString(row.path("taskId").asText()),
                            row.path("kind").asText(),
                            Long.parseLong(row.path("version").asText()),
                            java.time.Instant.parse(row.path("occurredAt").asText()));
                  } else {
                    if (rows.getString(4) == null) throw new ImportInvalidFileException();
                    var original = json.readTree(rows.getString(4));
                    var start =
                        new com.apptolast.organization.domain.SessionStart(
                            java.util.UUID.fromString(original.path("id").asText()),
                            java.util.UUID.fromString(original.path("projectId").asText()),
                            java.util.UUID.fromString(original.path("taskId").asText()),
                            java.time.Instant.parse(original.path("startedAt").asText()),
                            original.path("plannedMinutes").decimalValue().intValueExact(),
                            java.time.Instant.parse(original.path("plannedEndAt").asText()),
                            original.path("zoneId").asText());
                    durable =
                        decoder.session(
                            row.path("receipt"),
                            java.util.UUID.fromString(row.path("id").asText()),
                            start,
                            row.path("action").asText(),
                            Long.parseLong(row.path("expectedRevision").asText()),
                            java.time.Instant.parse(row.path("occurredAt").asText()));
                  }
                  if (jdbc.update(
                          "UPDATE import_stage SET durable_receipt=?::jsonb WHERE ctid=?::tid",
                          json.writeValueAsString(durable),
                          rows.getString(1))
                      != 1)
                    throw new StorageUnavailableException(
                        new IllegalStateException("Receipt preparation was not persisted"));
                } catch (IllegalArgumentException
                    | java.time.DateTimeException
                    | ArithmeticException invalid) {
                  throw new ImportInvalidFileException();
                } catch (IOException failure) {
                  throw new StorageUnavailableException(failure);
                }
              }
              return null;
            });
  }

  private static long storedCountTotal(com.fasterxml.jackson.databind.JsonNode counts) {
    var names =
        java.util.Set.of(
            "projects",
            "tasks",
            "taskStatusHistory",
            "availability",
            "plannedBlocks",
            "blockProjections",
            "blockChanges",
            "workSessions",
            "workSessionIntervals",
            "workSessionChanges",
            "appearance",
            "customization",
            "projectCustomFieldValues",
            "taskCustomFieldValues");
    if (counts == null || !counts.isObject() || counts.size() != names.size())
      throw new IllegalArgumentException("Invalid stored import counts");
    long total = 0;
    for (var name : names) {
      var count = counts.get(name);
      if (count == null
          || !count.isIntegralNumber()
          || !count.canConvertToLong()
          || count.longValue() < 0
          || count.longValue() > 100000)
        throw new IllegalArgumentException("Invalid stored import count");
      total += count.longValue();
    }
    if (total > 100000) throw new IllegalArgumentException("Invalid stored import total");
    return total;
  }

  public java.util.Optional<com.apptolast.organization.application.ImportReceipt> find(
      String owner, java.util.UUID key) {
    return stored(
        () ->
            jdbc
                .query(
                    "SELECT request_key,file_sha256,byte_length,recorded_at,outcome,inserted_counts::text,identical_counts::text FROM import_receipts WHERE owner_id=? AND request_key=?",
                    (row, index) -> {
                      try {
                        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        var inserted = mapper.readTree(row.getString("inserted_counts"));
                        var identical = mapper.readTree(row.getString("identical_counts"));
                        long insertedTotal = storedCountTotal(inserted);
                        long identicalTotal = storedCountTotal(identical);
                        var recorded = row.getObject("recorded_at", java.time.OffsetDateTime.class);
                        if (insertedTotal + identicalTotal > 100000
                            || recorded == null
                            || recorded.getYear() < 1
                            || recorded.getYear() > 9999
                            || recorded.getNano() % 1000 != 0
                            || !row.getString("outcome")
                                .equals(insertedTotal == 0 ? "NO_CHANGE" : "IMPORTED"))
                          throw new IllegalArgumentException("Invalid stored import receipt");
                        return new com.apptolast.organization.application.ImportReceipt(
                            row.getObject("request_key", java.util.UUID.class),
                            row.getString("file_sha256"),
                            row.getLong("byte_length"),
                            row.getObject("recorded_at", java.time.OffsetDateTime.class)
                                .toInstant(),
                            row.getString("outcome"),
                            mapper.readValue(
                                row.getString("inserted_counts"),
                                com.apptolast.organization.application.ImportCounts.class),
                            mapper.readValue(
                                row.getString("identical_counts"),
                                com.apptolast.organization.application.ImportCounts.class));
                      } catch (IOException | IllegalArgumentException invalid) {
                        throw new StorageUnavailableException(invalid);
                      }
                    },
                    owner,
                    key)
                .stream()
                .findFirst());
  }

  public com.apptolast.organization.application.ImportReceipt apply(
      String owner,
      java.util.UUID key,
      String expectedSha256,
      InputStream body,
      java.util.function.Supplier<java.time.Instant> recordedAt) {
    return stored(
        () ->
            writing.execute(
                status -> {
                  jdbc.execute("SET LOCAL lock_timeout='2s'");
                  jdbc.execute("SET LOCAL statement_timeout='10s'");
                  final ImportJsonReader.Header header;
                  try {
                    header = stage(body, expectedSha256);
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                  if (!owner.equals(header.owner())) throw new ImportInvalidFileException();
                  validate(owner);
                  prepareReceipts();
                  validateHistoricalRelations();
                  for (var scope : List.of("PROJECT", "TASK")) {
                    jdbc.queryForObject(
                        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                        Object.class,
                        "customization:" + owner + ":" + scope);
                  }
                  for (var table :
                      List.of(
                          "appearance_preferences",
                          "availability_preferences",
                          "block_changes",
                          "block_projections",
                          "customization_preferences",
                          "import_receipts",
                          "planned_blocks",
                          "project_custom_field_values",
                          "projects",
                          "task_custom_field_values",
                          "task_status_history",
                          "tasks",
                          "work_session_changes",
                          "work_session_intervals",
                          "work_sessions")) {
                    jdbc.execute("LOCK TABLE " + table + " IN EXCLUSIVE MODE");
                  }
                  var previous = find(owner, key);
                  if (previous.isPresent()) {
                    if (!previous.get().fileSha256().equals(header.fileSha256()))
                      throw new com.apptolast.organization.application.ImportKeyReusedException();
                    return previous.get();
                  }
                  var identical = compare(owner);
                  var inserted = header.counts().minus(identical);
                  int insertedProjects =
                      jdbc.update(
                          """
                      INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at)
                      SELECT (payload->>'id')::uuid,?,payload->>'name',payload->>'description',payload->>'status',
                        (payload->>'version')::bigint,(payload->>'createdAt')::timestamptz,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='projects'
                        AND NOT EXISTS(SELECT 1 FROM projects p WHERE p.id::text=s.payload->>'id')
                      """,
                          owner);
                  if (insertedProjects != inserted.projects())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported projects were not persisted"));
                  int insertedTasks =
                      jdbc.update(
                          """
                      INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,estimated_minutes,status,version,completed_at,created_at,updated_at)
                      SELECT (payload->>'id')::uuid,(payload->>'projectId')::uuid,(payload->>'parentId')::uuid,
                        payload->>'title',payload->>'completionCriterion',(payload->>'estimatedMinutes')::numeric::integer,
                        payload->>'status',(payload->>'version')::bigint,(payload->>'completedAt')::timestamptz,
                        (payload->>'createdAt')::timestamptz,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='tasks'
                        AND NOT EXISTS(SELECT 1 FROM tasks t WHERE t.id::text=s.payload->>'id')
                      """);
                  if (insertedTasks != inserted.tasks())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported tasks were not persisted"));
                  int insertedHistory =
                      jdbc.update(
                          """
                      INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at)
                      SELECT (payload->>'id')::uuid,(payload->>'projectId')::uuid,(payload->>'taskId')::uuid,
                        (payload->>'taskVersion')::bigint,payload->>'fromStatus',payload->>'toStatus',(payload->>'occurredAt')::timestamptz
                      FROM import_stage s WHERE collection='taskStatusHistory'
                        AND NOT EXISTS(SELECT 1 FROM task_status_history h WHERE h.id::text=s.payload->>'id')
                      """);
                  if (insertedHistory != inserted.taskStatusHistory())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported task history was not persisted"));
                  int insertedAvailability =
                      jdbc.update(
                          """
                      INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)
                      SELECT (payload->>'id')::uuid,?,payload->>'zoneId',(payload->>'mondayMinutes')::integer,
                        (payload->>'tuesdayMinutes')::integer,(payload->>'wednesdayMinutes')::integer,
                        (payload->>'thursdayMinutes')::integer,(payload->>'fridayMinutes')::integer,
                        (payload->>'saturdayMinutes')::integer,(payload->>'sundayMinutes')::integer,
                        (payload->>'version')::bigint,(payload->>'createdAt')::timestamptz,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='availability'
                        AND NOT EXISTS(SELECT 1 FROM availability_preferences a WHERE a.id::text=s.payload->>'id')
                      """,
                          owner);
                  if (insertedAvailability != inserted.availability())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported availability was not persisted"));
                  int insertedBlocks =
                      insert(
                          """
                      INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)
                      SELECT (payload->>'id')::uuid,(payload->>'projectId')::uuid,(payload->>'taskId')::uuid,
                        (payload->>'requestKey')::uuid,payload->>'objective',(payload->>'startLocal')::timestamp,
                        (payload->>'endLocal')::timestamp,payload->>'zoneId',payload->>'startOffset',payload->>'endOffset',
                        (payload->>'allowOverBudget')::boolean,(payload->>'startAt')::timestamptz,
                        (payload->>'endAt')::timestamptz,(payload->>'durationMinutes')::numeric::integer,
                        (payload->>'createdAt')::timestamptz
                      FROM import_stage s WHERE collection='plannedBlocks'
                        AND NOT EXISTS(SELECT 1 FROM planned_blocks r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.plannedBlocks());
                  int insertedProjections =
                      insert(
                          """
                      INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes)
                      SELECT (payload->>'blockId')::uuid,(payload->>'version')::bigint,payload->>'status',
                        (payload->>'updatedAt')::timestamptz,(payload->>'startLocal')::timestamp,(payload->>'endLocal')::timestamp,
                        payload->>'zoneId',payload->>'startOffset',payload->>'endOffset',(payload->>'startAt')::timestamptz,
                        (payload->>'endAt')::timestamptz,(payload->>'durationMinutes')::numeric::integer
                      FROM import_stage s WHERE collection='blockProjections'
                        AND NOT EXISTS(SELECT 1 FROM block_projections r WHERE r.block_id::text=s.payload->>'blockId')
                      """,
                          inserted.blockProjections());
                  int insertedSessions =
                      insert(
                          """
                      INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since,effective_end_at,last_decision_at)
                      SELECT (payload->>'id')::uuid,?,(payload->>'projectId')::uuid,(payload->>'taskId')::uuid,
                        (payload->>'requestKey')::uuid,(payload->>'startedAt')::timestamptz,(payload->>'plannedMinutes')::numeric::integer,
                        (payload->>'plannedEndAt')::timestamptz,payload->>'zoneId',payload->>'status',(payload->>'revision')::bigint,
                        (payload->>'changedAt')::timestamptz,(payload->>'workedMicroseconds')::bigint,
                        (payload->>'runningSince')::timestamptz,(payload->>'effectiveEndAt')::timestamptz,
                        (payload->>'lastDecisionAt')::timestamptz
                      FROM import_stage s WHERE collection='workSessions'
                        AND NOT EXISTS(SELECT 1 FROM work_sessions r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.workSessions(),
                          owner);
                  int insertedIntervals =
                      insert(
                          """
                      INSERT INTO work_session_intervals(session_id,revision,start_at,end_at)
                      SELECT (payload->>'sessionId')::uuid,(payload->>'revision')::bigint,
                        (payload->>'startAt')::timestamptz,(payload->>'endAt')::timestamptz
                      FROM import_stage s WHERE collection='workSessionIntervals'
                        AND NOT EXISTS(SELECT 1 FROM work_session_intervals r WHERE r.session_id::text=s.payload->>'sessionId' AND r.revision=(s.payload->>'revision')::bigint)
                      """,
                          inserted.workSessionIntervals());
                  int insertedAppearance =
                      insert(
                          """
                      INSERT INTO appearance_preferences(id,owner_id,theme,accent_light,accent_dark,version,updated_at)
                      SELECT (payload->>'id')::uuid,?,payload->>'theme',payload->>'accentLight',payload->>'accentDark',
                        (payload->>'version')::bigint,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='appearance'
                        AND NOT EXISTS(SELECT 1 FROM appearance_preferences r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.appearance(),
                          owner);
                  int insertedCustomization =
                      insert(
                          """
                      INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at)
                      SELECT (payload->>'id')::uuid,?,payload->>'scope',payload->'visibleFields',payload->'customFields',
                        (payload->>'version')::bigint,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='customization'
                        AND NOT EXISTS(SELECT 1 FROM customization_preferences r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.customization(),
                          owner);
                  int insertedProjectValues =
                      insert(
                          """
                      INSERT INTO project_custom_field_values(id,owner_id,project_id,field_values,version,updated_at)
                      SELECT (payload->>'id')::uuid,?,(payload->>'projectId')::uuid,
                        coalesce((SELECT jsonb_object_agg(v->>'fieldId',v->'value') FROM jsonb_array_elements(payload->'values') v),'{}'::jsonb),
                        (payload->>'version')::bigint,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='projectCustomFieldValues'
                        AND NOT EXISTS(SELECT 1 FROM project_custom_field_values r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.projectCustomFieldValues(),
                          owner);
                  int insertedTaskValues =
                      insert(
                          """
                      INSERT INTO task_custom_field_values(id,owner_id,task_id,field_values,version,updated_at)
                      SELECT (payload->>'id')::uuid,?,(payload->>'taskId')::uuid,
                        coalesce((SELECT jsonb_object_agg(v->>'fieldId',v->'value') FROM jsonb_array_elements(payload->'values') v),'{}'::jsonb),
                        (payload->>'version')::bigint,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='taskCustomFieldValues'
                        AND NOT EXISTS(SELECT 1 FROM task_custom_field_values r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.taskCustomFieldValues(),
                          owner);
                  int insertedBlockChanges =
                      insert(
                          """
                      INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)
                      SELECT (payload->>'id')::uuid,(payload->>'projectId')::uuid,(payload->>'taskId')::uuid,
                        (payload->>'blockId')::uuid,(payload->>'requestKey')::uuid,payload->>'kind',
                        (payload->>'version')::bigint,(payload->>'occurredAt')::timestamptz,durable_receipt
                      FROM import_stage s WHERE collection='blockChanges'
                        AND NOT EXISTS(SELECT 1 FROM block_changes r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.blockChanges());
                  int insertedSessionChanges =
                      insert(
                          """
                      INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt)
                      SELECT (payload->>'id')::uuid,?,(payload->>'sessionId')::uuid,(payload->>'requestKey')::uuid,
                        payload->>'action',(payload->>'expectedRevision')::bigint,(payload->>'occurredAt')::timestamptz,durable_receipt
                      FROM import_stage s WHERE collection='workSessionChanges'
                        AND NOT EXISTS(SELECT 1 FROM work_session_changes r WHERE r.id::text=s.payload->>'id')
                      """,
                          inserted.workSessionChanges(),
                          owner);
                  var receipt =
                      new com.apptolast.organization.application.ImportReceipt(
                          key,
                          header.fileSha256(),
                          header.byteLength(),
                          recordedAt.get(),
                          insertedProjects
                                      + insertedTasks
                                      + insertedHistory
                                      + insertedAvailability
                                      + insertedBlocks
                                      + insertedProjections
                                      + insertedSessions
                                      + insertedIntervals
                                      + insertedAppearance
                                      + insertedCustomization
                                      + insertedProjectValues
                                      + insertedTaskValues
                                      + insertedBlockChanges
                                      + insertedSessionChanges
                                  == 0
                              ? "NO_CHANGE"
                              : "IMPORTED",
                          inserted,
                          identical);
                  try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    int affected =
                        jdbc.update(
                            "INSERT INTO import_receipts(owner_id,request_key,file_sha256,byte_length,recorded_at,outcome,inserted_counts,identical_counts) VALUES (?,?,?,?,?,?,?::jsonb,?::jsonb)",
                            owner,
                            key,
                            receipt.fileSha256(),
                            receipt.byteLength(),
                            java.time.OffsetDateTime.ofInstant(
                                receipt.recordedAt(), java.time.ZoneOffset.UTC),
                            receipt.outcome(),
                            mapper.writeValueAsString(receipt.insertedCounts()),
                            mapper.writeValueAsString(receipt.identicalCounts()));
                    if (affected != 1)
                      throw new StorageUnavailableException(
                          new IllegalStateException("Import receipt was not persisted"));
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                  return receipt;
                }));
  }

  private static <T> T stored(java.util.function.Supplier<T> operation) {
    try {
      return operation.get();
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException failure) {
      throw new StorageUnavailableException(failure);
    }
  }

  private int insert(String sql, long expected, Object... arguments) {
    int affected = jdbc.update(sql, arguments);
    if (affected != expected)
      throw new StorageUnavailableException(
          new IllegalStateException("Imported rows were not persisted"));
    return affected;
  }
}
