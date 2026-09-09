package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRun;
import java.util.List;

/**
 * One outbox row offered to the worker: the event as the rules see it, its blocked flag and the
 * runs already recorded for it. Carrying the runs is what lets a stranded event be retried in the
 * order the outbox already imposes, instead of through a second queue with its own ordering.
 */
public record AutomationCandidate(
    AutomationEvent event, boolean blocked, List<AutomationRun> runs) {
  public AutomationCandidate {
    runs = List.copyOf(runs);
  }
}
