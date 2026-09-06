package com.apptolast.organization.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record BlockRequest(
    String objective,
    LocalDateTime startLocal,
    LocalDateTime endLocal,
    String zoneId,
    ZoneOffset startOffset,
    ZoneOffset endOffset,
    boolean allowOverBudget) {
  public BlockRequest {
    objective = objective == null ? "" : objective.replaceAll("(?U)^\\s+|\\s+$", "");
    if (objective.isEmpty())
      throw new ValidationException(
          java.util.List.of(
              new FieldError("objective", "REQUIRED", "Escribe un objetivo para el bloque.")));
    if (objective.codePointCount(0, objective.length()) > 500)
      throw new ValidationException(
          java.util.List.of(
              new FieldError("objective", "TOO_LONG", "El objetivo admite hasta 500 caracteres.")));
    validateDestination(startLocal, endLocal, zoneId);
  }

  static void validateDestination(LocalDateTime startLocal, LocalDateTime endLocal, String zoneId) {
    validateLocal(startLocal, "startLocal");
    validateLocal(endLocal, "endLocal");
    if (zoneId == null || zoneId.isBlank())
      throw new ValidationException(
          java.util.List.of(new FieldError("zoneId", "INVALID_VALUE", "Elige una zona válida.")));
  }

  private static void validateLocal(LocalDateTime local, String field) {
    if (local == null
        || local.getYear() < 1
        || local.getYear() > 9999
        || local.getSecond() != 0
        || local.getNano() != 0)
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  field,
                  "INVALID_FORMAT",
                  "Introduce una fecha y hora con precisión de minutos entre 0001 y 9999.")));
  }
}
