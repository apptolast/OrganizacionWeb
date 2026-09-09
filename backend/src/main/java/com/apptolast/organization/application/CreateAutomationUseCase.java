package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;

public interface CreateAutomationUseCase {
  AutomationRule create(String owner, AutomationDraft draft);
}
