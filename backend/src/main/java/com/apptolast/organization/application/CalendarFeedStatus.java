package com.apptolast.organization.application;

import java.time.Instant;

public record CalendarFeedStatus(boolean active, Instant createdAt) {}
