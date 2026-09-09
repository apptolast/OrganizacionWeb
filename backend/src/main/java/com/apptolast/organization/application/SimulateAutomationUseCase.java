package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;

public interface SimulateAutomationUseCase {
  AutomationSimulation simulate(String owner, AutomationDraft draft);
}
