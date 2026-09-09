package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

/** Owner-scoped lookups that turn an indirect reference of an event into its project. */
public interface AutomationEventProjects {
  Optional<UUID> projectOfTask(String owner, UUID taskId);

  Optional<UUID> projectOfWorkSession(String owner, UUID sessionId);
}
