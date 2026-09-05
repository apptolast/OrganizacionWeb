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
    if (!"ProjectCreated.v1".equals(type) || schemaVersion != 1) return "UNSUPPORTED_EVENT";
    if (!java.util.Set.of(
                "eventId", "aggregateId", "ownerId", "occurredAt", "schemaVersion", "name", "type")
            .equals(payload.keySet())
        || !eventId.toString().equals(payload.get("eventId"))
        || !aggregateId.toString().equals(payload.get("aggregateId"))
        || !ownerId.equals(payload.get("ownerId"))
        || !type.equals(payload.get("type"))
        || !Integer.valueOf(1).equals(payload.get("schemaVersion"))
        || !(payload.get("occurredAt") instanceof String timestamp)
        || !(payload.get("name") instanceof String name)) return "INVALID_EVENT";
    try {
      if (!occurredAt.equals(Instant.parse(timestamp))) return "INVALID_EVENT";
    } catch (java.time.format.DateTimeParseException error) {
      return "INVALID_EVENT";
    }
    if (name.isEmpty()
        || name.codePointCount(0, name.length()) > 120
        || !name.equals(name.replaceAll("(?U)^\\s+|\\s+$", ""))) return "INVALID_EVENT";
    return null;
  }
}
