package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookEndpoint;

public record WebhookCreation(WebhookEndpoint endpoint, String secret) {
  @Override
  public String toString() {
    return "WebhookCreation[endpoint=" + endpoint + ", secret=REDACTED]";
  }
}
