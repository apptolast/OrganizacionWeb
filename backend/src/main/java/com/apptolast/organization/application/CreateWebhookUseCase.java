package com.apptolast.organization.application;

import java.util.List;

public interface CreateWebhookUseCase {
  WebhookCreation create(String owner, String url, String description, List<String> eventTypes);
}
