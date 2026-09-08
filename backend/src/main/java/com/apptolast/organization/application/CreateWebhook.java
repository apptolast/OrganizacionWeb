package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookEndpoint;
import com.apptolast.organization.domain.WebhookIntent;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class CreateWebhook implements CreateWebhookUseCase {
  private static final int SECRET_BYTES = 32;
  private final WebhookEndpoints endpoints;
  private final WebhookSecrets secrets;
  private final WebhookDestinationGuard destinations;
  private final Clock clock;
  private final SecureRandom random;

  public CreateWebhook(
      WebhookEndpoints endpoints,
      WebhookSecrets secrets,
      WebhookDestinationGuard destinations,
      Clock clock,
      SecureRandom random) {
    this.endpoints = endpoints;
    this.secrets = secrets;
    this.destinations = destinations;
    this.clock = clock;
    this.random = random;
  }

  @Override
  public WebhookCreation create(
      String owner, String url, String description, List<String> eventTypes) {
    var intent = new WebhookIntent(url, description, eventTypes);
    if (!secrets.available())
      throw new WebhookOperationException(WebhookOperationException.Code.CONNECTORS_DISABLED);
    destinations.check(intent.url());
    var endpoint =
        WebhookEndpoint.active(UUID.randomUUID(), intent, CustomizationTime.capture(clock));
    var secret = newSecret();
    endpoints.insert(owner, endpoint, secrets.encrypt(endpoint.id(), secret));
    return new WebhookCreation(endpoint, secret);
  }

  private String newSecret() {
    var bytes = new byte[SECRET_BYTES];
    random.nextBytes(bytes);
    return "whsec_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
