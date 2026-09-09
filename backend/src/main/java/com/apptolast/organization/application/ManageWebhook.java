package com.apptolast.organization.application;

import static com.apptolast.organization.application.WebhookOperationException.Code;

import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import com.apptolast.organization.domain.WebhookInvalidException;
import com.apptolast.organization.domain.WebhookPingPayload;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reads and lifecycle of the owner's webhooks. Every operation resolves a foreign endpoint exactly
 * like a missing one, so the API cannot be used as an existence oracle.
 */
public final class ManageWebhook implements ManageWebhookUseCase {
  private static final List<String> SETTABLE_STATUSES =
      List.of(WebhookEndpoint.ACTIVE, WebhookEndpoint.DISABLED);
  private final WebhookEndpoints endpoints;
  private final WebhookDeliveries deliveries;
  private final WebhookSecrets secrets;
  private final Clock clock;

  public ManageWebhook(
      WebhookEndpoints endpoints,
      WebhookDeliveries deliveries,
      WebhookSecrets secrets,
      Clock clock) {
    this.endpoints = endpoints;
    this.deliveries = deliveries;
    this.secrets = secrets;
    this.clock = clock;
  }

  @Override
  public List<WebhookEndpoint> list(String owner) {
    return endpoints.list(owner);
  }

  @Override
  public WebhookEndpoint find(String owner, UUID id) {
    return endpoints.find(owner, id).orElseThrow(ManageWebhook::notFound);
  }

  @Override
  public WebhookEndpoint changeStatus(String owner, UUID id, String status) {
    if (status == null || !SETTABLE_STATUSES.contains(status))
      throw new WebhookInvalidException(List.of("status"));
    return endpoints
        .changeStatus(owner, id, status, now())
        .orElseThrow(ManageWebhook::notFound);
  }

  @Override
  public void delete(String owner, UUID id) {
    if (!endpoints.delete(owner, id)) throw notFound();
  }

  @Override
  public List<WebhookDelivery> deliveries(String owner, UUID id) {
    find(owner, id);
    return deliveries.list(owner, id);
  }

  @Override
  public WebhookDelivery ping(String owner, UUID id) {
    requireConnectorKey();
    requireActive(owner, id);
    if (deliveries.hasPendingPing(owner, id)) throw operation(Code.DELIVERY_PENDING);
    var now = now();
    var delivery = WebhookDelivery.ping(UUID.randomUUID(), now);
    return deliveries.enqueuePing(
        owner, id, delivery, WebhookPingPayload.of(delivery.id(), id, owner, now));
  }

  @Override
  public WebhookDelivery redeliver(String owner, UUID id, UUID deliveryId) {
    requireConnectorKey();
    requireActive(owner, id);
    var delivery =
        deliveries.find(owner, id, deliveryId).orElseThrow(ManageWebhook::notFound);
    if (!delivery.isTerminal()) throw operation(Code.DELIVERY_PENDING);
    return deliveries.requeue(owner, id, delivery.requeued(now()));
  }

  private void requireConnectorKey() {
    if (!secrets.available()) throw operation(Code.CONNECTORS_DISABLED);
  }

  private void requireActive(String owner, UUID id) {
    if (!find(owner, id).isActive()) throw operation(Code.DISABLED);
  }

  private Instant now() {
    return CustomizationTime.capture(clock);
  }

  private static WebhookOperationException notFound() {
    return operation(Code.NOT_FOUND);
  }

  private static WebhookOperationException operation(Code code) {
    return new WebhookOperationException(code);
  }
}
