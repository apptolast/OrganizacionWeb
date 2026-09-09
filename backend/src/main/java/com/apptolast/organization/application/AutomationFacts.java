package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

/** The values a template needs, read as they stand now and never from the event history. */
public interface AutomationFacts {
  Optional<String> projectName(String owner, UUID projectId);

  Optional<String> taskTitle(String owner, UUID taskId);

  boolean projectCompleted(String owner, UUID projectId);
}
