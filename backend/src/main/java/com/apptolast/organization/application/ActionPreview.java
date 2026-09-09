package com.apptolast.organization.application;

import java.util.UUID;

/** What an action would do for one event, resolved but never executed. */
public sealed interface ActionPreview {
  /** wouldFail names the deterministic failure the run would hit, or null when it would succeed. */
  record Task(
      UUID projectId,
      String title,
      String completionCriterion,
      Integer estimatedMinutes,
      String wouldFail)
      implements ActionPreview {}

  record Webhook(UUID endpointId, UUID eventId) implements ActionPreview {}
}
