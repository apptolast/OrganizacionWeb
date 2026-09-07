package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomFieldValues(
    UUID entityId,
    CustomizationScope scope,
    CustomizationRevision schema,
    CustomizationRevision revision,
    List<CustomFieldValue> values,
    Instant updatedAt) {}
