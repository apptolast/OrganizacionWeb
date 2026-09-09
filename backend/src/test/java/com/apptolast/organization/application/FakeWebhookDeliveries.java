package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** In-memory delivery log that records what the use case asked it to do. */
class FakeWebhookDeliveries implements WebhookDeliveries {
  final List<WebhookDelivery> stored = new ArrayList<>();
  WebhookDelivery enqueued;
  String enqueuedBody;
  UUID enqueuedFor;
  WebhookDelivery requeued;
  boolean pendingPing;

  @Override
  public List<WebhookDelivery> list(String owner, UUID endpointId) {
    return List.copyOf(stored);
  }

  @Override
  public boolean hasPendingPing(String owner, UUID endpointId) {
    return pendingPing;
  }

  @Override
  public WebhookDelivery enqueuePing(
      String owner, UUID endpointId, WebhookDelivery delivery, String body) {
    enqueued = delivery;
    enqueuedBody = body;
    enqueuedFor = endpointId;
    stored.add(delivery);
    return delivery;
  }

  @Override
  public Optional<WebhookDelivery> find(String owner, UUID endpointId, UUID deliveryId) {
    return stored.stream().filter(delivery -> delivery.id().equals(deliveryId)).findFirst();
  }

  @Override
  public WebhookDelivery requeue(String owner, UUID endpointId, WebhookDelivery delivery) {
    requeued = delivery;
    stored.replaceAll(stored -> stored.id().equals(delivery.id()) ? delivery : stored);
    return delivery;
  }

  WebhookDelivery give(UUID id, String status, Instant createdAt) {
    var delivery =
        new WebhookDelivery(
            id,
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "TaskCreated.v1",
            status,
            6,
            200,
            12,
            null,
            null,
            createdAt,
            createdAt);
    stored.add(delivery);
    return delivery;
  }
}
