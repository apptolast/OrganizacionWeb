package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CustomFieldValuesCollection(
    UUID id, Map<UUID, Object> values, long version, Instant updatedAt) {}
