package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ActionPreview;
import com.apptolast.organization.application.AutomationMatch;
import com.apptolast.organization.application.AutomationSimulation;
import com.apptolast.organization.domain.AutomationAction;
import com.apptolast.organization.domain.AutomationRule;
import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.NotifyWebhookAction;
import java.util.List;
import java.util.UUID;

/**
 * The closed wire shapes. Every key is always written, with an explicit null where it does not
 * apply, and each variant is its own record so it can never leak the other one's keys. Instants
 * travel as their own ISO-8601 text so the microsecond resolution never depends on a serializer
 * setting.
 */
final class AutomationView {
  private AutomationView() {}

  record Trigger(String eventType) {}

  record Condition(UUID projectId) {}

  sealed interface ActionView {}

  record TaskAction(
      String type,
      UUID projectId,
      String titleTemplate,
      String criterionTemplate,
      Integer estimatedMinutes)
      implements ActionView {}

  record WebhookAction(String type, UUID endpointId) implements ActionView {}

  record Rule(
      UUID id,
      String name,
      boolean enabled,
      Trigger trigger,
      Condition condition,
      ActionView action,
      long version,
      String createdAt,
      String updatedAt) {}

  record Rules(List<Rule> items) {}

  record Run(
      UUID id,
      UUID eventId,
      String eventType,
      String occurredAt,
      int attempt,
      String status,
      UUID createdTaskId,
      UUID deliveryId,
      String errorCode,
      String executedAt) {}

  record Runs(List<Run> items, String nextCursor) {}

  sealed interface PreviewView {}

  record TaskPreview(
      String type,
      UUID projectId,
      String title,
      String completionCriterion,
      Integer estimatedMinutes,
      String wouldFail)
      implements PreviewView {}

  record WebhookPreview(String type, UUID endpointId, UUID eventId) implements PreviewView {}

  record Match(
      UUID eventId,
      String eventType,
      String occurredAt,
      PreviewView preview,
      boolean loopGuarded) {}

  record Simulation(int evaluatedEvents, List<Match> matches) {}

  static Rule of(AutomationRule rule) {
    var draft = rule.draft();
    return new Rule(
        rule.id(),
        draft.name(),
        draft.enabled(),
        new Trigger(draft.eventType()),
        draft.conditionProjectId() == null ? null : new Condition(draft.conditionProjectId()),
        actionOf(draft.action()),
        rule.version(),
        rule.createdAt().toString(),
        rule.updatedAt().toString());
  }

  private static ActionView actionOf(AutomationAction action) {
    return switch (action) {
      case CreateTaskAction task ->
          new TaskAction(
              task.type(),
              task.projectId(),
              task.titleTemplate(),
              task.criterionTemplate(),
              task.estimatedMinutes());
      case NotifyWebhookAction webhook -> new WebhookAction(webhook.type(), webhook.endpointId());
    };
  }

  static Run of(AutomationRun run) {
    return new Run(
        run.id(),
        run.eventId(),
        run.eventType(),
        run.occurredAt().toString(),
        run.attempt(),
        run.status(),
        run.createdTaskId(),
        run.deliveryId(),
        run.errorCode(),
        run.executedAt().toString());
  }

  static Simulation of(AutomationSimulation simulation) {
    return new Simulation(
        simulation.evaluatedEvents(),
        simulation.matches().stream().map(AutomationView::of).toList());
  }

  private static Match of(AutomationMatch match) {
    return new Match(
        match.eventId(),
        match.eventType(),
        match.occurredAt().toString(),
        previewOf(match.preview()),
        match.loopGuarded());
  }

  private static PreviewView previewOf(ActionPreview preview) {
    return switch (preview) {
      case ActionPreview.Task task ->
          new TaskPreview(
              "CREATE_TASK",
              task.projectId(),
              task.title(),
              task.completionCriterion(),
              task.estimatedMinutes(),
              task.wouldFail());
      case ActionPreview.Webhook webhook ->
          new WebhookPreview("NOTIFY_WEBHOOK", webhook.endpointId(), webhook.eventId());
    };
  }
}
