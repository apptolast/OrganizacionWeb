package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record WorkSessionExtended(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    String revision,
    int additionalMinutes,
    Instant previousEndAt,
    Instant effectiveEndAt,
    String status) {}
