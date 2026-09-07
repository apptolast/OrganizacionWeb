package com.apptolast.organization.domain;

import java.util.UUID;

public record CustomFieldValuesRevision(
    CustomizationScope scope,
    UUID entityId,
    CustomizationRevision schema,
    CustomizationRevision values) {}
