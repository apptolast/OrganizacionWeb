package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record ProjectUpdated(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String name,
    String type) {}
