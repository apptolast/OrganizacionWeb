package com.apptolast.organization.application;

import java.util.UUID;

/** Removes a rule of the owner, freeing a quota slot; its runs keep the task they created. */
public final class DeleteAutomation implements DeleteAutomationUseCase {
  private final AutomationRuleStore rules;

  public DeleteAutomation(AutomationRuleStore rules) {
    this.rules = rules;
  }

  @Override
  public void delete(String owner, UUID id, long expectedVersion) {
    rules.delete(owner, id, expectedVersion);
  }
}
