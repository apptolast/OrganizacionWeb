package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRule;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Reads the owner's rules in a stable order; anything else is indistinguishable from missing. */
public final class ReadAutomations implements ReadAutomationsUseCase {
  private static final Comparator<AutomationRule> STABLE_ORDER =
      Comparator.comparing(AutomationRule::createdAt).thenComparing(AutomationRule::id);
  private final AutomationRuleStore rules;

  public ReadAutomations(AutomationRuleStore rules) {
    this.rules = rules;
  }

  @Override
  public List<AutomationRule> list(String owner) {
    return rules.list(owner).stream().sorted(STABLE_ORDER).toList();
  }

  @Override
  public AutomationRule get(String owner, UUID id) {
    return rules.find(owner, id).orElseThrow(ResourceNotFoundException::new);
  }
}
