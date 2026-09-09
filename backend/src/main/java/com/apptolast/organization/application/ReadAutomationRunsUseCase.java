package com.apptolast.organization.application;

import java.util.UUID;

public interface ReadAutomationRunsUseCase {
  AutomationRunPage read(String owner, UUID ruleId, AutomationRunCursor after);
}
