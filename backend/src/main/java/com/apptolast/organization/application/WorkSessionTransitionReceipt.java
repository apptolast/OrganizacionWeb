package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Instant;
import java.util.UUID;

public record WorkSessionTransitionReceipt(
    UUID id,
    UUID sessionId,
    String action,
    Instant occurredAt,
    WorkSessionState before,
    WorkSessionState after) {
  public void requireIntent(UUID session, String requestedAction, long expected) {
    if (!sessionId.equals(session)
        || !action.equals(requestedAction)
        || before.revision() != expected) throw new WorkSessionIdempotencyConflictException();
  }
}
