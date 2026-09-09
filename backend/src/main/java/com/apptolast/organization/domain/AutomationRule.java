package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/** A stored rule: its validated body plus the identity, version and timestamps of the server. */
public record AutomationRule(
    UUID id, AutomationDraft draft, long version, Instant createdAt, Instant updatedAt) {}
