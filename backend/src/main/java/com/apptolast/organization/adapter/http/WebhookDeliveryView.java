package com.apptolast.organization.adapter.http;

import com.apptolast.organization.domain.WebhookDelivery;

/**
 * The closed delivery DTO: exactly the eleven public fields. It never exposes the body, the target
 * URL, the signature nor any header of the receiver.
 */
public record WebhookDeliveryView(
    String id,
    String eventId,
    String eventType,
    String status,
    int attempt,
    Integer httpStatus,
    Integer latencyMs,
    String errorClass,
    String nextAttemptAt,
    String createdAt,
    String updatedAt) {
  public static WebhookDeliveryView of(WebhookDelivery delivery) {
    return new WebhookDeliveryView(
        delivery.id().toString(),
        delivery.eventId().toString(),
        delivery.eventType(),
        delivery.status(),
        delivery.attempt(),
        delivery.httpStatus(),
        delivery.latencyMs(),
        delivery.errorClass(),
        WebhookEndpointView.format(delivery.nextAttemptAt()),
        WebhookEndpointView.format(delivery.createdAt()),
        WebhookEndpointView.format(delivery.updatedAt()));
  }
}
