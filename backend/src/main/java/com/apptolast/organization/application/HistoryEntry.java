package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record HistoryEntry<D>(
    UUID id,
    String type,
    Instant occurredAt,
    UUID projectId,
    String projectName,
    UUID taskId,
    String taskTitle,
    D details) {}
