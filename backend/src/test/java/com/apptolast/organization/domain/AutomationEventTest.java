package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutomationEventTest {
  private static final UUID PROJECT = UUID.randomUUID();
  private static final UUID TASK = UUID.randomUUID();
  private static final UUID SESSION = UUID.randomUUID();
  private static final Instant OCCURRED = Instant.parse("2026-09-08T10:15:30.123456Z");

  private static AutomationEvent event(String type, UUID aggregateId, Map<String, Object> payload) {
    return new AutomationEvent(
        UUID.randomUUID(), "a", type, aggregateId, OCCURRED, Map.copyOf(payload));
  }

  @Test
  void s27_projectAndTaskEventsNameTheirProjectInTheAggregate() {
    for (var type :
        java.util.List.of(
            "ProjectCreated.v1",
            "ProjectUpdated.v1",
            "ProjectStatusChanged.v1",
            "TaskCreated.v1",
            "SubtaskCreated.v1",
            "TaskStatusChanged.v1"))
      assertThat(event(type, PROJECT, Map.of()).projectSource())
          .isEqualTo(new EventProject.Known(PROJECT));
  }

  @Test
  void s27_blockEventsResolveTheProjectThroughTheirTask() {
    var payload = Map.<String, Object>of("taskId", TASK.toString());
    assertThat(event("BlockPlanned.v1", PROJECT, payload).projectSource())
        .isEqualTo(new EventProject.OfTask(TASK));
    assertThat(event("BlockChanged.v1", PROJECT, payload).projectSource())
        .isEqualTo(new EventProject.OfTask(TASK));
  }

  @Test
  void s27_aStartingWorkSessionCarriesItsProjectAndTheRestGoThroughTheSession() {
    assertThat(
            event(
                    "WorkSessionStarted.v1",
                    SESSION,
                    Map.of("projectId", PROJECT.toString(), "taskId", TASK.toString()))
                .projectSource())
        .isEqualTo(new EventProject.Known(PROJECT));
    for (var type :
        java.util.List.of(
            "WorkSessionStateChanged.v1", "WorkSessionExtended.v1", "WorkSessionClosed.v1"))
      assertThat(event(type, SESSION, Map.of()).projectSource())
          .isEqualTo(new EventProject.OfWorkSession(SESSION));
  }

  @Test
  void s6_s18_theTaskOfAnEventIsNamedDirectlyOrReachedThroughItsSession() {
    for (var type :
        java.util.List.of(
            "TaskCreated.v1",
            "SubtaskCreated.v1",
            "TaskStatusChanged.v1",
            "BlockPlanned.v1",
            "BlockChanged.v1",
            "WorkSessionStarted.v1"))
      assertThat(event(type, PROJECT, Map.of("taskId", TASK.toString())).taskSource())
          .hasValue(new EventTask.Known(TASK));
    for (var type :
        java.util.List.of(
            "WorkSessionStateChanged.v1", "WorkSessionExtended.v1", "WorkSessionClosed.v1"))
      assertThat(event(type, SESSION, Map.of()).taskSource())
          .hasValue(new EventTask.OfWorkSession(SESSION));
  }

  @Test
  void s7_projectEventsHaveNoTaskAtAll() {
    for (var type :
        java.util.List.of("ProjectCreated.v1", "ProjectUpdated.v1", "ProjectStatusChanged.v1"))
      assertThat(event(type, PROJECT, Map.of("name", "Marketing")).taskSource()).isEmpty();
  }

  @Test
  void s29_onlyTaskCreationEventsConsultTheLoopGuard() {
    var created = Map.<String, Object>of("taskId", TASK.toString(), "title", "Redactar informe");
    assertThat(event("TaskCreated.v1", PROJECT, created).loopGuardTaskId()).hasValue(TASK);
    assertThat(event("SubtaskCreated.v1", PROJECT, created).loopGuardTaskId()).hasValue(TASK);
    assertThat(
            event("TaskStatusChanged.v1", PROJECT, Map.of("taskId", TASK.toString()))
                .loopGuardTaskId())
        .isEmpty();
    assertThat(event("ProjectUpdated.v1", PROJECT, Map.of()).loopGuardTaskId()).isEmpty();
  }
}
