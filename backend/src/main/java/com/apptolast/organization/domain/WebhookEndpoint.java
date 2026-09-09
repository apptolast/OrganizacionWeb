package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookEndpoint(
    UUID id,
    String url,
    String description,
    List<String> eventTypes,
    String status,
    String disabledReason,
    Instant disabledAt,
    Instant createdAt,
    Instant updatedAt) {
  public static final String ACTIVE = "active";
  public static final String DISABLED = "disabled";
  public static final String MANUAL = "MANUAL";
  public static final String DELIVERY_EXHAUSTED = "DELIVERY_EXHAUSTED";

  public WebhookEndpoint {
    eventTypes = List.copyOf(eventTypes);
  }

  public static WebhookEndpoint active(UUID id, WebhookIntent intent, Instant now) {
    return new WebhookEndpoint(
        id, intent.url(), intent.description(), intent.eventTypes(), ACTIVE, null, null, now, now);
  }

  public boolean isActive() {
    return ACTIVE.equals(status);
  }

  /** Binary and idempotent: repeating the current status changes nothing, not even updatedAt. */
  public WebhookEndpoint withStatus(String target, Instant now) {
    if (target.equals(status)) return this;
    return DISABLED.equals(target) ? disabled(MANUAL, now) : reactivated(now);
  }

  public WebhookEndpoint disabledByExhaustion(Instant now) {
    return isActive() ? disabled(DELIVERY_EXHAUSTED, now) : this;
  }

  private WebhookEndpoint disabled(String reason, Instant now) {
    return new WebhookEndpoint(
        id, url, description, eventTypes, DISABLED, reason, now, createdAt, now);
  }

  private WebhookEndpoint reactivated(Instant now) {
    return new WebhookEndpoint(
        id, url, description, eventTypes, ACTIVE, null, null, createdAt, now);
  }
}
