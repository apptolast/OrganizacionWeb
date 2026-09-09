package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class AutomationEventTypeTest {
  @Test
  void s2_acceptsExactlyTheTwelvePublishedTypes() {
    var published =
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
    for (var type : published) assertThat(AutomationEventType.of(type)).hasValue(type);
    for (var other : List.of("TaskCreated.v2", "taskcreated.v1", "webhook.ping.v1", ""))
      assertThat(AutomationEventType.of(other)).isEmpty();
  }
}
