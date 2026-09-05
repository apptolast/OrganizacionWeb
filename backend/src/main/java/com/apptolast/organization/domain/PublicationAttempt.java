package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record PublicationAttempt(
    UUID eventId,
    String outcome,
    long attempt,
    Instant completedAt,
    Instant nextAttemptAt,
    String code) {}
