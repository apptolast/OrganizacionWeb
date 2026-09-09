package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;

/**
 * A delivery leased for one attempt, with everything the send needs. Amendment B1: the lease lets
 * the HTTP exchange happen outside any transaction.
 *
 * <p>The secret is in the clear here and must never be logged: {@link #toString()} redacts it.
 */
public record ClaimedDelivery(
    WebhookEndpoint endpoint,
    String ownerId,
    WebhookDelivery delivery,
    String body,
    String secret) {
  @Override
  public String toString() {
    return "ClaimedDelivery[endpoint="
        + endpoint.id()
        + ", delivery="
        + delivery.id()
        + ", secret=REDACTED]";
  }
}
