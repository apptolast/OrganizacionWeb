package com.apptolast.organization.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WorkSessionClosed(
    UUID eventId,
    UUID aggregateId,
    String ownerId,
    Instant occurredAt,
    int schemaVersion,
    String type,
    String revision,
    String fromStatus,
    String workedMicroseconds,
    LocalDate workDate,
    String closeZoneId) {}
