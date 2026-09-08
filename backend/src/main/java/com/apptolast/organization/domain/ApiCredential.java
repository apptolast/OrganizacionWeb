package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiCredential(
    UUID id,
    String name,
    List<String> scopes,
    Instant createdAt,
    Instant expiresAt,
    Instant revokedAt) {}
