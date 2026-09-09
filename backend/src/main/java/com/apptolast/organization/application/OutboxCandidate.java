package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

/**
 * One outbox row seen from the webhook side. It is read-only: enqueuing never changes the row's
 * status, attempts, published_at nor payload.
 */
public record OutboxCandidate(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    String status,
    int schemaVersion,
    String payload,
    boolean payloadValid) {
  private static final String BLOCKED = "blocked";
  private static final int SUPPORTED_SCHEMA_VERSION = 1;

  public boolean isBlocked() {
    return BLOCKED.equals(status);
  }

  public boolean isSupported() {
    return schemaVersion == SUPPORTED_SCHEMA_VERSION;
  }
}
