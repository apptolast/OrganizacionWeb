package com.apptolast.organization.domain;

/** Closed set of actions a rule may run. */
public sealed interface AutomationAction permits CreateTaskAction, NotifyWebhookAction {
  String type();
}
