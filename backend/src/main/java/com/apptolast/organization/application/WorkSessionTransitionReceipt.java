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
    WorkSessionState after,
    WorkSessionClosure closure,
    WorkSessionExtension extension) {
  public WorkSessionTransitionReceipt(
      UUID id,
      UUID sessionId,
      String action,
      Instant occurredAt,
      WorkSessionState before,
      WorkSessionState after,
      WorkSessionClosure closure) {
    this(id, sessionId, action, occurredAt, before, after, closure, null);
  }

  public WorkSessionTransitionReceipt(
      UUID id,
      UUID sessionId,
      String action,
      Instant occurredAt,
      WorkSessionState before,
      WorkSessionState after) {
    this(id, sessionId, action, occurredAt, before, after, null);
  }

  public void requireIntent(UUID session, String requestedAction, long expected) {
    if (!sessionId.equals(session)
        || !action.equals(requestedAction)
        || before.revision() != expected) throw new WorkSessionIdempotencyConflictException();
  }

  public void requireIntent(
      UUID session,
      String requestedAction,
      long expected,
      com.apptolast.organization.domain.WorkSessionCloseNotes notes) {
    requireIntent(session, requestedAction, expected);
    if (action.equals("CLOSE")
        && !new com.apptolast.organization.domain.WorkSessionCloseNotes(
                closure.progressNote(), closure.nextStep())
            .equals(notes)) throw new WorkSessionIdempotencyConflictException();
  }
}
