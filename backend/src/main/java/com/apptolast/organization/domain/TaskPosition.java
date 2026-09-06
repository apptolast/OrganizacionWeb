package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record TaskPosition(Instant createdAt, UUID id) {}
