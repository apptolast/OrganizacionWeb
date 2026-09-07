package com.apptolast.organization.domain;

public record CustomFieldDefinition(
    java.util.UUID id, String label, CustomFieldType type, boolean active) {}
