package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ExportDataQueries;
import com.apptolast.organization.application.PreparedExport;
import com.apptolast.organization.application.StorageUnavailableException;
import java.io.IOException;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresExportDataQueries implements ExportDataQueries {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final PostgresCustomizationStore customization;
  private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
      new com.fasterxml.jackson.databind.ObjectMapper()
          .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

  public PostgresExportDataQueries(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    customization =
        new PostgresCustomizationStore(
            jdbc, manager, new com.fasterxml.jackson.databind.ObjectMapper());
    transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public PreparedExport prepare(String owner, Supplier<Instant> timestamp) {
    try {
      return transaction.execute(
          status -> {
            jdbc.queryForObject("SELECT pg_current_snapshot()::text", String.class);
            if (jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM work_sessions s LEFT JOIN projects p ON p.id=s.project_id LEFT JOIN tasks t ON t.id=s.task_id WHERE s.owner_id=? AND (p.owner_id IS DISTINCT FROM s.owner_id OR t.project_id IS DISTINCT FROM s.project_id))",
                Boolean.class,
                owner)) throw new IllegalArgumentException("Invalid session relationship");
            if (jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM project_custom_field_values v LEFT JOIN projects p ON p.id=v.project_id WHERE v.owner_id=? AND p.owner_id IS DISTINCT FROM v.owner_id)",
                Boolean.class,
                owner)) throw new IllegalArgumentException("Invalid project values relationship");
            if (jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM task_custom_field_values v LEFT JOIN tasks t ON t.id=v.task_id LEFT JOIN projects p ON p.id=t.project_id WHERE v.owner_id=? AND p.owner_id IS DISTINCT FROM v.owner_id)",
                Boolean.class,
                owner)) throw new IllegalArgumentException("Invalid task values relationship");
            if (jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM work_session_changes c LEFT JOIN work_sessions s ON s.id=c.session_id WHERE (c.owner_id=? OR s.owner_id=?) AND c.owner_id IS DISTINCT FROM s.owner_id)",
                Boolean.class,
                owner,
                owner)) throw new IllegalArgumentException("Invalid session change relationship");
            guardRawSize(owner);
            if (recordCount(owner) > 100000)
              throw new com.apptolast.organization.application.ExportTooLargeException();
            if (jdbc.queryForObject(
                    "SELECT coalesce(sum(octet_length(name)::bigint+octet_length(description)),0) FROM projects WHERE owner_id=?",
                    Long.class,
                    owner)
                > ExportBuffer.LIMIT)
              throw new com.apptolast.organization.application.ExportTooLargeException();
            try {
              return new ExportJsonWriter()
                  .prepare(
                      owner,
                      timestamp.get(),
                      (collection, json) -> writeCollection(owner, collection, json));
            } catch (IOException error) {
              throw new StorageUnavailableException(error);
            }
          });
    } catch (com.apptolast.organization.application.ExportTooLargeException
        | StorageUnavailableException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new StorageUnavailableException(error);
    }
  }

  long writeCollection(
      String owner, String collection, com.fasterxml.jackson.core.JsonGenerator json)
      throws IOException {
    return switch (collection) {
      case "projects" -> projects(owner, json);
      case "tasks" -> tasks(owner, json);
      case "plannedBlocks" -> blocks(owner, json);
      case "blockProjections" -> projections(owner, json);
      case "blockChanges" -> blockChanges(owner, json);
      case "workSessions" -> sessions(owner, json);
      case "workSessionIntervals" -> intervals(owner, json);
      case "workSessionChanges" -> sessionChanges(owner, json);
      case "taskStatusHistory" -> taskHistory(owner, json);
      case "availability" -> availability(owner, json);
      case "appearance" -> appearance(owner, json);
      case "customization" -> customization(owner, json);
      case "projectCustomFieldValues" -> projectValues(owner, json);
      case "taskCustomFieldValues" -> taskValues(owner, json);
      default -> throw new IllegalStateException("Unknown export collection: " + collection);
    };
  }

  private int scalarBatch(String owner, String sizeQuery) {
    long maximumBytes = jdbc.queryForObject(sizeQuery, Long.class, owner);
    if (maximumBytes > ExportBuffer.LIMIT)
      throw new IllegalArgumentException("Stored scalar exceeds the bounded reader");
    // Account for UTF-16 decoding and fixed JDBC columns; large rows remain single-row reads.
    return (int) Math.max(1, Math.min(64, 1048576L / (2 * maximumBytes + 4096)));
  }

  private long projects(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return jdbc.query(
        connection -> {
          var query =
              connection.prepareStatement(
                  "SELECT id,name,description,status,version,created_at,updated_at FROM projects WHERE owner_id=? ORDER BY id",
                  java.sql.ResultSet.TYPE_FORWARD_ONLY,
                  java.sql.ResultSet.CONCUR_READ_ONLY);
          query.setString(1, owner);
          query.setFetchSize(64);
          return query;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Long>)
            rows -> {
              long count = 0;
              while (rows.next()) {
                try {
                  json.writeStartObject();
                  json.writeStringField("id", rows.getString("id"));
                  json.writeStringField("name", rows.getString("name"));
                  json.writeStringField("description", rows.getString("description"));
                  json.writeStringField("status", rows.getString("status"));
                  json.writeStringField("version", rows.getString("version"));
                  for (var name : java.util.List.of("createdAt", "updatedAt")) {
                    var instant =
                        rows.getObject(
                                name.equals("createdAt") ? "created_at" : "updated_at",
                                java.time.OffsetDateTime.class)
                            .toInstant();
                    json.writeStringField(name, instant(instant));
                  }
                  json.writeEndObject();
                  count++;
                } catch (IOException error) {
                  throw new StorageUnavailableException(error);
                }
              }
              return count;
            });
  }

  private long tasks(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT t.id,t.project_id,t.parent_id,t.title,t.completion_criterion,t.estimated_minutes,t.status,t.version,t.completed_at,t.created_at,t.updated_at FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? ORDER BY t.id",
        owner,
        (row) -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("parentId", row.getString("parent_id"));
          json.writeStringField("title", row.getString("title"));
          json.writeStringField("completionCriterion", row.getString("completion_criterion"));
          var minutes = row.getObject("estimated_minutes", Integer.class);
          if (minutes == null) json.writeNullField("estimatedMinutes");
          else json.writeNumberField("estimatedMinutes", minutes);
          json.writeStringField("status", row.getString("status"));
          json.writeStringField("version", row.getString("version"));
          time(json, row, "completedAt", "completed_at");
          time(json, row, "createdAt", "created_at");
          time(json, row, "updatedAt", "updated_at");
          json.writeEndObject();
        },
        64);
  }

  private interface RowWriter {
    void write(java.sql.ResultSet row) throws IOException, java.sql.SQLException;
  }

  private long rows(String sql, String owner, RowWriter writer) {
    return rows(sql, owner, writer, 1);
  }

  private long rows(String sql, String owner, RowWriter writer, int fetchSize) {
    return jdbc.query(
        connection -> {
          var statement =
              connection.prepareStatement(
                  sql, java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
          statement.setString(1, owner);
          statement.setFetchSize(fetchSize);
          return statement;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Long>)
            result -> {
              long count = 0;
              while (result.next()) {
                try {
                  writer.write(result);
                } catch (IOException error) {
                  throw new StorageUnavailableException(error);
                }
                count++;
              }
              return count;
            });
  }

  private static void time(
      com.fasterxml.jackson.core.JsonGenerator json,
      java.sql.ResultSet row,
      String field,
      String column)
      throws IOException, java.sql.SQLException {
    var value = row.getObject(column, java.time.OffsetDateTime.class);
    json.writeStringField(field, value == null ? null : instant(value.toInstant()));
  }

  private static final java.time.format.DateTimeFormatter INSTANT =
      new java.time.format.DateTimeFormatterBuilder().appendInstant(6).toFormatter();

  private static String instant(Instant value) {
    int year = value.atOffset(java.time.ZoneOffset.UTC).getYear();
    if (year < 1 || year > 9999)
      throw new IllegalArgumentException("Unrepresentable stored instant");
    return INSTANT.format(value);
  }

  private long taskHistory(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT h.id,h.project_id,h.task_id,h.task_version,h.from_status,h.to_status,h.occurred_at FROM task_status_history h JOIN projects p ON p.id=h.project_id WHERE p.owner_id=? ORDER BY h.id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("taskId", row.getString("task_id"));
          json.writeStringField("taskVersion", row.getString("task_version"));
          json.writeStringField("fromStatus", row.getString("from_status"));
          json.writeStringField("toStatus", row.getString("to_status"));
          time(json, row, "occurredAt", "occurred_at");
          json.writeEndObject();
        },
        64);
  }

  private long availability(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at FROM availability_preferences WHERE owner_id=? ORDER BY id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("zoneId", row.getString("zone_id"));
          for (var day :
              java.util.List.of(
                  "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"))
            json.writeNumberField(day + "Minutes", row.getInt(day + "_minutes"));
          json.writeStringField("version", row.getString("version"));
          time(json, row, "createdAt", "created_at");
          time(json, row, "updatedAt", "updated_at");
          json.writeEndObject();
        });
  }

  private long appearance(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT id,theme,accent_light,accent_dark,version,updated_at FROM appearance_preferences WHERE owner_id=? ORDER BY id",
        owner,
        row -> {
          var values =
              new com.apptolast.organization.domain.AppearanceValues(
                  row.getString("theme"),
                  row.getString("accent_light"),
                  row.getString("accent_dark"));
          if (!values.accentLight().equals(row.getString("accent_light"))
              || !values.accentDark().equals(row.getString("accent_dark")))
            throw new IllegalArgumentException("Noncanonical stored appearance");
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("theme", row.getString("theme"));
          json.writeStringField("accentLight", row.getString("accent_light"));
          json.writeStringField("accentDark", row.getString("accent_dark"));
          json.writeStringField("version", row.getString("version"));
          time(json, row, "updatedAt", "updated_at");
          json.writeEndObject();
        });
  }

  private long customization(String owner, com.fasterxml.jackson.core.JsonGenerator json)
      throws IOException {
    long count = 0;
    for (var scope : com.apptolast.organization.domain.CustomizationScope.values()) {
      var stored = customization.find(owner, scope);
      if (stored.isEmpty()) continue;
      var value = stored.orElseThrow();
      json.writeStartObject();
      json.writeStringField("id", value.id().toString());
      json.writeStringField("scope", value.scope().name());
      json.writeArrayFieldStart("visibleFields");
      for (var field : value.visibleFields()) json.writeString(field);
      json.writeEndArray();
      json.writeArrayFieldStart("customFields");
      for (var field : value.customFields()) {
        json.writeStartObject();
        json.writeStringField("id", field.id().toString());
        json.writeStringField("label", field.label());
        json.writeStringField("type", field.type().name());
        json.writeBooleanField("active", field.active());
        json.writeEndObject();
      }
      json.writeEndArray();
      json.writeStringField("version", Long.toString(value.version()));
      json.writeStringField(
          "updatedAt",
          java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
              .withZone(java.time.ZoneOffset.UTC)
              .format(value.updatedAt()));
      json.writeEndObject();
      count++;
    }
    return count;
  }

  private long projectValues(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    var configuration =
        customization.find(owner, com.apptolast.organization.domain.CustomizationScope.PROJECT);
    return rows(
        "SELECT v.id,v.project_id,v.field_values,v.version,v.updated_at FROM project_custom_field_values v WHERE v.owner_id=? ORDER BY v.id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          values(json, row.getString("field_values"), configuration);
          json.writeStringField("version", row.getString("version"));
          time(json, row, "updatedAt", "updated_at");
          json.writeEndObject();
        });
  }

  private static void values(
      com.fasterxml.jackson.core.JsonGenerator json,
      String raw,
      java.util.Optional<com.apptolast.organization.domain.Customization> configuration)
      throws IOException {
    var stored = JSON.readTree(raw);
    if (!stored.isObject() || stored.size() > 12)
      throw new IllegalArgumentException("Invalid stored values");
    var ordered = new java.util.TreeMap<String, com.fasterxml.jackson.databind.JsonNode>();
    stored.fields().forEachRemaining(entry -> ordered.put(entry.getKey(), entry.getValue()));
    json.writeArrayFieldStart("values");
    for (var entry : ordered.entrySet()) {
      var id = java.util.UUID.fromString(entry.getKey());
      if (!id.toString().equals(entry.getKey()))
        throw new IllegalArgumentException("Invalid stored field identity");
      var definition =
          configuration.orElseThrow().customFields().stream()
              .filter(field -> field.id().equals(id))
              .findFirst()
              .orElseThrow();
      var node = entry.getValue();
      Object value;
      if (node.isNull()) value = null;
      else if (node.isTextual()) value = node.textValue();
      else if (node.isBoolean()) value = node.booleanValue();
      else if (node.isNumber()) value = node.decimalValue();
      else throw new IllegalArgumentException("Invalid stored value type");
      var canonical =
          new com.apptolast.organization.domain.CustomFieldInput(id, value)
              .canonical(definition.type(), 0);
      if (value instanceof String && !value.equals(canonical))
        throw new IllegalArgumentException("Noncanonical stored text");
      json.writeStartObject();
      json.writeStringField("fieldId", id.toString());
      json.writeFieldName("value");
      if (canonical == null) json.writeNull();
      else if (canonical instanceof String text) json.writeString(text);
      else if (canonical instanceof Boolean bool) json.writeBoolean(bool);
      else json.writeNumber((Integer) canonical);
      json.writeEndObject();
    }
    json.writeEndArray();
  }

  private long taskValues(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    var configuration =
        customization.find(owner, com.apptolast.organization.domain.CustomizationScope.TASK);
    return rows(
        "SELECT v.id,t.project_id,v.task_id,v.field_values,v.version,v.updated_at FROM task_custom_field_values v JOIN tasks t ON t.id=v.task_id WHERE v.owner_id=? ORDER BY v.id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("taskId", row.getString("task_id"));
          values(json, row.getString("field_values"), configuration);
          json.writeStringField("version", row.getString("version"));
          time(json, row, "updatedAt", "updated_at");
          json.writeEndObject();
        });
  }

  private long blocks(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT b.id,b.project_id,b.task_id,b.request_key,b.objective,b.start_local,b.end_local,b.zone_id,b.start_offset,b.end_offset,b.allow_over_budget,b.start_at,b.end_at,b.duration_minutes,b.created_at FROM planned_blocks b JOIN projects p ON p.id=b.project_id WHERE p.owner_id=? ORDER BY b.id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("taskId", row.getString("task_id"));
          json.writeStringField("requestKey", row.getString("request_key"));
          json.writeStringField("objective", row.getString("objective"));
          local(json, row, "startLocal", "start_local");
          local(json, row, "endLocal", "end_local");
          json.writeStringField("zoneId", row.getString("zone_id"));
          offset(json, row, "startOffset", "start_offset");
          offset(json, row, "endOffset", "end_offset");
          json.writeBooleanField("allowOverBudget", row.getBoolean("allow_over_budget"));
          time(json, row, "startAt", "start_at");
          time(json, row, "endAt", "end_at");
          json.writeNumberField("durationMinutes", row.getInt("duration_minutes"));
          time(json, row, "createdAt", "created_at");
          json.writeEndObject();
        },
        scalarBatch(
            owner,
            "SELECT coalesce(max(octet_length(b.objective)::bigint+octet_length(b.zone_id)+coalesce(octet_length(b.start_offset),0)+coalesce(octet_length(b.end_offset),0)),0) FROM planned_blocks b JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?"));
  }

  private static void offset(
      com.fasterxml.jackson.core.JsonGenerator json,
      java.sql.ResultSet row,
      String field,
      String column)
      throws IOException, java.sql.SQLException {
    var value = row.getString(column);
    if (value != null) java.time.ZoneOffset.of(value);
    json.writeStringField(field, value);
  }

  private static void local(
      com.fasterxml.jackson.core.JsonGenerator json,
      java.sql.ResultSet row,
      String field,
      String column)
      throws IOException, java.sql.SQLException {
    var value = row.getObject(column, java.time.LocalDateTime.class);
    json.writeStringField(
        field,
        value == null
            ? null
            : java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").format(value));
  }

  private long projections(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT v.block_id,v.version,v.status,v.updated_at,v.start_local,v.end_local,v.zone_id,v.start_offset,v.end_offset,v.start_at,v.end_at,v.duration_minutes FROM block_projections v JOIN planned_blocks b ON b.id=v.block_id JOIN projects p ON p.id=b.project_id WHERE p.owner_id=? ORDER BY v.block_id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("blockId", row.getString("block_id"));
          json.writeStringField("version", row.getString("version"));
          json.writeStringField("status", row.getString("status"));
          time(json, row, "updatedAt", "updated_at");
          local(json, row, "startLocal", "start_local");
          local(json, row, "endLocal", "end_local");
          json.writeStringField("zoneId", row.getString("zone_id"));
          offset(json, row, "startOffset", "start_offset");
          offset(json, row, "endOffset", "end_offset");
          time(json, row, "startAt", "start_at");
          time(json, row, "endAt", "end_at");
          var duration = row.getObject("duration_minutes", Integer.class);
          if (duration == null) json.writeNullField("durationMinutes");
          else json.writeNumberField("durationMinutes", duration);
          json.writeEndObject();
        },
        scalarBatch(
            owner,
            "SELECT coalesce(max(coalesce(octet_length(v.zone_id),0)::bigint+coalesce(octet_length(v.start_offset),0)+coalesce(octet_length(v.end_offset),0)),0) FROM block_projections v JOIN planned_blocks b ON b.id=v.block_id JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?"));
  }

  private long sessions(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since,effective_end_at,last_decision_at FROM work_sessions WHERE owner_id=? ORDER BY id",
        owner,
        row -> {
          if (row.getLong("revision") < 0)
            throw new IllegalArgumentException("Invalid session revision");
          if (!java.util.Set.of("running", "paused", "closed").contains(row.getString("status")))
            throw new IllegalArgumentException("Invalid session status");
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("taskId", row.getString("task_id"));
          json.writeStringField("requestKey", row.getString("request_key"));
          time(json, row, "startedAt", "started_at");
          json.writeNumberField("plannedMinutes", row.getInt("planned_minutes"));
          time(json, row, "plannedEndAt", "planned_end_at");
          json.writeStringField("zoneId", row.getString("zone_id"));
          json.writeStringField("status", row.getString("status"));
          json.writeStringField("revision", row.getString("revision"));
          time(json, row, "changedAt", "changed_at");
          json.writeStringField("workedMicroseconds", row.getString("worked_microseconds"));
          time(json, row, "runningSince", "running_since");
          time(json, row, "effectiveEndAt", "effective_end_at");
          time(json, row, "lastDecisionAt", "last_decision_at");
          json.writeEndObject();
        },
        scalarBatch(
            owner,
            "SELECT coalesce(max(octet_length(zone_id)::bigint+octet_length(status)),0) FROM work_sessions WHERE owner_id=?"));
  }

  private long intervals(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT i.session_id,i.revision,i.start_at,i.end_at FROM work_session_intervals i JOIN work_sessions s ON s.id=i.session_id WHERE s.owner_id=? ORDER BY i.session_id,i.revision",
        owner,
        row -> {
          if (row.getLong("revision") < 0)
            throw new IllegalArgumentException("Invalid interval revision");
          json.writeStartObject();
          json.writeStringField("sessionId", row.getString("session_id"));
          json.writeStringField("revision", row.getString("revision"));
          time(json, row, "startAt", "start_at");
          time(json, row, "endAt", "end_at");
          json.writeEndObject();
        },
        64);
  }

  private long recordCount(String owner) {
    var sources =
        java.util.List.of(
            "projects WHERE owner_id=?",
            "tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=?",
            "task_status_history h JOIN projects p ON p.id=h.project_id WHERE p.owner_id=?",
            "availability_preferences WHERE owner_id=?",
            "planned_blocks b JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?",
            "block_projections v JOIN planned_blocks b ON b.id=v.block_id JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?",
            "block_changes c JOIN projects p ON p.id=c.project_id WHERE p.owner_id=?",
            "work_sessions WHERE owner_id=?",
            "work_session_intervals i JOIN work_sessions s ON s.id=i.session_id WHERE s.owner_id=?",
            "work_session_changes WHERE owner_id=?",
            "appearance_preferences WHERE owner_id=?",
            "customization_preferences WHERE owner_id=?",
            "project_custom_field_values WHERE owner_id=?",
            "task_custom_field_values WHERE owner_id=?");
    var query =
        sources.stream()
            .map(source -> "SELECT 1 FROM " + source)
            .collect(java.util.stream.Collectors.joining(" UNION ALL "));
    return jdbc.queryForObject(
        "SELECT count(*) FROM (" + query + " LIMIT 100001) records",
        Long.class,
        java.util.Collections.nCopies(sources.size(), owner).toArray());
  }

  private void guardRawSize(String owner) {
    var sources =
        java.util.List.of(
            "SELECT greatest(octet_length(visible_fields::text),octet_length(custom_fields::text)) AS bytes FROM customization_preferences WHERE owner_id=?",
            "SELECT octet_length(field_values::text) FROM project_custom_field_values WHERE owner_id=?",
            "SELECT octet_length(field_values::text) FROM task_custom_field_values WHERE owner_id=?",
            "SELECT octet_length(receipt::text)::bigint+octet_length(action) FROM work_session_changes WHERE owner_id=?",
            "SELECT octet_length(zone_id) FROM availability_preferences WHERE owner_id=?",
            "SELECT octet_length(zone_id)::bigint+octet_length(status) FROM work_sessions WHERE owner_id=?",
            "SELECT octet_length(b.objective)::bigint+octet_length(b.zone_id)+coalesce(octet_length(b.start_offset),0)+coalesce(octet_length(b.end_offset),0) FROM planned_blocks b JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?",
            "SELECT coalesce(octet_length(v.zone_id),0)::bigint+coalesce(octet_length(v.start_offset),0)+coalesce(octet_length(v.end_offset),0) FROM block_projections v JOIN planned_blocks b ON b.id=v.block_id JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?",
            "SELECT octet_length(c.receipt::text) FROM block_changes c JOIN projects p ON p.id=c.project_id WHERE p.owner_id=?");
    if (jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM ("
            + String.join(" UNION ALL ", sources)
            + ") payload WHERE bytes>"
            + ExportBuffer.LIMIT
            + ")",
        Boolean.class,
        java.util.Collections.nCopies(sources.size(), owner).toArray()))
      throw new IllegalArgumentException("Stored payload exceeds the bounded reader");
  }

  private long blockChanges(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT c.id,c.project_id,c.task_id,c.block_id,c.request_key,c.kind,c.version,c.occurred_at,c.receipt FROM block_changes c JOIN projects p ON p.id=c.project_id WHERE p.owner_id=? ORDER BY c.id",
        owner,
        row -> {
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("projectId", row.getString("project_id"));
          json.writeStringField("taskId", row.getString("task_id"));
          json.writeStringField("blockId", row.getString("block_id"));
          json.writeStringField("requestKey", row.getString("request_key"));
          json.writeStringField("kind", row.getString("kind"));
          json.writeStringField("version", row.getString("version"));
          time(json, row, "occurredAt", "occurred_at");
          json.writeFieldName("receipt");
          new ExportReceiptWriter(JSON)
              .block(
                  json,
                  row.getString("receipt"),
                  row.getObject("id", java.util.UUID.class),
                  row.getObject("block_id", java.util.UUID.class),
                  row.getObject("project_id", java.util.UUID.class),
                  row.getObject("task_id", java.util.UUID.class),
                  row.getString("kind"),
                  row.getLong("version"),
                  row.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant());
          json.writeEndObject();
        });
  }

  private long sessionChanges(String owner, com.fasterxml.jackson.core.JsonGenerator json) {
    return rows(
        "SELECT c.id,c.session_id,c.request_key,c.action,c.expected_revision,c.occurred_at,c.receipt,s.project_id,s.task_id,s.started_at,s.planned_minutes,s.planned_end_at,s.zone_id FROM work_session_changes c JOIN work_sessions s ON s.id=c.session_id WHERE c.owner_id=? ORDER BY c.id",
        owner,
        row -> {
          var original =
              new com.apptolast.organization.domain.SessionStart(
                  row.getObject("session_id", java.util.UUID.class),
                  row.getObject("project_id", java.util.UUID.class),
                  row.getObject("task_id", java.util.UUID.class),
                  row.getObject("started_at", java.time.OffsetDateTime.class).toInstant(),
                  row.getInt("planned_minutes"),
                  row.getObject("planned_end_at", java.time.OffsetDateTime.class).toInstant(),
                  row.getString("zone_id"));
          json.writeStartObject();
          json.writeStringField("id", row.getString("id"));
          json.writeStringField("sessionId", row.getString("session_id"));
          json.writeStringField("requestKey", row.getString("request_key"));
          json.writeStringField("action", row.getString("action"));
          json.writeStringField("expectedRevision", row.getString("expected_revision"));
          time(json, row, "occurredAt", "occurred_at");
          json.writeFieldName("receipt");
          new ExportReceiptWriter(JSON)
              .session(
                  json,
                  row.getString("receipt"),
                  row.getObject("id", java.util.UUID.class),
                  original,
                  row.getString("action"),
                  row.getLong("expected_revision"),
                  row.getObject("occurred_at", java.time.OffsetDateTime.class).toInstant());
          json.writeEndObject();
        });
  }
}
