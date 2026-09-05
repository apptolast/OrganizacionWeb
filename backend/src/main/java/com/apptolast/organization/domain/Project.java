package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record Project(
    UUID id,
    String ownerId,
    String name,
    String description,
    String status,
    Instant createdAt,
    Instant updatedAt) {
  public static Project create(
      UUID id, String ownerId, String name, String description, Instant now) {
    return new Project(id, ownerId, name, description, "idea", now, now);
  }

  public Project {
    if (id == null
        || ownerId == null
        || ownerId.isBlank()
        || !"idea".equals(status)
        || createdAt == null
        || updatedAt == null
        || updatedAt.isBefore(createdAt)) {
      throw new IllegalArgumentException("A project requires valid identity, state and timestamps");
    }
    String normalized = name == null ? "" : name.replaceAll("(?U)^\\s+|\\s+$", "");
    if (normalized.isEmpty()) {
      throw new ValidationException(
          java.util.List.of(new FieldError("name", "REQUIRED", "Escribe un nombre.")));
    }
    if (normalized.codePointCount(0, normalized.length()) > 120) {
      throw new ValidationException(
          java.util.List.of(
              new FieldError("name", "TOO_LONG", "El nombre admite hasta 120 caracteres.")));
    }
    if (description != null && description.codePointCount(0, description.length()) > 4000) {
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  "description", "TOO_LONG", "La descripción admite hasta 4000 caracteres.")));
    }
    name = normalized;
    description = description == null ? "" : description;
  }
}
