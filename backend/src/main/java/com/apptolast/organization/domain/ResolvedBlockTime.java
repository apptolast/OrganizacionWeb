package com.apptolast.organization.domain;

import java.time.*;
import java.util.Set;

public record ResolvedBlockTime(
    Instant startAt,
    Instant endAt,
    ZoneOffset startOffset,
    ZoneOffset endOffset,
    int durationMinutes) {
  public ResolvedBlockTime {
    if (startAt == null
        || endAt == null
        || startOffset == null
        || endOffset == null
        || durationMinutes < 1
        || durationMinutes > 1440
        || startAt.getNano() != 0
        || endAt.getNano() != 0
        || !Duration.between(startAt, endAt).equals(Duration.ofMinutes(durationMinutes))
        || startAt.atOffset(ZoneOffset.UTC).getYear() < 1
        || startAt.atOffset(ZoneOffset.UTC).getYear() > 9999
        || endAt.atOffset(ZoneOffset.UTC).getYear() < 1
        || endAt.atOffset(ZoneOffset.UTC).getYear() > 9999)
      throw new IllegalArgumentException("Invalid resolved block time");
  }

  public static ResolvedBlockTime resolve(BlockRequest request, Set<String> catalog, Instant now) {
    if (!catalog.contains(request.zoneId()))
      throw new ValidationException(
          java.util.List.of(
              new FieldError("zoneId", "INVALID_VALUE", "Elige una zona del catálogo vigente.")));
    var zone = ZoneId.of(request.zoneId());
    var startOffset = offset(zone, request.startLocal(), request.startOffset(), "start");
    var endOffset = offset(zone, request.endLocal(), request.endOffset(), "end");
    var start = request.startLocal().toInstant(startOffset);
    var end = request.endLocal().toInstant(endOffset);
    checkYear(start, "startLocal");
    checkYear(end, "endLocal");
    var seconds = Duration.between(start, end).getSeconds();
    if (seconds < 60 || seconds > 86400 || seconds % 60 != 0)
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  "endLocal",
                  "OUT_OF_RANGE",
                  "El bloque debe durar de 1 a 1440 minutos enteros.")));
    if (start.isBefore(now))
      throw new ValidationException(
          java.util.List.of(
              new FieldError("startLocal", "IN_PAST", "El inicio no puede estar en el pasado.")));
    return new ResolvedBlockTime(start, end, startOffset, endOffset, (int) (seconds / 60));
  }

  private static ZoneOffset offset(
      ZoneId zone, LocalDateTime local, ZoneOffset requested, String endpoint) {
    var offsets = zone.getRules().getValidOffsets(local);
    if (offsets.isEmpty())
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  endpoint + "Local",
                  "NONEXISTENT_LOCAL_TIME",
                  "Esta hora no existe en la zona elegida.")));
    if (requested == null && offsets.size() > 1)
      throw new BlockOffsetException(
          new FieldError(
              endpoint + "Offset",
              "AMBIGUOUS_OFFSET",
              "Elige una de las ocurrencias de esta hora."),
          offsets);
    if (requested != null && !offsets.contains(requested))
      throw new BlockOffsetException(
          new FieldError(
              endpoint + "Offset",
              "INVALID_OFFSET",
              "El desplazamiento no corresponde a esta hora y zona."),
          offsets);
    return requested == null ? offsets.getFirst() : requested;
  }

  private static void checkYear(Instant value, String field) {
    int year = value.atOffset(ZoneOffset.UTC).getYear();
    if (year < 1 || year > 9999)
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  field,
                  "OUT_OF_RANGE",
                  "La fecha resuelta debe permanecer entre los años 0001 y 9999.")));
  }
}
