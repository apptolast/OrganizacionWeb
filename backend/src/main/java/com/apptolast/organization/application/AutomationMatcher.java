package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.EventProject;
import java.util.Optional;
import java.util.UUID;

/** Decides whether an event fires a rule, and whether it must be skipped to keep depth at one. */
public final class AutomationMatcher {
  private final AutomationEventProjects projects;
  private final AutomationLoopGuard guard;

  public AutomationMatcher(AutomationEventProjects projects, AutomationLoopGuard guard) {
    this.projects = projects;
    this.guard = guard;
  }

  public boolean matches(String owner, AutomationDraft rule, AutomationEvent event) {
    if (!rule.enabled() || !owner.equals(event.ownerId())) return false;
    if (!rule.eventType().equals(event.eventType())) return false;
    return rule.conditionProjectId() == null
        || projectOf(owner, event).filter(rule.conditionProjectId()::equals).isPresent();
  }

  public boolean loopGuarded(String owner, AutomationEvent event) {
    return event
        .loopGuardTaskId()
        .filter(task -> guard.createdByAutomation(owner, task))
        .isPresent();
  }

  /** Empty when the reference does not resolve inside this owner's data. */
  public Optional<UUID> projectOf(String owner, AutomationEvent event) {
    return switch (event.projectSource()) {
      case EventProject.Known known -> Optional.of(known.projectId());
      case EventProject.OfTask task -> projects.projectOfTask(owner, task.taskId());
      case EventProject.OfWorkSession session ->
          projects.projectOfWorkSession(owner, session.sessionId());
    };
  }
}
