package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owner-scoped storage of rules with optimistic versions and the twenty-rule cap. */
public interface AutomationRuleStore {
  AutomationRule create(String owner, AutomationRule rule);

  List<AutomationRule> list(String owner);

  Optional<AutomationRule> find(String owner, UUID id);

  AutomationRule replace(
      String owner, UUID id, long expectedVersion, AutomationDraft draft, Instant now);

  void delete(String owner, UUID id, long expectedVersion);
}
