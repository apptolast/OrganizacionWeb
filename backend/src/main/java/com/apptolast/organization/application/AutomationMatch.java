package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

/** One event a simulated rule would fire on. loopGuarded reports the guard without applying it. */
public record AutomationMatch(
    UUID eventId,
    String eventType,
    Instant occurredAt,
    ActionPreview preview,
    boolean loopGuarded) {}
