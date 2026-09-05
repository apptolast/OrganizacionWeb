package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record ProjectStatusChanged(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    String fromStatus,
    String toStatus) {}
