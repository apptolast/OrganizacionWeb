package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Task(
    UUID id,
    UUID projectId,
    String title,
    String completionCriterion,
    Integer estimatedMinutes,
    String status,
    Instant createdAt,
    Instant updatedAt) {
  public Task {
    if (id == null
        || projectId == null
        || !"pending".equals(status)
        || createdAt == null
        || updatedAt == null
        || updatedAt.isBefore(createdAt))
      throw new IllegalArgumentException("A task requires valid identity, state and timestamps");
    title = title == null ? "" : title.replaceAll("(?U)^\\s+|\\s+$", "");
    if (title.isEmpty())
      throw new ValidationException(
          List.of(new FieldError("title", "REQUIRED", "Escribe un título para la tarea.")));
    if (estimatedMinutes != null && (estimatedMinutes < 1 || estimatedMinutes > 1440))
      throw new ValidationException(
          List.of(
              new FieldError(
                  "estimatedMinutes",
                  "OUT_OF_RANGE",
                  "La estimación debe estar entre 1 y 1440 minutos.")));
    completionCriterion = completionCriterion == null ? "" : completionCriterion;
    if (title.codePointCount(0, title.length()) > 160)
      throw new ValidationException(
          List.of(new FieldError("title", "TOO_LONG", "El título admite hasta 160 caracteres.")));
    if (completionCriterion.codePointCount(0, completionCriterion.length()) > 2000)
      throw new ValidationException(
          List.of(
              new FieldError(
                  "completionCriterion", "TOO_LONG", "El criterio admite hasta 2000 caracteres.")));
  }

  public static Task create(
      UUID id, UUID projectId, String title, String criterion, Integer minutes, Instant now) {
    return new Task(id, projectId, title, criterion, minutes, "pending", now, now);
  }
}
