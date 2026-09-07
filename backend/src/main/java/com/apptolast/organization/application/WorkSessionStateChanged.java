package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record WorkSessionStateChanged(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    String action,
    String revision,
    String fromStatus,
    String toStatus,
    String workedMicroseconds,
    Instant runningSince) {}
