package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record BlockPosition(Instant createdAt, UUID id) {}
