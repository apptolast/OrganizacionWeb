package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

/** Owner-scoped lookups that turn an indirect reference of an event into a task or a project. */
public interface AutomationEventProjects {
  Optional<UUID> projectOfTask(String owner, UUID taskId);

  Optional<UUID> taskOfWorkSession(String owner, UUID sessionId);
}
