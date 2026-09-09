package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRule;
import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.CreateTaskAction;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Walks each owner's outbox and turns the events their rules match into runs and effects. */
public final class ExecuteAutomations implements ExecuteAutomationsUseCase {
  private static final String SUCCEEDED = "succeeded";

  private final AutomationWork work;
  private final AutomationRuleStore rules;
  private final AutomationMatcher matcher;
  private final AutomationRendering rendering;
  private final WebhookEndpointLookup endpoints;
  private final Clock clock;

  public ExecuteAutomations(
      AutomationWork work,
      AutomationRuleStore rules,
      AutomationMatcher matcher,
      AutomationFacts facts,
      WebhookEndpointLookup endpoints,
      Clock clock) {
    this.work = work;
    this.rules = rules;
    this.matcher = matcher;
    this.rendering = new AutomationRendering(matcher, facts);
    this.endpoints = endpoints;
    this.clock = clock;
  }

  @Override
  public void runCycle() {
    for (var owner : work.ownersWithRules()) walk(owner);
  }

  private void walk(String owner) {
    work.cursor(owner)
        .or(() -> startCursorOf(owner))
        .ifPresent(cursor -> work.after(owner, cursor).forEach(row -> process(owner, row)));
  }

  /**
   * An owner with no cursor starts at the instant of their oldest rule, never at the beginning of
   * the outbox: rules answer for what happens after they exist, never for the account's history.
   */
  private Optional<AutomationCursor> startCursorOf(String owner) {
    var start =
        rules.list(owner).stream()
            .map(AutomationRule::createdAt)
            .min(Comparator.naturalOrder())
            .map(oldest -> new AutomationCursor(oldest, AutomationCursor.START));
    start.ifPresent(cursor -> work.startCursor(owner, cursor));
    return start;
  }

  private void process(String owner, AutomationCandidate candidate) {
    var event = candidate.event();
    // A blocked row is a deliberate skip: it produces nothing, but the walk still moves past it.
    var outcomes = candidate.blocked() ? List.<AutomationOutcome>of() : firedBy(owner, event);
    work.commit(new AutomationCommit(owner, reachedBy(event), outcomes));
  }

  private List<AutomationOutcome> firedBy(String owner, AutomationEvent event) {
    return rules.list(owner).stream()
        .filter(rule -> matcher.matches(owner, rule.draft(), event))
        .map(rule -> outcomeOf(owner, rule, event))
        .toList();
  }

  private AutomationOutcome outcomeOf(String owner, AutomationRule rule, AutomationEvent event) {
    var action = (CreateTaskAction) rule.draft().action();
    var preview = rendering.preview(owner, action, event);
    return new AutomationOutcome(
        run(owner, rule.id(), event, SUCCEEDED),
        new AutomationEffect.CreateTask(
            preview.projectId(),
            preview.title(),
            preview.completionCriterion(),
            preview.estimatedMinutes()));
  }

  private AutomationRun run(String owner, UUID ruleId, AutomationEvent event, String status) {
    return new AutomationRun(
        UUID.randomUUID(),
        ruleId,
        owner,
        event.eventId(),
        event.eventType(),
        event.occurredAt(),
        1,
        status,
        null,
        null,
        null,
        CustomizationTime.capture(clock));
  }

  private static AutomationCursor reachedBy(AutomationEvent event) {
    return new AutomationCursor(event.occurredAt(), event.eventId());
  }
}
