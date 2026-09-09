package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookCursor;
import com.apptolast.organization.domain.WebhookEndpoint;

/** An active endpoint with no outbox-derived delivery in flight, plus where its cursor stands. */
public record ReadyEndpoint(WebhookEndpoint endpoint, String ownerId, WebhookCursor cursor) {}
