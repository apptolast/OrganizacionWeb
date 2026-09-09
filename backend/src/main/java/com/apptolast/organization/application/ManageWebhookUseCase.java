package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.util.List;
import java.util.UUID;

public interface ManageWebhookUseCase {
  List<WebhookEndpoint> list(String owner);

  WebhookEndpoint find(String owner, UUID id);

  WebhookEndpoint changeStatus(String owner, UUID id, String status);

  void delete(String owner, UUID id);

  WebhookDelivery ping(String owner, UUID id);

  List<WebhookDelivery> deliveries(String owner, UUID id);

  WebhookDelivery redeliver(String owner, UUID id, UUID deliveryId);
}
