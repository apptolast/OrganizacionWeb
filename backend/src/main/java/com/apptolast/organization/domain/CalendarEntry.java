package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record CalendarEntry(
    UUID blockId,
    UUID projectId,
    UUID taskId,
    String title,
    String objective,
    Instant startAt,
    Instant endAt,
    long version,
    Instant stampedAt) {}
