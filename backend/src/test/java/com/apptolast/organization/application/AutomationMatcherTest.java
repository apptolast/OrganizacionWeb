package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomationMatcherTest {
  private static final UUID P = UUID.randomUUID();
  private static final UUID P2 = UUID.randomUUID();
  private static final UUID TASK_OF_P = UUID.randomUUID();
  private static final UUID TASK_OF_P2 = UUID.randomUUID();
  private static final UUID SESSION_OF_P = UUID.randomUUID();
  private static final UUID SESSION_OF_P2 = UUID.randomUUID();
  private static final UUID AUTOMATED_TASK = UUID.randomUUID();
  private static final Instant OCCURRED = Instant.parse("2026-09-08T10:15:30.123456Z");

  private final Map<UUID, UUID> projectOfTask =
      Map.of(TASK_OF_P, P, TASK_OF_P2, P2, AUTOMATED_TASK, P);
  private final Map<UUID, UUID> projectOfSession = Map.of(SESSION_OF_P, P, SESSION_OF_P2, P2);

  private final AutomationEventProjects projects =
      new AutomationEventProjects() {
        @Override
        public Optional<UUID> projectOfTask(String owner, UUID taskId) {
          return owner.equals("a")
              ? Optional.ofNullable(projectOfTask.get(taskId))
              : Optional.empty();
        }

        @Override
        public Optional<UUID> projectOfWorkSession(String owner, UUID sessionId) {
          return owner.equals("a")
              ? Optional.ofNullable(projectOfSession.get(sessionId))
              : Optional.empty();
        }
      };

  private final AutomationLoopGuard guard =
      (owner, taskId) -> owner.equals("a") && taskId.equals(AUTOMATED_TASK);

  private final AutomationMatcher matcher = new AutomationMatcher(projects, guard);

  private static AutomationEvent event(
      String owner, String type, UUID aggregateId, Map<String, Object> payload) {
    return new AutomationEvent(UUID.randomUUID(), owner, type, aggregateId, OCCURRED, payload);
  }

  private static AutomationDraft rule(String eventType, UUID conditionProjectId) {
    return new AutomationDraft(
        "R", true, eventType, conditionProjectId, new CreateTaskAction(P, "Revisar", null, null));
  }

  private static Map<String, Object> withTask(UUID taskId) {
    return Map.of("taskId", taskId.toString(), "title", "Redactar informe");
  }

  @Test
  void s27_theConditionComparesTheProjectResolvedForEachEventType() {
    assertThat(
            matcher.matches(
                "a",
                rule("TaskCreated.v1", P),
                event("a", "TaskCreated.v1", P, withTask(TASK_OF_P))))
        .isTrue();
    assertThat(
            matcher.matches(
                "a",
                rule("TaskCreated.v1", P),
                event("a", "TaskCreated.v1", P2, withTask(TASK_OF_P2))))
        .isFalse();
    assertThat(
            matcher.matches(
                "a",
                rule("WorkSessionStarted.v1", P),
                event(
                    "a",
                    "WorkSessionStarted.v1",
                    SESSION_OF_P,
                    Map.of("projectId", P.toString(), "taskId", TASK_OF_P.toString()))))
        .isTrue();
    assertThat(
            matcher.matches(
                "a",
                rule("BlockPlanned.v1", P),
                event("a", "BlockPlanned.v1", P, Map.of("taskId", TASK_OF_P.toString()))))
        .isTrue();
    assertThat(
            matcher.matches(
                "a",
                rule("BlockChanged.v1", P),
                event("a", "BlockChanged.v1", P2, Map.of("taskId", TASK_OF_P2.toString()))))
        .isFalse();
    assertThat(
            matcher.matches(
                "a",
                rule("WorkSessionClosed.v1", P),
                event("a", "WorkSessionClosed.v1", SESSION_OF_P, Map.of())))
        .isTrue();
    assertThat(
            matcher.matches(
                "a",
                rule("WorkSessionExtended.v1", P),
                event("a", "WorkSessionExtended.v1", SESSION_OF_P2, Map.of())))
        .isFalse();
    assertThat(
            matcher.matches(
                "a", rule("ProjectUpdated.v1", P), event("a", "ProjectUpdated.v1", P, Map.of())))
        .isTrue();
  }

  @Test
  void s27_anEventOfAnotherOwnerNeverMatches() {
    assertThat(
            matcher.matches(
                "a",
                rule("TaskCreated.v1", null),
                event("b", "TaskCreated.v1", P, withTask(TASK_OF_P))))
        .isFalse();
  }

  @Test
  void s27_aNullConditionMatchesAnyEventOfTheTriggerType() {
    assertThat(
            matcher.matches(
                "a",
                rule("TaskCreated.v1", null),
                event("a", "TaskCreated.v1", P2, withTask(TASK_OF_P2))))
        .isTrue();
    assertThat(
            matcher.matches(
                "a",
                rule("TaskCreated.v1", null),
                event("a", "TaskStatusChanged.v1", P, withTask(TASK_OF_P))))
        .isFalse();
  }

  @Test
  void s24_aDisabledRuleNeverMatches() {
    var disabled =
        new AutomationDraft(
            "R", false, "TaskCreated.v1", null, new CreateTaskAction(P, "Revisar", null, null));
    assertThat(matcher.matches("a", disabled, event("a", "TaskCreated.v1", P, withTask(TASK_OF_P))))
        .isFalse();
  }

  @Test
  void s28_s29_theGuardOnlyBlocksCreationEventsOfAutomatedTasks() {
    assertThat(matcher.loopGuarded("a", event("a", "TaskCreated.v1", P, withTask(AUTOMATED_TASK))))
        .isTrue();
    assertThat(
            matcher.loopGuarded("a", event("a", "SubtaskCreated.v1", P, withTask(AUTOMATED_TASK))))
        .isTrue();
    assertThat(matcher.loopGuarded("a", event("a", "TaskCreated.v1", P, withTask(TASK_OF_P))))
        .isFalse();
    assertThat(
            matcher.loopGuarded(
                "a",
                event("a", "TaskStatusChanged.v1", P, Map.of("taskId", AUTOMATED_TASK.toString()))))
        .isFalse();
    assertThat(matcher.loopGuarded("a", event("a", "ProjectUpdated.v1", P, Map.of()))).isFalse();
  }
}
