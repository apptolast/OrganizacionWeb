package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRun;
import java.util.*;

/** Test double of the run store: newest first, strictly after the cursor, capped at the limit. */
final class InMemoryAutomationRuns implements AutomationRunStore {
  private static final Comparator<AutomationRun> NEWEST_FIRST =
      Comparator.comparing(AutomationRun::executedAt).thenComparing(AutomationRun::id).reversed();

  private final List<AutomationRun> stored = new ArrayList<>();

  AutomationRun add(AutomationRun run) {
    stored.add(run);
    return run;
  }

  @Override
  public List<AutomationRun> page(String owner, UUID ruleId, AutomationRunCursor after, int limit) {
    return stored.stream()
        .filter(run -> run.ownerId().equals(owner) && ruleId.equals(run.ruleId()))
        .sorted(NEWEST_FIRST)
        .filter(run -> after == null || isAfter(run, after))
        .limit(limit)
        .toList();
  }

  private static boolean isAfter(AutomationRun run, AutomationRunCursor cursor) {
    int byInstant = run.executedAt().compareTo(cursor.executedAt());
    return byInstant < 0 || byInstant == 0 && run.id().compareTo(cursor.id()) < 0;
  }
}
