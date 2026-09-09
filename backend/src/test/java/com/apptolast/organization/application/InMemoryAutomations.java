package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import java.time.Instant;
import java.util.*;

/** Test double of the rule store: owner-scoped list with optimistic versions and the 20 cap. */
final class InMemoryAutomations implements AutomationRuleStore {
  final Map<String, List<AutomationRule>> byOwner = new HashMap<>();

  @Override
  public AutomationRule create(String owner, AutomationRule rule) {
    var owned = byOwner.computeIfAbsent(owner, key -> new ArrayList<>());
    if (owned.size() >= 20) throw new AutomationLimitException();
    owned.add(rule);
    return rule;
  }

  @Override
  public List<AutomationRule> list(String owner) {
    return List.copyOf(byOwner.getOrDefault(owner, List.of()));
  }

  @Override
  public Optional<AutomationRule> find(String owner, UUID id) {
    return list(owner).stream().filter(rule -> rule.id().equals(id)).findFirst();
  }

  @Override
  public AutomationRule replace(
      String owner, UUID id, long expectedVersion, AutomationDraft draft, Instant now) {
    var owned = byOwner.getOrDefault(owner, new ArrayList<>());
    var current = find(owner, id).orElseThrow(ResourceNotFoundException::new);
    if (current.version() != expectedVersion) throw new AutomationConflictException();
    var next = new AutomationRule(id, draft, current.version() + 1, current.createdAt(), now);
    owned.set(owned.indexOf(current), next);
    return next;
  }

  @Override
  public void delete(String owner, UUID id, long expectedVersion) {
    var current = find(owner, id).orElseThrow(ResourceNotFoundException::new);
    if (current.version() != expectedVersion) throw new AutomationConflictException();
    byOwner.get(owner).remove(current);
  }
}
