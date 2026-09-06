package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record BlockPlanned(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    UUID blockId,
    UUID taskId,
    Instant startAt,
    Instant endAt,
    String zoneId,
    int durationMinutes) {}
