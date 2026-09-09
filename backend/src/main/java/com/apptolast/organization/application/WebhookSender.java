package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookAttempt;

/** One outgoing attempt. Implementations never follow redirects and never keep the response body. */
@FunctionalInterface
public interface WebhookSender {
  WebhookAttempt send(String url, String secret, String eventId, String body);
}
