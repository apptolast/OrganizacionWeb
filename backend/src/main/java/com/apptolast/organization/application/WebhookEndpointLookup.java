package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Port over the webhook endpoints of feature 25: true only for an active endpoint of this owner.
 * Phase one ships a stub that always answers false; phase two plugs the real adapter.
 */
@FunctionalInterface
public interface WebhookEndpointLookup {
  boolean isActiveEndpointOf(String owner, UUID endpointId);
}
