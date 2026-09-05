package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record ProjectSummary(
    UUID id, String name, String status, Instant createdAt, Instant updatedAt) {}
