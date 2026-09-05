package com.apptolast.organization.application;

public enum DeliveryOutcome {
  ACCEPTED,
  BROKER_UNAVAILABLE,
  BROKER_NACK,
  UNROUTABLE,
  CONFIRM_TIMEOUT
}
