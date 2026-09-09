package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import java.util.UUID;

public interface ReplaceAutomationUseCase {
  AutomationRule replace(String owner, UUID id, long expectedVersion, AutomationDraft draft);
}
