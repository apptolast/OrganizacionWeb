package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Instant;

public record WorkSessionEnd(
    WorkSessionState state, Instant effectiveEndAt, Instant lastDecisionAt) {}
