package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record WorkSessionStarted(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    UUID projectId,
    UUID taskId,
    int plannedMinutes,
    Instant plannedEndAt,
    String zoneId) {}
