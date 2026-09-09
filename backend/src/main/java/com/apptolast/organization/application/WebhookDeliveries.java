package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookDeliveries {
  /** The last fifty terminal deliveries plus every pending one, newest first. */
  List<WebhookDelivery> list(String owner, UUID endpointId);

  /** A ping still in flight blocks the next one; outbox deliveries do not. */
  boolean hasPendingPing(String owner, UUID endpointId);

  WebhookDelivery enqueuePing(String owner, UUID endpointId, WebhookDelivery delivery, String body);

  Optional<WebhookDelivery> find(String owner, UUID endpointId, UUID deliveryId);

  /** Persists a reopened delivery, keeping its stored body untouched. */
  WebhookDelivery requeue(String owner, UUID endpointId, WebhookDelivery delivery);
}
