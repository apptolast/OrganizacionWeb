package com.apptolast.organization.application;

import java.time.Instant;

public record WorkSessionExtension(
    int additionalMinutes, Instant previousEndAt, Instant effectiveEndAt) {}
