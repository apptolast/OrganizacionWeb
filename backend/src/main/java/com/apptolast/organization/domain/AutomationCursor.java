package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * How far the automations worker has walked one owner's outbox, as the tuple (occurredAt, eventId).
 * A fresh owner starts at the instant of their first rule with the nil event id, so an event of
 * that very instant still counts as new.
 *
 * <p>Not to be confused with {@code AutomationRunCursor}, which paginates one rule's history.
 */
public record AutomationCursor(Instant occurredAt, UUID eventId) {
  /** The id a fresh cursor carries: below every real event id in unsigned order. */
  public static final UUID START = new UUID(0L, 0L);

  /** True when the given event sits strictly after this cursor in tuple order. */
  public boolean precedes(Instant candidateOccurredAt, UUID candidateEventId) {
    var byInstant = candidateOccurredAt.compareTo(occurredAt);
    if (byInstant != 0) return byInstant > 0;
    // PostgreSQL orders uuid as unsigned bytes while UUID#compareTo is signed; feature 25 already
    // pinned that comparison down and both cursors must agree with the same database ordering.
    return WebhookCursor.compareUnsigned(candidateEventId, eventId) > 0;
  }
}
