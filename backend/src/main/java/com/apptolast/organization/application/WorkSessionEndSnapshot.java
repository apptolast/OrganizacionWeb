package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Instant;

public record WorkSessionEndSnapshot(
    WorkSessionState state, Instant serverNow, Instant effectiveEndAt) {}
