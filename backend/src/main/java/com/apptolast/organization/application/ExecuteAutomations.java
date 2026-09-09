package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRule;
import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.NotifyWebhookAction;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Walks each owner's outbox and turns the events their rules match into runs and effects. */
public final class ExecuteAutomations implements ExecuteAutomationsUseCase {
  private static final String SUCCEEDED = "succeeded";
  private static final String FAILED = "failed";
  private static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
  private static final String RETRY = "retry";
  private static final String STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE";
  private static final int FIRST_ATTEMPT = 1;
  private static final int MAX_ATTEMPTS = 3;

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
    var cursor = work.cursor(owner).or(() -> startCursorOf(owner));
    if (cursor.isEmpty()) return;
    for (var candidate : work.after(owner, cursor.get())) if (!process(owner, candidate)) return;
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

  /** False when the confirmation did not land, which strands the walk on this very event. */
  private boolean process(String owner, AutomationCandidate candidate) {
    var event = candidate.event();
    // A blocked row is a deliberate skip: it produces nothing, but the walk still moves past it.
    var outcomes = candidate.blocked() ? List.<AutomationOutcome>of() : firedBy(owner, candidate);
    try {
      work.commit(new AutomationCommit(owner, reachedBy(event), outcomes));
      return true;
    } catch (RuntimeException failure) {
      // Whatever broke the confirmation, from here it is one thing: the write did not happen.
      // Stranding an owner behind an unexpected failure would be worse than one coarse code.
      var rows = outcomes.stream().map(ExecuteAutomations::unavailable).toList();
      rows.forEach(work::record);
      // An event with nothing left open no longer holds the walk back: only the cursor lags, and
      // the next cycle fixes that when it reads the event again and finds every rule settled.
      return !rows.isEmpty() && rows.stream().noneMatch(row -> RETRY.equals(row.status()));
    }
  }

  /** The only row that survives a rolled back confirmation, written outside it. */
  private static AutomationRun unavailable(AutomationOutcome outcome) {
    var run = outcome.run();
    return new AutomationRun(
        run.id(),
        run.ruleId(),
        run.ownerId(),
        run.eventId(),
        run.eventType(),
        run.occurredAt(),
        run.attempt(),
        run.attempt() < MAX_ATTEMPTS ? RETRY : FAILED,
        null,
        null,
        STORAGE_UNAVAILABLE,
        run.executedAt());
  }

  private List<AutomationOutcome> firedBy(String owner, AutomationCandidate candidate) {
    var event = candidate.event();
    return rules.list(owner).stream()
        .filter(rule -> matcher.matches(owner, rule.draft(), event))
        .map(rule -> attemptOf(owner, rule, candidate))
        .flatMap(Optional::stream)
        .toList();
  }

  /** Empty when this rule already settled this event: a settled row is never attempted again. */
  private Optional<AutomationOutcome> attemptOf(
      String owner, AutomationRule rule, AutomationCandidate candidate) {
    var prior = candidate.runs().stream().filter(run -> rule.id().equals(run.ruleId())).findFirst();
    if (prior.filter(run -> !RETRY.equals(run.status())).isPresent()) return Optional.empty();
    return Optional.of(
        outcomeOf(owner, rule, candidate.event(), Attempt.after(prior.orElse(null))));
  }

  private AutomationOutcome outcomeOf(
      String owner, AutomationRule rule, AutomationEvent event, Attempt attempt) {
    return switch (rule.draft().action()) {
      case CreateTaskAction action -> taskOutcome(owner, rule, event, action, attempt);
      case NotifyWebhookAction action -> notifyOutcome(owner, rule, event, action, attempt);
    };
  }

  /** A retried run keeps the identity of its row and only bumps the attempt number. */
  private record Attempt(UUID runId, int number) {
    static Attempt after(AutomationRun prior) {
      return prior == null
          ? new Attempt(UUID.randomUUID(), FIRST_ATTEMPT)
          : new Attempt(prior.id(), prior.attempt() + 1);
    }
  }

  private AutomationOutcome taskOutcome(
      String owner,
      AutomationRule rule,
      AutomationEvent event,
      CreateTaskAction action,
      Attempt attempt) {
    var preview = rendering.preview(owner, action, event);
    if (preview.wouldFail() != null)
      return failed(owner, rule, event, preview.wouldFail(), attempt);
    return new AutomationOutcome(
        run(owner, rule.id(), event, SUCCEEDED, null, attempt),
        new AutomationEffect.CreateTask(
            preview.projectId(),
            preview.title(),
            preview.completionCriterion(),
            preview.estimatedMinutes()));
  }

  private AutomationOutcome notifyOutcome(
      String owner,
      AutomationRule rule,
      AutomationEvent event,
      NotifyWebhookAction action,
      Attempt attempt) {
    // A deleted endpoint and a disabled one look the same from here, and the contract gives both
    // the same code: the rule can no longer reach an active endpoint of this owner.
    if (!endpoints.isActiveEndpointOf(owner, action.endpointId()))
      return failed(owner, rule, event, ENDPOINT_NOT_FOUND, attempt);
    return new AutomationOutcome(
        run(owner, rule.id(), event, SUCCEEDED, null, attempt),
        new AutomationEffect.Notify(action.endpointId(), event));
  }

  /** A deterministic failure: it is settled at the first attempt and never retried. */
  private AutomationOutcome failed(
      String owner, AutomationRule rule, AutomationEvent event, String errorCode, Attempt attempt) {
    return new AutomationOutcome(
        run(owner, rule.id(), event, FAILED, errorCode, attempt), new AutomationEffect.None());
  }

  private AutomationRun run(
      String owner,
      UUID ruleId,
      AutomationEvent event,
      String status,
      String errorCode,
      Attempt attempt) {
    return new AutomationRun(
        attempt.runId(),
        ruleId,
        owner,
        event.eventId(),
        event.eventType(),
        event.occurredAt(),
        attempt.number(),
        status,
        null,
        null,
        errorCode,
        CustomizationTime.capture(clock));
  }

  private static AutomationCursor reachedBy(AutomationEvent event) {
    return new AutomationCursor(event.occurredAt(), event.eventId());
  }
}
