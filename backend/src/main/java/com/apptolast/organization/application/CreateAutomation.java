package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import java.time.Clock;
import java.util.UUID;

/** Validates a draft against the owner's projects and endpoints and stores it as version one. */
public final class CreateAutomation implements CreateAutomationUseCase {
  private final AutomationRuleStore rules;
  private final AutomationReferences references;
  private final Clock clock;

  public CreateAutomation(
      AutomationRuleStore rules,
      AutomationTargets targets,
      WebhookEndpointLookup endpoints,
      Clock clock) {
    this.rules = rules;
    this.references = new AutomationReferences(targets, endpoints);
    this.clock = clock;
  }

  @Override
  public AutomationRule create(String owner, AutomationDraft draft) {
    references.check(owner, draft);
    var now = CustomizationTime.capture(clock);
    return rules.create(owner, new AutomationRule(UUID.randomUUID(), draft, 1, now, now));
  }
}
