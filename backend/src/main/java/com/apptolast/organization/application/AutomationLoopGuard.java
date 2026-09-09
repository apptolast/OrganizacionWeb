package com.apptolast.organization.application;

import java.util.UUID;

/** True when this task was created by an automation, so its creation event must not chain. */
@FunctionalInterface
public interface AutomationLoopGuard {
  boolean createdByAutomation(String owner, UUID taskId);
}
