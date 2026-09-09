package com.apptolast.organization.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rango semiabierto [from, to) en UTC. Ambos extremos son obligatorios, se escriben en el formato de
 * instante de la aplicación y no pueden abarcar más de dieciséis días.
 */
public record ExternalEventsRange(Instant from, Instant to) {
  public static final Duration MAX = Duration.ofDays(16);
  private static final String UTC_INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

  public static ExternalEventsRange of(String from, String to) {
    var errors = new ArrayList<FieldError>();
    var start = instant(from, "from", errors);
    var end = instant(to, "to", errors);
    if (errors.isEmpty() && !isWithinLimits(start, end))
      errors.add(
          new FieldError(
              "to",
              "OUT_OF_RANGE",
              "El fin debe ser posterior al inicio y no abarcar más de dieciséis días."));
    if (!errors.isEmpty()) throw new ValidationException(errors);
    return new ExternalEventsRange(start, end);
  }

  private static boolean isWithinLimits(Instant start, Instant end) {
    return end.isAfter(start) && !Duration.between(start, end).minus(MAX).isPositive();
  }

  private static Instant instant(String value, String field, List<FieldError> errors) {
    if (value == null || value.isEmpty()) {
      errors.add(new FieldError(field, "REQUIRED", "Indica el instante en UTC."));
      return null;
    }
    if (!value.matches(UTC_INSTANT)) {
      errors.add(new FieldError(field, "INVALID_FORMAT", "Usa el formato 2030-01-07T00:00:00Z."));
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException malformed) {
      errors.add(new FieldError(field, "INVALID_FORMAT", "Usa el formato 2030-01-07T00:00:00Z."));
      return null;
    }
  }
}
