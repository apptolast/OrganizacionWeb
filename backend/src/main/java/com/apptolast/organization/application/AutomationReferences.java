package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.NotifyWebhookAction;

/** Checks that the projects and endpoints named by a draft belong to the owner. */
final class AutomationReferences {
  private final AutomationTargets targets;
  private final WebhookEndpointLookup endpoints;

  AutomationReferences(AutomationTargets targets, WebhookEndpointLookup endpoints) {
    this.targets = targets;
    this.endpoints = endpoints;
  }

  void check(String owner, AutomationDraft draft) {
    if (draft.conditionProjectId() != null
        && !targets.ownsProject(owner, draft.conditionProjectId()))
      throw new AutomationTargetNotFoundException("condition.projectId");
    switch (draft.action()) {
      case CreateTaskAction task -> {
        if (!targets.ownsProject(owner, task.projectId()))
          throw new AutomationTargetNotFoundException("action.projectId");
      }
      case NotifyWebhookAction webhook -> {
        if (!endpoints.isActiveEndpointOf(owner, webhook.endpointId()))
          throw new WebhookEndpointNotFoundException();
      }
    }
  }
}
