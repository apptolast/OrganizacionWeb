package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record SubtaskCreated(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    UUID taskId,
    UUID parentTaskId,
    String title)
    implements TaskCreationEvent {}
