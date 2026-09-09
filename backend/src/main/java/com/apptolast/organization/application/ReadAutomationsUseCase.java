package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRule;
import java.util.List;
import java.util.UUID;

public interface ReadAutomationsUseCase {
  List<AutomationRule> list(String owner);

  AutomationRule get(String owner, UUID id);
}
