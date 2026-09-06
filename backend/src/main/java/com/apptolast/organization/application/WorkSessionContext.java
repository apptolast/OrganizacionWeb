package com.apptolast.organization.application;

import java.util.Optional;

public record WorkSessionContext(
    String projectStatus, String taskStatus, Optional<String> zoneId) {}
