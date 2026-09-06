package com.apptolast.organization.domain;
import java.time.Instant;
import java.util.UUID;
public record BlockChangeReceipt(UUID id, UUID blockId, String kind, long version, Instant occurredAt, PlannedBlock before, PlannedBlock after) {}
