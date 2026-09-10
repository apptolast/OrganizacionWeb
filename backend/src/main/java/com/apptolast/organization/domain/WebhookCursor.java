package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * How far an endpoint has walked the outbox, as the tuple (occurredAt, eventId). A fresh endpoint
 * starts at its creation instant with the nil event id, so an event of that very instant still
 * counts as new.
 *
 * <p>Who decides what lies beyond the cursor is the {@code (occurred_at, event_id) > (?, ?)} of
 * {@link com.apptolast.organization.adapter.persistence.PostgresWebhookOutbox}, not this record.
 * This class used to carry a {@code precedes} twin of that comparison whose only caller was a test
 * double: production that reimplements the decision it is meant to check, and three mutants that
 * nothing could kill. It is gone; the doubles compare on their own.
 */
public record WebhookCursor(Instant occurredAt, UUID eventId) {
  /**
   * {@link UUID#compareTo} is signed, so it puts every id whose first bit is set *below* the nil
   * id. PostgreSQL orders uuid values as unsigned bytes, and so must this cursor: otherwise half
   * the ids of an endpoint's very first instant would silently never be delivered.
   */
  public static int compareUnsigned(UUID left, UUID right) {
    var high = Long.compareUnsigned(left.getMostSignificantBits(), right.getMostSignificantBits());
    return high != 0
        ? high
        : Long.compareUnsigned(left.getLeastSignificantBits(), right.getLeastSignificantBits());
  }
}
