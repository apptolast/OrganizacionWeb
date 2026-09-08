package com.apptolast.organization.domain;

public record Customization(
    java.util.UUID id,
    String owner,
    CustomizationScope scope,
    java.util.List<String> visibleFields,
    java.util.List<CustomFieldDefinition> customFields,
    long version,
    java.time.Instant updatedAt) {}
