package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record Appearance(
    UUID id,
    String owner,
    String theme,
    String accentLight,
    String accentDark,
    long version,
    Instant updatedAt) {}
