package com.apptolast.organization.application;

import java.time.Instant;

/** The only moment the secret url exists outside the caller's browser. */
public record CalendarFeedLink(String url, Instant createdAt) {}
