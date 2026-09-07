package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Instant;

public record WorkSessionSnapshot(
    WorkSessionState state, Instant serverNow, long netMicroseconds) {}
