package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationCursor;
import java.util.List;

/**
 * Everything one event produced, to be confirmed atomically: the runs of every rule that fired, the
 * effects and the cursor. A null cursor means the walk must not advance, which is what a retry of
 * an older event asks for.
 */
public record AutomationCommit(
    String owner, AutomationCursor reached, List<AutomationOutcome> outcomes) {
  public AutomationCommit {
    outcomes = List.copyOf(outcomes);
  }
}
