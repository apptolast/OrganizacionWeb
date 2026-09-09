package com.apptolast.organization.domain;

import java.util.UUID;

/** Where the project of an event has to be looked for; the lookup itself lives in an adapter. */
public sealed interface EventProject {
  /** The event already names its project. */
  record Known(UUID projectId) implements EventProject {}

  /** The project is the one owning this task. */
  record OfTask(UUID taskId) implements EventProject {}

  /** The project is the one owning the task of this work session. */
  record OfWorkSession(UUID sessionId) implements EventProject {}
}
