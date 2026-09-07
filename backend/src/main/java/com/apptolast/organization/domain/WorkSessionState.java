package com.apptolast.organization.domain;

import java.time.Instant;

public record WorkSessionState(SessionStart session, String status, long revision, Instant changedAt,
    long workedMicroseconds, Instant runningSince) {}
