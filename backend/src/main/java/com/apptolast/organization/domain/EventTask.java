package com.apptolast.organization.domain;

import java.util.UUID;

/** Where the task of an event has to be looked for, when the event has one at all. */
public sealed interface EventTask {
  /** The event already names its task. */
  record Known(UUID taskId) implements EventTask {}

  /** The task is the one this work session runs on. */
  record OfWorkSession(UUID sessionId) implements EventTask {}
}
