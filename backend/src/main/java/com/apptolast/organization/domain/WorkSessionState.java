package com.apptolast.organization.domain;

import java.time.Instant;

public record WorkSessionState(
    SessionStart session,
    String status,
    long revision,
    Instant changedAt,
    long workedMicroseconds,
    Instant runningSince) {
  public void requireTransition(long expected, boolean pause) {
    if (revision != expected) throw new WorkSessionTransitionException("PRECONDITION_FAILED");
    if (!status.equals(pause ? "running" : "paused"))
      throw new WorkSessionTransitionException("WORK_SESSION_STATE_CONFLICT");
    if (revision == Long.MAX_VALUE)
      throw new WorkSessionTransitionException("WORK_SESSION_REVISION_EXHAUSTED");
  }

  public void requireTime(Instant now) {
    if (now.isBefore(changedAt) || !now.isBefore(Instant.parse("+10000-01-01T00:00:00Z")))
      throw new WorkSessionTransitionException("WORK_SESSION_TIME_OUT_OF_RANGE");
  }
}
