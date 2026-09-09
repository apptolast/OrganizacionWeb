package com.apptolast.organization.adapter.http;

import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

/**
 * The closed endpoint DTO: exactly the nine public fields, never the secret, with instants at the
 * microsecond precision the contract fixes.
 */
public record WebhookEndpointView(
    String id,
    String url,
    String description,
    List<String> eventTypes,
    String status,
    String disabledReason,
    String disabledAt,
    String createdAt,
    String updatedAt) {
  private static final DateTimeFormatter INSTANT =
      new DateTimeFormatterBuilder().appendInstant(6).toFormatter();

  public static WebhookEndpointView of(WebhookEndpoint endpoint) {
    return new WebhookEndpointView(
        endpoint.id().toString(),
        endpoint.url(),
        endpoint.description(),
        endpoint.eventTypes(),
        endpoint.status(),
        endpoint.disabledReason(),
        format(endpoint.disabledAt()),
        format(endpoint.createdAt()),
        format(endpoint.updatedAt()));
  }

  static String format(java.time.Instant instant) {
    return instant == null ? null : INSTANT.format(instant);
  }
}
