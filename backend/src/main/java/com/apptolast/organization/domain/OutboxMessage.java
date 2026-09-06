package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxMessage(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    String type,
    int schemaVersion,
    String json,
    Map<String, Object> payload,
    long attempts) {
  public OutboxMessage {
    payload = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(payload));
  }

  public String validationCode() {
    if ((!"ProjectCreated.v1".equals(type)
            && !"ProjectUpdated.v1".equals(type)
            && !"ProjectStatusChanged.v1".equals(type)
            && !"TaskCreated.v1".equals(type)
            && !"SubtaskCreated.v1".equals(type)
            && !"TaskStatusChanged.v1".equals(type)
            && !"BlockPlanned.v1".equals(type))
        || schemaVersion != 1) return "UNSUPPORTED_EVENT";
    boolean taskStatusChanged = "TaskStatusChanged.v1".equals(type);
    boolean blockPlanned = "BlockPlanned.v1".equals(type);
    boolean statusChanged = "ProjectStatusChanged.v1".equals(type);
    boolean subtaskCreated = "SubtaskCreated.v1".equals(type);
    boolean taskCreated = "TaskCreated.v1".equals(type) || subtaskCreated;
    var expected =
        taskCreated
            ? java.util.Set.of(
                "eventId",
                "aggregateId",
                "ownerId",
                "occurredAt",
                "schemaVersion",
                "type",
                "taskId",
                "title")
            : (statusChanged || taskStatusChanged)
                ? java.util.Set.of(
                    "eventId",
                    "aggregateId",
                    "ownerId",
                    "occurredAt",
                    "schemaVersion",
                    "type",
                    "fromStatus",
                    "toStatus")
                : java.util.Set.of(
                    "eventId",
                    "aggregateId",
                    "ownerId",
                    "occurredAt",
                    "schemaVersion",
                    "name",
                    "type");
    if (taskStatusChanged) {
      expected = new java.util.HashSet<>(expected);
      expected.add("taskId");
    }
    if (subtaskCreated) {
      expected = new java.util.HashSet<>(expected);
      expected.add("parentTaskId");
    }
    if (blockPlanned)
      expected =
          java.util.Set.of(
              "eventId",
              "aggregateId",
              "ownerId",
              "occurredAt",
              "schemaVersion",
              "type",
              "blockId",
              "taskId",
              "startAt",
              "endAt",
              "zoneId",
              "durationMinutes");
    if (!expected.equals(payload.keySet())
        || !eventId.toString().equals(payload.get("eventId"))
        || !aggregateId.toString().equals(payload.get("aggregateId"))
        || !ownerId.equals(payload.get("ownerId"))
        || !type.equals(payload.get("type"))
        || !Integer.valueOf(1).equals(payload.get("schemaVersion"))
        || !(payload.get("occurredAt") instanceof String timestamp)) return "INVALID_EVENT";
    try {
      if (!occurredAt.equals(Instant.parse(timestamp))) return "INVALID_EVENT";
    } catch (java.time.format.DateTimeParseException error) {
      return "INVALID_EVENT";
    }
    if (blockPlanned) {
      if (!(payload.get("blockId") instanceof String blockId)
          || !blockId.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
          || !(payload.get("taskId") instanceof String taskId)
          || !taskId.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
          || !(payload.get("zoneId") instanceof String zone)
          || zone.isBlank()
          || !(payload.get("startAt") instanceof String start)
          || !(payload.get("endAt") instanceof String end)
          || !(payload.get("durationMinutes") instanceof Integer minutes)
          || minutes < 1
          || minutes > 1440) return "INVALID_EVENT";
      try {
        new ResolvedBlockTime(
            Instant.parse(start),
            Instant.parse(end),
            java.time.ZoneOffset.UTC,
            java.time.ZoneOffset.UTC,
            minutes);
        return null;
      } catch (java.time.DateTimeException | IllegalArgumentException error) {
        return "INVALID_EVENT";
      }
    }
    if ((taskCreated || taskStatusChanged)
        && (!(payload.get("taskId") instanceof String taskId)
            || !taskId.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
      return "INVALID_EVENT";
    if (subtaskCreated
        && (!(payload.get("parentTaskId") instanceof String parentId)
            || !parentId.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            || UUID.fromString(parentId).equals(UUID.fromString((String) payload.get("taskId")))))
      return "INVALID_EVENT";
    if (taskStatusChanged) {
      return payload.get("fromStatus") instanceof String from
              && payload.get("toStatus") instanceof String to
              && java.util.Set.of("pending", "completed").contains(from)
              && java.util.Set.of("pending", "completed").contains(to)
              && !from.equals(to)
          ? null
          : "INVALID_EVENT";
    }
    if (statusChanged) {
      return payload.get("fromStatus") instanceof String from
              && payload.get("toStatus") instanceof String to
              && ProjectStates.allows(from, to)
          ? null
          : "INVALID_EVENT";
    }
    if (!(payload.get(taskCreated ? "title" : "name") instanceof String name))
      return "INVALID_EVENT";
    if (name.isEmpty()
        || name.codePointCount(0, name.length()) > (taskCreated ? 160 : 120)
        || !name.equals(name.replaceAll("(?U)^\\s+|\\s+$", ""))) return "INVALID_EVENT";
    return null;
  }
}
