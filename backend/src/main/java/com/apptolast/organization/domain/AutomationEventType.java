package com.apptolast.organization.domain;

import java.util.List;
import java.util.Optional;

/** Closed catalog of the twelve event types a rule may listen to. */
public final class AutomationEventType {
  public static final List<String> PUBLISHED =
      List.of(
          "ProjectCreated.v1",
          "ProjectUpdated.v1",
          "ProjectStatusChanged.v1",
          "TaskCreated.v1",
          "SubtaskCreated.v1",
          "TaskStatusChanged.v1",
          "BlockPlanned.v1",
          "BlockChanged.v1",
          "WorkSessionStarted.v1",
          "WorkSessionStateChanged.v1",
          "WorkSessionExtended.v1",
          "WorkSessionClosed.v1");

  private AutomationEventType() {}

  public static Optional<String> of(String raw) {
    return PUBLISHED.contains(raw) ? Optional.of(raw) : Optional.empty();
  }
}
