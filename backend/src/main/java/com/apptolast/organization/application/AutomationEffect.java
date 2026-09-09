package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;
import java.util.UUID;

/** What a run asks the adapter to do, already resolved and pending one single confirmation. */
public sealed interface AutomationEffect {
  /** A run that must leave no trace beyond its own row: a deterministic failure or a skip. */
  record None() implements AutomationEffect {}

  /** The task the adapter creates through the existing use case; it owns the resulting id. */
  record CreateTask(
      UUID projectId, String title, String completionCriterion, Integer estimatedMinutes)
      implements AutomationEffect {}

  /** The original event, queued for the endpoint without transformation. */
  record Notify(UUID endpointId, AutomationEvent event) implements AutomationEffect {}
}
