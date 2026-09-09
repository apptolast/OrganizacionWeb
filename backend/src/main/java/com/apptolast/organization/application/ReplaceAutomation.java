package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import java.time.Clock;
import java.util.UUID;

/** A full replacement that always bumps the version; it never consumes a new quota slot. */
public final class ReplaceAutomation implements ReplaceAutomationUseCase {
  private final AutomationRuleStore rules;
  private final AutomationReferences references;
  private final Clock clock;

  public ReplaceAutomation(
      AutomationRuleStore rules,
      AutomationTargets targets,
      WebhookEndpointLookup endpoints,
      Clock clock) {
    this.rules = rules;
    this.references = new AutomationReferences(targets, endpoints);
    this.clock = clock;
  }

  @Override
  public AutomationRule replace(String owner, UUID id, long expectedVersion, AutomationDraft draft) {
    references.check(owner, draft);
    return rules.replace(owner, id, expectedVersion, draft, CustomizationTime.capture(clock));
  }
}
