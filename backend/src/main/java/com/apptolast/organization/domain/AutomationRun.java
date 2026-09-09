package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One attempt of one rule on one event. ruleId is null once the rule is deleted, which keeps the
 * loop guard working while hiding the run from the API.
 */
public record AutomationRun(
    UUID id,
    UUID ruleId,
    String ownerId,
    UUID eventId,
    String eventType,
    Instant occurredAt,
    int attempt,
    String status,
    UUID createdTaskId,
    UUID deliveryId,
    String errorCode,
    Instant executedAt) {}
