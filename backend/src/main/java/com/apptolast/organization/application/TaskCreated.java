package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record TaskCreated(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    UUID taskId,
    String title)
    implements TaskCreationEvent {}
