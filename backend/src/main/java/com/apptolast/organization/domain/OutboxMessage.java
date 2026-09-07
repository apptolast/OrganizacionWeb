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
            && !"BlockPlanned.v1".equals(type)
            && !"BlockChanged.v1".equals(type)
            && !"WorkSessionStarted.v1".equals(type)
            && !"WorkSessionStateChanged.v1".equals(type)
            && !"WorkSessionClosed.v1".equals(type))
        || schemaVersion != 1) return "UNSUPPORTED_EVENT";
    boolean taskStatusChanged = "TaskStatusChanged.v1".equals(type);
    boolean blockPlanned = "BlockPlanned.v1".equals(type);
    boolean blockChanged = "BlockChanged.v1".equals(type);
    boolean workSessionStarted = "WorkSessionStarted.v1".equals(type);
    boolean workSessionStateChanged = "WorkSessionStateChanged.v1".equals(type);
    boolean workSessionClosed = "WorkSessionClosed.v1".equals(type);
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
    if (blockChanged)
      expected =
          java.util.Set.of(
              "eventId",
              "aggregateId",
              "ownerId",
              "occurredAt",
              "schemaVersion",
              "type",
              "changeId",
              "blockId",
              "taskId",
              "kind",
              "revision",
              "before",
              "after");
    if (workSessionStarted)
      expected =
          java.util.Set.of(
              "eventId",
              "aggregateId",
              "ownerId",
              "occurredAt",
              "schemaVersion",
              "type",
              "projectId",
              "taskId",
              "plannedMinutes",
              "plannedEndAt",
              "zoneId");
    if (workSessionStateChanged)
      expected =
          java.util.Set.of(
              "eventId",
              "aggregateId",
              "ownerId",
              "occurredAt",
              "schemaVersion",
              "type",
              "action",
              "revision",
              "fromStatus",
              "toStatus",
              "workedMicroseconds",
              "runningSince");
    if (workSessionClosed)
      expected =
          java.util.Set.of(
              "eventId",
              "aggregateId",
              "ownerId",
              "occurredAt",
              "schemaVersion",
              "type",
              "revision",
              "fromStatus",
              "workedMicroseconds",
              "workDate",
              "closeZoneId");
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
    if (workSessionClosed) {
      if (eventId.equals(aggregateId)) return "INVALID_EVENT";
      if (!(payload.get("closeZoneId") instanceof String zone) || zone.isBlank())
        return "INVALID_EVENT";
      if (!(payload.get("workDate") instanceof String date)
          || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return "INVALID_EVENT";
      try {
        if (java.time.LocalDate.parse(date).getYear() < 1) return "INVALID_EVENT";
      } catch (java.time.DateTimeException invalid) {
        return "INVALID_EVENT";
      }
      if (!workSessionTimestamp(timestamp)
          || occurredAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))) return "INVALID_EVENT";
      if (!(payload.get("workedMicroseconds") instanceof String worked)
          || !worked.matches("0|[1-9][0-9]*")) return "INVALID_EVENT";
      if (!(payload.get("revision") instanceof String revision) || !revision.matches("[1-9][0-9]*"))
        return "INVALID_EVENT";
      try {
        Long.parseLong(revision);
      } catch (NumberFormatException invalid) {
        return "INVALID_EVENT";
      }
      if (!"running".equals(payload.get("fromStatus"))
          && !"paused".equals(payload.get("fromStatus"))) return "INVALID_EVENT";
      return null;
    }
    if (workSessionStateChanged) {
      if (eventId.equals(aggregateId)) return "INVALID_EVENT";
      if (!workSessionTimestamp(timestamp)
          || occurredAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))) return "INVALID_EVENT";
      if (!"PAUSE".equals(payload.get("action")) && !"RESUME".equals(payload.get("action")))
        return "INVALID_EVENT";
      boolean pause = "PAUSE".equals(payload.get("action"));
      if (!(pause ? "running" : "paused").equals(payload.get("fromStatus"))) return "INVALID_EVENT";
      if (!(pause ? "paused" : "running").equals(payload.get("toStatus"))) return "INVALID_EVENT";
      if (pause && payload.get("runningSince") != null) return "INVALID_EVENT";
      if (!pause) {
        if (!(payload.get("runningSince") instanceof String since) || !workSessionTimestamp(since))
          return "INVALID_EVENT";
        try {
          if (!occurredAt.equals(Instant.parse(since))) return "INVALID_EVENT";
        } catch (java.time.DateTimeException invalid) {
          return "INVALID_EVENT";
        }
      }
      if (!(payload.get("workedMicroseconds") instanceof String worked)
          || !worked.matches("0|[1-9][0-9]*")) return "INVALID_EVENT";
      if (!(payload.get("revision") instanceof String revision) || !revision.matches("[1-9][0-9]*"))
        return "INVALID_EVENT";
      try {
        Long.parseLong(revision);
      } catch (NumberFormatException invalid) {
        return "INVALID_EVENT";
      }
      return null;
    }
    if (workSessionStarted) {
      if (eventId.equals(aggregateId)) return "INVALID_EVENT";
      for (var field : java.util.List.of("projectId", "taskId")) {
        if (!(payload.get(field) instanceof String id)
            || !id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
          return "INVALID_EVENT";
      }
      if (!(payload.get("plannedMinutes") instanceof Integer minutes)
          || !(payload.get("zoneId") instanceof String zone)
          || zone.isBlank()
          || minutes < 1
          || minutes > 1440
          || !(payload.get("plannedEndAt") instanceof String end)
          || !workSessionTimestamp(timestamp)
          || !workSessionTimestamp(end)
          || occurredAt.getNano() % 1000 != 0
          || occurredAt.isBefore(Instant.parse("0001-01-01T00:00:00Z"))) return "INVALID_EVENT";
      try {
        var plannedEnd = Instant.parse(end);
        return plannedEnd.isBefore(Instant.parse("+10000-01-01T00:00:00Z"))
                && occurredAt.plusSeconds(minutes * 60L).equals(plannedEnd)
            ? null
            : "INVALID_EVENT";
      } catch (java.time.DateTimeException error) {
        return "INVALID_EVENT";
      }
    }
    if (blockChanged) {
      for (var field : java.util.List.of("changeId", "blockId", "taskId")) {
        if (!(payload.get(field) instanceof String id)
            || !id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
          return "INVALID_EVENT";
      }
      if (eventId.equals(UUID.fromString((String) payload.get("changeId")))) return "INVALID_EVENT";
      var revision = payload.get("revision");
      if (!(revision instanceof Integer || revision instanceof Long)
          || ((Number) revision).longValue() < 1) return "INVALID_EVENT";
      if (!(payload.get("kind") instanceof String kind)
          || !(kind.equals("RESCHEDULED") || kind.equals("CANCELLED"))
          || (kind.equals("CANCELLED")
              ? payload.get("after") != null
              : payload.get("after") == null)) return "INVALID_EVENT";
      return validChangedInterval(payload.get("before"))
              && (kind.equals("CANCELLED") || validChangedInterval(payload.get("after")))
          ? null
          : "INVALID_EVENT";
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

  private static boolean validChangedInterval(Object value) {
    if (!(value instanceof Map<?, ?> interval)
        || !interval
            .keySet()
            .equals(java.util.Set.of("startAt", "endAt", "zoneId", "durationMinutes"))
        || !(interval.get("zoneId") instanceof String zone)
        || zone.isBlank()
        || !(interval.get("startAt") instanceof String start)
        || !(interval.get("endAt") instanceof String end)
        || !(interval.get("durationMinutes") instanceof Integer minutes)) return false;
    try {
      new ResolvedBlockTime(
          Instant.parse(start),
          Instant.parse(end),
          java.time.ZoneOffset.UTC,
          java.time.ZoneOffset.UTC,
          minutes);
      return true;
    } catch (java.time.DateTimeException | IllegalArgumentException error) {
      return false;
    }
  }

  private static boolean workSessionTimestamp(String value) {
    return value.matches(
        "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{1,6})?Z");
  }
}
