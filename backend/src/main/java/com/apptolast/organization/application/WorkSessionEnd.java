package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Instant;

public record WorkSessionEnd(
    WorkSessionState state, Instant effectiveEndAt, Instant lastDecisionAt) {
  public void requireTime(Instant now) {
    state.requireTime(now);
    if (now.isBefore(lastDecisionAt))
      throw new com.apptolast.organization.domain.WorkSessionTransitionException(
          "WORK_SESSION_TIME_OUT_OF_RANGE");
  }
}
