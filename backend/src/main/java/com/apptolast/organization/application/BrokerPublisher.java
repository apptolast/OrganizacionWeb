package com.apptolast.organization.application;

import com.apptolast.organization.domain.OutboxMessage;

@FunctionalInterface
public interface BrokerPublisher {
  DeliveryOutcome publish(OutboxMessage message);
}
