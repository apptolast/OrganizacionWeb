package com.apptolast.organization.domain;

import java.util.UUID;

public record CustomFieldValue(UUID fieldId, String label, CustomFieldType type, Object value) {}
