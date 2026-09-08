package com.apptolast.organization.adapter.persistence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;

public final class ExportReceiptWriter {
  private static final DateTimeFormatter INSTANT =
      new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
  private static final DateTimeFormatter LOCAL =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");
  private final ObjectMapper json;

  public ExportReceiptWriter(ObjectMapper json) {
    this.json = json;
  }

  public void session(
      JsonGenerator out,
      String raw,
      UUID changeId,
      com.apptolast.organization.domain.SessionStart original,
      String action,
      long expectedRevision,
      Instant occurredAt)
      throws IOException {
    var value = read(raw);
    require(
        value.path("id").asText().equals(changeId.toString())
            && value.path("sessionId").asText().equals(original.id().toString())
            && value.path("action").asText().equals(action)
            && value.path("before").path("revision").isIntegralNumber()
            && value.path("before").path("revision").canConvertToLong()
            && value.path("before").path("revision").longValue() == expectedRevision
            && checkedInstant(value, "occurredAt").equals(occurredAt));
    original(value.path("before").path("session"), original);
    original(value.path("after").path("session"), original);
    validateTransition(value, original, action, expectedRevision, occurredAt);
    out.writeStartObject();
    fields(out, value, "id", "sessionId", "action");
    instant(out, value, "occurredAt");
    out.writeFieldName("before");
    state(out, value.path("before"));
    out.writeFieldName("after");
    state(out, value.path("after"));
    if (action.equals("CLOSE")) {
      out.writeObjectFieldStart("closure");
      fields(out, value.path("closure"), "progressNote", "nextStep", "workDate", "closeZoneId");
      out.writeEndObject();
    } else if (action.equals("EXTEND")) {
      var extension = value.path("extension");
      out.writeObjectFieldStart("extension");
      fields(out, extension, "additionalMinutes");
      instant(out, extension, "previousEndAt");
      instant(out, extension, "effectiveEndAt");
      out.writeEndObject();
    }
    out.writeEndObject();
  }

  private static void validateTransition(
      JsonNode value,
      com.apptolast.organization.domain.SessionStart original,
      String action,
      long revision,
      Instant now) {
    require(java.util.Set.of("PAUSE", "RESUME", "CLOSE", "EXTEND").contains(action));
    require(revision > 0 && revision < Long.MAX_VALUE);
    var before = readState(value.path("before"), original);
    var after = readState(value.path("after"), original);
    require(
        before.workedMicroseconds() >= 0
            && !before.changedAt().isBefore(original.startedAt())
            && before.workedMicroseconds()
                <= java.time.temporal.ChronoUnit.MICROS.between(
                    original.startedAt(), before.changedAt())
            && !now.isBefore(before.changedAt()));
    boolean running = before.status().equals("running");
    require(
        running
            ? before.changedAt().equals(before.runningSince())
            : before.status().equals("paused") && before.runningSince() == null);
    if (!action.equals("CLOSE"))
      require(value.path("closure").isMissingNode() || value.path("closure").isNull());
    if (!action.equals("EXTEND"))
      require(value.path("extension").isMissingNode() || value.path("extension").isNull());
    if (action.equals("CLOSE")) {
      var closure = value.path("closure");
      require(closure.isObject());
      try {
        new com.apptolast.organization.domain.WorkSessionCloseNotes(
            text(closure, "progressNote"), text(closure, "nextStep"));
        var day = java.time.LocalDate.parse(text(closure, "workDate"));
        require(
            day.getYear() >= 1 && day.getYear() <= 9999 && !text(closure, "closeZoneId").isBlank());
      } catch (com.apptolast.organization.domain.ValidationException
          | java.time.DateTimeException invalid) {
        throw new IllegalArgumentException("Invalid stored closure", invalid);
      }
    }
    if (action.equals("EXTEND")) {
      var extension = value.path("extension");
      require(extension.isObject());
      long minutes = number(extension, "additionalMinutes");
      var previous = checkedInstant(extension, "previousEndAt");
      var end = checkedInstant(extension, "effectiveEndAt");
      require(minutes >= 1 && minutes <= 1440 && !previous.isBefore(original.plannedEndAt()));
      var base = now.isAfter(previous) ? now : previous;
      require(base.plusSeconds(minutes * 60).equals(end));
    }
    if (action.equals("PAUSE")) require(running);
    if (action.equals("RESUME")) require(!running);
    long worked;
    try {
      worked =
          Math.addExact(
              before.workedMicroseconds(),
              running && !action.equals("EXTEND")
                  ? java.time.temporal.ChronoUnit.MICROS.between(before.runningSince(), now)
                  : 0);
    } catch (ArithmeticException invalid) {
      throw new IllegalArgumentException("Invalid stored work counter", invalid);
    }
    var expected =
        action.equals("EXTEND")
            ? new com.apptolast.organization.domain.WorkSessionState(
                original,
                before.status(),
                revision + 1,
                before.changedAt(),
                before.workedMicroseconds(),
                before.runningSince())
            : new com.apptolast.organization.domain.WorkSessionState(
                original,
                action.equals("CLOSE") ? "closed" : action.equals("PAUSE") ? "paused" : "running",
                revision + 1,
                now,
                worked,
                action.equals("RESUME") ? now : null);
    require(after.equals(expected));
  }

  private static com.apptolast.organization.domain.WorkSessionState readState(
      JsonNode value, com.apptolast.organization.domain.SessionStart original) {
    require(value.isObject() && value.has("runningSince"));
    return new com.apptolast.organization.domain.WorkSessionState(
        original,
        text(value, "status"),
        number(value, "revision"),
        checkedInstant(value, "changedAt"),
        number(value, "workedMicroseconds"),
        value.path("runningSince").isNull() ? null : checkedInstant(value, "runningSince"));
  }

  private static void original(
      JsonNode value, com.apptolast.organization.domain.SessionStart original) {
    require(
        value.path("id").asText().equals(original.id().toString())
            && value.path("projectId").asText().equals(original.projectId().toString())
            && value.path("taskId").asText().equals(original.taskId().toString())
            && checkedInstant(value, "startedAt").equals(original.startedAt())
            && number(value, "plannedMinutes") == original.plannedMinutes()
            && checkedInstant(value, "plannedEndAt").equals(original.plannedEndAt())
            && value.path("zoneId").asText().equals(original.zoneId()));
  }

  private void state(JsonGenerator out, JsonNode value) throws IOException {
    out.writeStartObject();
    out.writeObjectFieldStart("session");
    var session = value.path("session");
    fields(out, session, "id", "projectId", "taskId");
    instant(out, session, "startedAt");
    fields(out, session, "plannedMinutes");
    instant(out, session, "plannedEndAt");
    fields(out, session, "zoneId");
    out.writeEndObject();
    fields(out, value, "status");
    out.writeStringField("revision", value.path("revision").asText());
    instant(out, value, "changedAt");
    out.writeStringField("workedMicroseconds", value.path("workedMicroseconds").asText());
    if (value.path("runningSince").isNull()) out.writeNullField("runningSince");
    else instant(out, value, "runningSince");
    out.writeEndObject();
  }

  public void block(
      JsonGenerator out,
      String raw,
      UUID changeId,
      UUID blockId,
      UUID projectId,
      UUID taskId,
      String kind,
      long version,
      Instant occurredAt)
      throws IOException {
    var value = read(raw);
    require(java.util.Set.of("RESCHEDULED", "CANCELLED").contains(kind) && version > 0);
    require(value.has("after") && (kind.equals("CANCELLED") == value.path("after").isNull()));
    require(
        value.path("id").asText().equals(changeId.toString())
            && value.path("blockId").asText().equals(blockId.toString())
            && value.path("kind").asText().equals(kind)
            && value.path("version").isIntegralNumber()
            && value.path("version").canConvertToLong()
            && value.path("version").longValue() == version
            && checkedInstant(value, "occurredAt").equals(occurredAt));
    context(value.path("before"), blockId, projectId, taskId);
    if (!value.path("after").isNull()) context(value.path("after"), blockId, projectId, taskId);
    blockTypes(value.path("before"));
    if (!value.path("after").isNull()) {
      blockTypes(value.path("after"));
      require(
          checkedInstant(value.path("before"), "createdAt")
              .equals(checkedInstant(value.path("after"), "createdAt")));
    }
    out.writeStartObject();
    fields(out, value, "id", "blockId", "kind");
    out.writeStringField("version", value.path("version").asText());
    instant(out, value, "occurredAt");
    out.writeFieldName("before");
    blockSnapshot(out, value.path("before"));
    out.writeFieldName("after");
    blockSnapshot(out, value.path("after"));
    out.writeEndObject();
  }

  private JsonNode read(String raw) throws IOException {
    JsonNode value =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    require(value != null && value.isObject());
    return value;
  }

  private static void blockTypes(JsonNode value) {
    require(value.isObject());
    var request = value.path("request");
    var time = value.path("time");
    require(request.isObject() && time.isObject());
    text(request, "objective");
    text(request, "zoneId");
    text(request, "startLocal");
    text(request, "endLocal");
    require(request.path("allowOverBudget").isBoolean());
    number(time, "durationMinutes");
    text(time, "startOffset");
    text(time, "endOffset");
    for (var field : java.util.List.of("startOffset", "endOffset")) {
      require(request.has(field));
      if (!request.path(field).isNull()) text(request, field);
    }
    checkedInstant(value, "createdAt");
    var start = checkedInstant(time, "startAt");
    var end = checkedInstant(time, "endAt");
    long duration = number(time, "durationMinutes");
    require(duration >= 1 && duration <= 1440 && start.plusSeconds(duration * 60).equals(end));
    try {
      for (var endpoint : java.util.List.of("start", "end")) {
        var local = LocalDateTime.parse(text(request, endpoint + "Local"));
        require(
            local.getYear() >= 1
                && local.getYear() <= 9999
                && local.getSecond() == 0
                && local.getNano() == 0);
        var offset = java.time.ZoneOffset.of(text(time, endpoint + "Offset"));
        require(local.toInstant(offset).equals(endpoint.equals("start") ? start : end));
        if (!request.path(endpoint + "Offset").isNull()) {
          require(java.time.ZoneOffset.of(text(request, endpoint + "Offset")).equals(offset));
        }
      }
    } catch (java.time.DateTimeException invalid) {
      throw new IllegalArgumentException("Invalid stored block time", invalid);
    }
  }

  private static Instant checkedInstant(JsonNode value, String field) {
    try {
      var instant = Instant.parse(text(value, field));
      int year = instant.atOffset(java.time.ZoneOffset.UTC).getYear();
      require(year >= 1 && year <= 9999 && instant.getNano() % 1000 == 0);
      return instant;
    } catch (java.time.DateTimeException invalid) {
      throw new IllegalArgumentException("Invalid stored receipt time", invalid);
    }
  }

  private static String text(JsonNode value, String field) {
    require(value.path(field).isTextual());
    return value.path(field).textValue();
  }

  private static long number(JsonNode value, String field) {
    var number = value.path(field);
    require(number.isIntegralNumber() && number.canConvertToLong());
    return number.longValue();
  }

  private static void context(JsonNode value, UUID block, UUID project, UUID task) {
    require(
        value.path("id").asText().equals(block.toString())
            && value.path("projectId").asText().equals(project.toString())
            && value.path("taskId").asText().equals(task.toString()));
  }

  private static void require(boolean valid) {
    if (!valid) throw new IllegalArgumentException("Invalid stored export receipt");
  }

  private void blockSnapshot(JsonGenerator out, JsonNode value) throws IOException {
    if (value.isNull()) {
      out.writeNull();
      return;
    }
    out.writeStartObject();
    fields(out, value, "id", "projectId", "taskId");
    var request = value.path("request");
    out.writeObjectFieldStart("request");
    fields(out, request, "objective");
    for (var field : java.util.List.of("startLocal", "endLocal")) {
      out.writeStringField(field, LOCAL.format(LocalDateTime.parse(request.path(field).asText())));
    }
    fields(out, request, "zoneId", "startOffset", "endOffset", "allowOverBudget");
    out.writeEndObject();
    var time = value.path("time");
    out.writeObjectFieldStart("time");
    instant(out, time, "startAt");
    instant(out, time, "endAt");
    fields(out, time, "startOffset", "endOffset", "durationMinutes");
    out.writeEndObject();
    instant(out, value, "createdAt");
    out.writeEndObject();
  }

  private void fields(JsonGenerator out, JsonNode value, String... fields) throws IOException {
    for (var field : fields) {
      out.writeFieldName(field);
      json.writeTree(out, value.path(field));
    }
  }

  private static void instant(JsonGenerator out, JsonNode value, String field) throws IOException {
    out.writeStringField(field, INSTANT.format(checkedInstant(value, field)));
  }
}
