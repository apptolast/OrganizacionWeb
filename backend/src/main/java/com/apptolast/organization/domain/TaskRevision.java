package com.apptolast.organization.domain;

import java.util.UUID;

public record TaskRevision(UUID id, long version) {}
