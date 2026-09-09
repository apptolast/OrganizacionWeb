package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import java.util.List;
import java.util.UUID;

/** Reads one rule's history by pages; a rule that is not the owner's simply does not exist. */
public final class ReadAutomationRuns implements ReadAutomationRunsUseCase {
  public static final int PAGE_SIZE = 20;

  private final AutomationRuleStore rules;
  private final AutomationRunStore runs;

  public ReadAutomationRuns(AutomationRuleStore rules, AutomationRunStore runs) {
    this.rules = rules;
    this.runs = runs;
  }

  @Override
  public AutomationRunPage read(String owner, UUID ruleId, AutomationRunCursor after) {
    rules.find(owner, ruleId).orElseThrow(ResourceNotFoundException::new);
    if (after != null && !ruleId.equals(after.ruleId())) throw invalidCursor();
    var found = runs.page(owner, ruleId, after, PAGE_SIZE + 1);
    if (found.size() <= PAGE_SIZE) return new AutomationRunPage(found, null);
    var page = found.subList(0, PAGE_SIZE);
    return new AutomationRunPage(page, cursorOf(ruleId, page.getLast()));
  }

  private static AutomationRunCursor cursorOf(UUID ruleId, AutomationRun last) {
    return new AutomationRunCursor(ruleId, last.executedAt(), last.id());
  }

  private static ValidationException invalidCursor() {
    return new ValidationException(
        List.of(new FieldError("cursor", "INVALID_VALUE", "Revisa el valor de este campo.")));
  }
}
