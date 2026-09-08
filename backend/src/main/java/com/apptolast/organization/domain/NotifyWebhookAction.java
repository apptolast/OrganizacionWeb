package com.apptolast.organization.domain;

import java.util.UUID;

public record NotifyWebhookAction(UUID endpointId) implements AutomationAction {
  public NotifyWebhookAction {
    if (endpointId == null) throw CreateTaskAction.invalid("action.endpointId", "REQUIRED");
  }

  @Override
  public String type() {
    return "NOTIFY_WEBHOOK";
  }
}
