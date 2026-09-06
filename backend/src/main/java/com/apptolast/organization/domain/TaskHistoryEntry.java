package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record TaskHistoryEntry(
    UUID id, long taskVersion, String fromStatus, String toStatus, Instant occurredAt) {}
