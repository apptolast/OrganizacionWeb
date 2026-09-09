package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/** One attempt log for one event towards one endpoint; never carries the body nor the secret. */
public record WebhookDelivery(
    UUID id,
    UUID eventId,
    String eventType,
    String status,
    int attempt,
    Integer httpStatus,
    Integer latencyMs,
    String errorClass,
    Instant nextAttemptAt,
    Instant createdAt,
    Instant updatedAt) {
  public static final String PENDING = "pending";
  public static final String SUCCEEDED = "succeeded";
  public static final String EXHAUSTED = "exhausted";
  public static final String PING = "webhook.ping.v1";

  /** A ping is its own event: the delivery id doubles as the event id. */
  public static WebhookDelivery ping(UUID id, Instant now) {
    return new WebhookDelivery(id, id, PING, PENDING, 0, null, null, null, now, now, now);
  }

  public boolean isPending() {
    return PENDING.equals(status);
  }

  public boolean isTerminal() {
    return SUCCEEDED.equals(status) || EXHAUSTED.equals(status);
  }

  /** Reopens a terminal delivery keeping id, event and body, and clearing the previous outcome. */
  public WebhookDelivery requeued(Instant now) {
    return new WebhookDelivery(
        id, eventId, eventType, PENDING, 0, null, null, null, now, createdAt, now);
  }
}
