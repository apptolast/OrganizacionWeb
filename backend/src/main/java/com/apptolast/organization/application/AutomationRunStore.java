package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRun;
import java.util.List;
import java.util.UUID;

/** Owner-scoped history of one rule, ordered executedAt and id descending. */
@FunctionalInterface
public interface AutomationRunStore {
  List<AutomationRun> page(String owner, UUID ruleId, AutomationRunCursor after, int limit);
}
