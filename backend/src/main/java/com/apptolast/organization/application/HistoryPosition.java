package com.apptolast.organization.application;

import java.time.Instant;
import java.util.UUID;

public record HistoryPosition(Instant occurredAt, String type, UUID id) {}
