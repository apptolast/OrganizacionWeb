package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.domain.Project;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

final class ImportRecordValidator {
  private ImportRecordValidator() {}

  static void row(String collection, JsonNode row, String owner) {
    String fields =
        switch (collection) {
          case "projects" -> "id,name,description,status,version,createdAt,updatedAt";
          case "tasks" ->
              "id,projectId,parentId,title,completionCriterion,estimatedMinutes,status,version,completedAt,createdAt,updatedAt";
          case "taskStatusHistory" ->
              "id,projectId,taskId,taskVersion,fromStatus,toStatus,occurredAt";
          case "availability" ->
              "id,zoneId,mondayMinutes,tuesdayMinutes,wednesdayMinutes,thursdayMinutes,fridayMinutes,saturdayMinutes,sundayMinutes,version,createdAt,updatedAt";
          case "plannedBlocks" ->
              "id,projectId,taskId,requestKey,objective,startLocal,endLocal,zoneId,startOffset,endOffset,allowOverBudget,startAt,endAt,durationMinutes,createdAt";
          case "blockProjections" ->
              "blockId,version,status,updatedAt,startLocal,endLocal,zoneId,startOffset,endOffset,startAt,endAt,durationMinutes";
          case "blockChanges" ->
              "id,projectId,taskId,blockId,requestKey,kind,version,occurredAt,receipt";
          case "workSessions" ->
              "id,projectId,taskId,requestKey,startedAt,plannedMinutes,plannedEndAt,zoneId,status,revision,changedAt,workedMicroseconds,runningSince,effectiveEndAt,lastDecisionAt";
          case "workSessionIntervals" -> "sessionId,revision,startAt,endAt";
          case "workSessionChanges" ->
              "id,sessionId,requestKey,action,expectedRevision,occurredAt,receipt";
          case "appearance" -> "id,theme,accentLight,accentDark,version,updatedAt";
          case "customization" -> "id,scope,visibleFields,customFields,version,updatedAt";
          case "projectCustomFieldValues" -> "id,projectId,values,version,updatedAt";
          case "taskCustomFieldValues" -> "id,projectId,taskId,values,version,updatedAt";
          default -> throw new ImportInvalidFileException();
        };
    var expected = java.util.Set.of(fields.split(","));
    if (!row.isObject() || row.size() != expected.size()) throw new ImportInvalidFileException();
    for (var field : expected) if (!row.has(field)) throw new ImportInvalidFileException();
    try {
      for (var field : expected) {
        var value = row.get(field);
        boolean nullable =
            field.equals("parentId")
                || field.equals("estimatedMinutes")
                || field.equals("completedAt")
                || collection.equals("blockProjections")
                    && java.util.Set.of(
                            "startLocal",
                            "endLocal",
                            "zoneId",
                            "startOffset",
                            "endOffset",
                            "startAt",
                            "endAt",
                            "durationMinutes")
                        .contains(field)
                || collection.equals("workSessions")
                    && java.util.Set.of(
                            "changedAt", "runningSince", "effectiveEndAt", "lastDecisionAt")
                        .contains(field);
        if (value.isNull()) {
          if (!nullable) throw new ImportInvalidFileException();
          continue;
        }
        if (field.equals("id")
            || field.endsWith("Id") && !field.equals("zoneId")
            || field.equals("requestKey")) {
          var raw = text(row, field);
          if (!UUID.fromString(raw).toString().equals(raw)) throw new ImportInvalidFileException();
        } else if (java.util.Set.of(
                "version", "revision", "taskVersion", "expectedRevision", "workedMicroseconds")
            .contains(field)) {
          var raw = text(row, field);
          if (!raw.matches("0|[1-9][0-9]*") || Long.parseLong(raw) < 0)
            throw new ImportInvalidFileException();
        } else if (field.endsWith("At") || field.equals("runningSince")) {
          var raw = text(row, field);
          var instant = Instant.parse(raw);
          int year = instant.atOffset(java.time.ZoneOffset.UTC).getYear();
          if (!raw.endsWith("Z") || year < 1 || year > 9999 || instant.getNano() % 1000 != 0)
            throw new ImportInvalidFileException();
        } else if (field.endsWith("Local")) {
          var local = java.time.LocalDateTime.parse(text(row, field));
          if (local.getYear() < 1
              || local.getYear() > 9999
              || local.getSecond() != 0
              || local.getNano() != 0) throw new ImportInvalidFileException();
        } else if (field.endsWith("Offset")) {
          java.time.ZoneOffset.of(text(row, field));
        } else if (field.endsWith("Minutes")) {
          if (!value.isNumber()) throw new ImportInvalidFileException();
          int minutes = value.decimalValue().intValueExact();
          if (minutes < 0) throw new ImportInvalidFileException();
        } else if (field.equals("allowOverBudget")) {
          if (!value.isBoolean()) throw new ImportInvalidFileException();
        } else if (!java.util.Set.of("receipt", "values", "customFields", "visibleFields")
            .contains(field)) {
          text(row, field);
        }
      }
      if (collection.equals("projects")) project(row, owner);
      if (collection.equals("tasks")) {
        task(row);
        boolean completed = text(row, "status").equals("completed");
        if (completed
                && (row.get("completedAt").isNull()
                    || !Instant.parse(text(row, "completedAt"))
                        .equals(Instant.parse(text(row, "updatedAt"))))
            || !completed && !row.get("completedAt").isNull())
          throw new ImportInvalidFileException();
      }
      if (collection.equals("taskStatusHistory") && Long.parseLong(text(row, "taskVersion")) == 0)
        throw new ImportInvalidFileException();
      if (java.util.Set.of("blockProjections", "blockChanges").contains(collection)
          && Long.parseLong(text(row, "version")) == 0) throw new ImportInvalidFileException();
      if (collection.equals("availability")) {
        var budgets =
            new java.util.EnumMap<java.time.DayOfWeek, Integer>(java.time.DayOfWeek.class);
        for (var day : java.time.DayOfWeek.values())
          budgets.put(
              day,
              row.get(day.name().toLowerCase(java.util.Locale.ROOT) + "Minutes")
                  .decimalValue()
                  .intValueExact());
        new com.apptolast.organization.domain.Availability(
            UUID.fromString(text(row, "id")),
            owner,
            text(row, "zoneId"),
            budgets,
            Long.parseLong(text(row, "version")),
            Instant.parse(text(row, "createdAt")),
            Instant.parse(text(row, "updatedAt")));
      }
      if (collection.equals("plannedBlocks")) {
        var objective = text(row, "objective");
        if (objective.codePointCount(0, objective.length()) > 500)
          throw new ImportInvalidFileException();
        new com.apptolast.organization.domain.BlockRequest(
            objective,
            java.time.LocalDateTime.parse(text(row, "startLocal")),
            java.time.LocalDateTime.parse(text(row, "endLocal")),
            text(row, "zoneId"),
            java.time.ZoneOffset.of(text(row, "startOffset")),
            java.time.ZoneOffset.of(text(row, "endOffset")),
            row.get("allowOverBudget").booleanValue());
        blockTime(row);
      }
      if (collection.equals("blockProjections")) {
        var state = text(row, "status");
        if (!java.util.Set.of("planned", "cancelled").contains(state))
          throw new ImportInvalidFileException();
        int present = 0;
        for (var field :
            java.util.List.of(
                "startLocal",
                "endLocal",
                "zoneId",
                "startOffset",
                "endOffset",
                "startAt",
                "endAt",
                "durationMinutes")) if (!row.get(field).isNull()) present++;
        if (present != 0 && present != 8) throw new ImportInvalidFileException();
        if (present == 8) {
          if (text(row, "zoneId").isBlank()) throw new ImportInvalidFileException();
          blockTime(row);
        }
      }
      if (collection.equals("workSessions")) {
        var state = text(row, "status");
        if (!java.util.Set.of("running", "paused", "closed").contains(state))
          throw new ImportInvalidFileException();
        int minutes = row.get("plannedMinutes").decimalValue().intValueExact();
        if (minutes < 1
            || minutes > 1440
            || text(row, "zoneId").isBlank()
            || !Instant.parse(text(row, "startedAt"))
                .plusSeconds(minutes * 60L)
                .equals(Instant.parse(text(row, "plannedEndAt"))))
          throw new ImportInvalidFileException();
        if (!state.equals("running") && !row.get("runningSince").isNull())
          throw new ImportInvalidFileException();
        if (!row.get("effectiveEndAt").isNull()
            && Instant.parse(text(row, "effectiveEndAt"))
                .isBefore(Instant.parse(text(row, "plannedEndAt"))))
          throw new ImportInvalidFileException();
      }
      if (collection.equals("taskStatusHistory")
          && (!java.util.Set.of("pending", "completed").contains(text(row, "fromStatus"))
              || !java.util.Set.of("pending", "completed").contains(text(row, "toStatus"))
              || text(row, "fromStatus").equals(text(row, "toStatus"))))
        throw new ImportInvalidFileException();
    } catch (IllegalArgumentException
        | ArithmeticException
        | java.time.DateTimeException
        | com.apptolast.organization.domain.ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }

  private static void blockTime(JsonNode row) {
    var start = Instant.parse(text(row, "startAt"));
    var end = Instant.parse(text(row, "endAt"));
    int minutes = row.get("durationMinutes").decimalValue().intValueExact();
    if (minutes <= 0
        || minutes > 1440
        || !start.plusSeconds(minutes * 60L).equals(end)
        || !java.time.LocalDateTime.parse(text(row, "startLocal"))
            .toInstant(java.time.ZoneOffset.of(text(row, "startOffset")))
            .equals(start)
        || !java.time.LocalDateTime.parse(text(row, "endLocal"))
            .toInstant(java.time.ZoneOffset.of(text(row, "endOffset")))
            .equals(end)) throw new ImportInvalidFileException();
  }

  static void project(JsonNode row, String owner) {
    try {
      var name = text(row, "name");
      if (name.codePointCount(0, name.length()) > 120) throw new ImportInvalidFileException();
      new Project(
          UUID.fromString(text(row, "id")),
          owner,
          text(row, "name"),
          text(row, "description"),
          text(row, "status"),
          Instant.parse(text(row, "createdAt")),
          Instant.parse(text(row, "updatedAt")));
    } catch (IllegalArgumentException
        | java.time.DateTimeException
        | com.apptolast.organization.domain.ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }

  private static String text(JsonNode row, String field) {
    var value = row.get(field);
    if (value == null || !value.isTextual()) throw new ImportInvalidFileException();
    return value.textValue();
  }

  static void task(JsonNode row) {
    try {
      var title = text(row, "title");
      if (title.codePointCount(0, title.length()) > 160) throw new ImportInvalidFileException();
      var estimate = row.get("estimatedMinutes");
      if (estimate == null || (!estimate.isNull() && !estimate.isNumber()))
        throw new ImportInvalidFileException();
      Integer minutes = estimate.isNull() ? null : estimate.decimalValue().intValueExact();
      new com.apptolast.organization.domain.Task(
          UUID.fromString(text(row, "id")),
          UUID.fromString(text(row, "projectId")),
          title,
          text(row, "completionCriterion"),
          minutes,
          text(row, "status"),
          Instant.parse(text(row, "createdAt")),
          Instant.parse(text(row, "updatedAt")));
    } catch (IllegalArgumentException
        | ArithmeticException
        | java.time.DateTimeException
        | com.apptolast.organization.domain.ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }
}
