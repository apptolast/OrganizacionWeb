package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationTemplate;
import com.apptolast.organization.domain.CreateTaskAction;
import com.apptolast.organization.domain.TemplateValues;

/** Resolves the templates of an action with the values in force right now, never the historic. */
final class AutomationRendering {
  private final AutomationMatcher matcher;
  private final AutomationFacts facts;

  AutomationRendering(AutomationMatcher matcher, AutomationFacts facts) {
    this.matcher = matcher;
    this.facts = facts;
  }

  ActionPreview.Task preview(String owner, CreateTaskAction action, AutomationEvent event) {
    var values = valuesFor(owner, event);
    var title = AutomationTemplate.render(action.titleTemplate(), values);
    var criterion =
        action.criterionTemplate() == null
            ? ""
            : AutomationTemplate.render(action.criterionTemplate(), values);
    return new ActionPreview.Task(
        action.projectId(),
        title,
        criterion,
        action.estimatedMinutes(),
        failureOf(owner, action, title, criterion));
  }

  private String failureOf(String owner, CreateTaskAction action, String title, String criterion) {
    if (facts.projectCompleted(owner, action.projectId())) return "PROJECT_COMPLETED";
    return action.resolvedFailure(title, criterion).orElse(null);
  }

  private TemplateValues valuesFor(String owner, AutomationEvent event) {
    var projectName =
        matcher
            .projectOf(owner, event)
            .flatMap(project -> facts.projectName(owner, project))
            .orElse(null);
    var taskTitle =
        matcher.taskOf(owner, event).flatMap(task -> facts.taskTitle(owner, task)).orElse(null);
    return new TemplateValues(event.eventType(), event.occurredAt(), taskTitle, projectName);
  }
}
