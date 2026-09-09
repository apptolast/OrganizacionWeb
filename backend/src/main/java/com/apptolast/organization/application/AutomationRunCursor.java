package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

/** An opaque position in one rule's history; a cursor of another rule is not a valid value. */
public record AutomationRunCursor(UUID ruleId, Instant executedAt, UUID id) {}
