package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record SessionStart(
    UUID id,
    UUID projectId,
    UUID taskId,
    Instant startedAt,
    int plannedMinutes,
    Instant plannedEndAt,
    String zoneId) {}
