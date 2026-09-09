package com.apptolast.organization.application;

/** The referenced endpoint is not an active endpoint of this owner. */
public final class WebhookEndpointNotFoundException extends RuntimeException {
  public WebhookEndpointNotFoundException() {
    super("El endpoint indicado no existe.");
  }

  public String field() {
    return "action.endpointId";
  }
}
