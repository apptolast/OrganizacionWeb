package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.NotifyWebhookAction;
import java.util.Comparator;

/** Evaluates a rule against the recent past without persisting it or producing any effect. */
public final class SimulateAutomation implements SimulateAutomationUseCase {
  /** How far back a dry run looks; the same number the contract publishes. */
  public static final int WINDOW = 100;

  private static final Comparator<AutomationEvent> NEWEST_FIRST =
      Comparator.comparing(AutomationEvent::occurredAt)
          .thenComparing(AutomationEvent::eventId)
          .reversed();

  private final AutomationEventTail events;
  private final AutomationMatcher matcher;
  private final AutomationRendering rendering;
  private final AutomationReferences references;

  public SimulateAutomation(
      AutomationEventTail events,
      AutomationMatcher matcher,
      AutomationFacts facts,
      AutomationTargets targets,
      WebhookEndpointLookup endpoints) {
    this.events = events;
    this.matcher = matcher;
    this.rendering = new AutomationRendering(matcher, facts);
    this.references = new AutomationReferences(targets, endpoints);
  }

  @Override
  public AutomationSimulation simulate(String owner, AutomationDraft draft) {
    references.check(owner, draft);
    var recent = events.recent(owner, WINDOW);
    var matches =
        recent.stream()
            .filter(event -> matcher.matches(owner, draft, event))
            .sorted(NEWEST_FIRST)
            .map(event -> match(owner, draft, event))
            .toList();
    return new AutomationSimulation(recent.size(), matches);
  }

  private AutomationMatch match(String owner, AutomationDraft draft, AutomationEvent event) {
    return new AutomationMatch(
        event.eventId(),
        event.eventType(),
        event.occurredAt(),
        preview(owner, draft, event),
        matcher.loopGuarded(owner, event));
  }

  private ActionPreview preview(String owner, AutomationDraft draft, AutomationEvent event) {
    return switch (draft.action()) {
      case CreateTaskAction action -> rendering.preview(owner, action, event);
      case NotifyWebhookAction action ->
          new ActionPreview.Webhook(action.endpointId(), event.eventId());
    };
  }
}
