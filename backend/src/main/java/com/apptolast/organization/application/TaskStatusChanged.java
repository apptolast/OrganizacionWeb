package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record TaskStatusChanged(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    UUID taskId,
    String fromStatus,
    String toStatus) {}
