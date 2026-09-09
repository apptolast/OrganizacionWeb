package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * How far an endpoint has walked the outbox, as the tuple (occurredAt, eventId). A fresh endpoint
 * starts at its creation instant with the nil event id, so an event of that very instant still
 * counts as new.
 */
public record WebhookCursor(Instant occurredAt, UUID eventId) {
  /** True when the given event sits strictly after this cursor in tuple order. */
  public boolean precedes(Instant candidateOccurredAt, UUID candidateEventId) {
    var byInstant = candidateOccurredAt.compareTo(occurredAt);
    if (byInstant != 0) return byInstant > 0;
    return compareUnsigned(candidateEventId, eventId) > 0;
  }

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
