package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class SimulateAutomationTest {
  private static final UUID P = UUID.randomUUID();
  private static final UUID COMPLETED = UUID.randomUUID();
  private static final UUID TASK = UUID.randomUUID();
  private static final UUID LONG_TASK = UUID.randomUUID();
  private static final UUID AUTOMATED_TASK = UUID.randomUUID();
  private static final UUID ENDPOINT = UUID.randomUUID();
  private static final Instant T1 = Instant.parse("2026-09-08T10:15:30.123456Z");
  private static final String LONG_TITLE = "x".repeat(150);

  private final List<AutomationEvent> tail = new ArrayList<>();
  private int requestedLimit;

  private final AutomationEventTail events =
      (owner, limit) -> {
        requestedLimit = limit;
        return owner.equals("a") ? newest(limit) : List.of();
      };

  private static final Comparator<AutomationEvent> NEWEST_FIRST =
      Comparator.comparing(AutomationEvent::occurredAt)
          .thenComparing(AutomationEvent::eventId)
          .reversed();

  /**
   * The tail as the real adapter serves it: {@code ORDER BY occurred_at DESC, event_id DESC LIMIT
   * n}. Honouring the limit is what lets the window of @s31 be measured instead of echoed.
   */
  private List<AutomationEvent> newest(int limit) {
    var kept = new HashSet<>(tail.stream().sorted(NEWEST_FIRST).limit(limit).toList());
    return tail.stream().filter(kept::contains).toList();
  }

  private final AutomationFacts facts =
      new AutomationFacts() {
        @Override
        public Optional<String> projectName(String owner, UUID projectId) {
          if (projectId.equals(P)) return Optional.of("Marketing");
          return projectId.equals(COMPLETED) ? Optional.of("Cerrado") : Optional.empty();
        }

        @Override
        public Optional<String> taskTitle(String owner, UUID taskId) {
          if (taskId.equals(TASK) || taskId.equals(AUTOMATED_TASK))
            return Optional.of("Redactar informe");
          return taskId.equals(LONG_TASK) ? Optional.of(LONG_TITLE) : Optional.empty();
        }

        @Override
        public boolean projectCompleted(String owner, UUID projectId) {
          return projectId.equals(COMPLETED);
        }
      };

  private final AutomationEventProjects projects =
      new AutomationEventProjects() {
        @Override
        public Optional<UUID> projectOfTask(String owner, UUID taskId) {
          return Optional.of(P);
        }

        @Override
        public Optional<UUID> taskOfWorkSession(String owner, UUID sessionId) {
          return Optional.empty();
        }
      };

  private final AutomationLoopGuard guard = (owner, taskId) -> taskId.equals(AUTOMATED_TASK);
  private final AutomationMatcher matcher = new AutomationMatcher(projects, guard);
  private final InMemoryAutomations rules = new InMemoryAutomations();
  private final AutomationTargets targets =
      (owner, project) -> owner.equals("a") && (project.equals(P) || project.equals(COMPLETED));
  private final WebhookEndpointLookup endpoints =
      (owner, endpoint) -> owner.equals("a") && endpoint.equals(ENDPOINT);
  private final SimulateAutomation simulate =
      new SimulateAutomation(events, matcher, facts, targets, endpoints);

  private AutomationEvent taskCreated(UUID eventId, Instant occurredAt, UUID taskId) {
    return new AutomationEvent(
        eventId,
        "a",
        "TaskCreated.v1",
        P,
        occurredAt,
        Map.of("taskId", taskId.toString(), "title", "Redactar informe"));
  }

  private static AutomationDraft rule(UUID target, String title, String criterion) {
    return new AutomationDraft(
        "R", true, "TaskCreated.v1", null, new CreateTaskAction(target, title, criterion, 30));
  }

  @Test
  void s30_previewsEveryMatchNewestFirstWithoutTouchingTheRules() {
    var newest = UUID.fromString("00000000-0000-4000-8000-000000000002");
    var older = UUID.fromString("00000000-0000-4000-8000-000000000001");
    tail.add(taskCreated(older, T1.minusSeconds(60), TASK));
    tail.add(taskCreated(newest, T1, TASK));
    for (int index = 0; index < 3; index++)
      tail.add(
          new AutomationEvent(
              UUID.randomUUID(),
              "a",
              "ProjectUpdated.v1",
              P,
              T1.minusSeconds(index),
              Map.of("name", "Marketing")));

    var result =
        simulate.simulate(
            "a",
            rule(
                P,
                "Revisar {{task.title}} en {{project.name}}",
                "{{event.type}} a las {{occurredAt}}"));

    assertThat(requestedLimit).isEqualTo(SimulateAutomation.WINDOW);
    assertThat(result.evaluatedEvents()).isEqualTo(5);
    assertThat(result.matches())
        .extracting(AutomationMatch::eventId)
        .containsExactly(newest, older);
    assertThat(result.matches().getFirst())
        .isEqualTo(
            new AutomationMatch(
                newest,
                "TaskCreated.v1",
                T1,
                new ActionPreview.Task(
                    P,
                    "Revisar Redactar informe en Marketing",
                    "TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z",
                    30,
                    null),
                false));
    assertThat(rules.list("a")).isEmpty();
  }

  @Test
  void s30_twoEventsSharingAnInstantAreOrderedByEventIdDescending() {
    var low = UUID.fromString("00000000-0000-4000-8000-00000000000a");
    var high = UUID.fromString("00000000-0000-4000-8000-00000000000b");
    tail.add(taskCreated(low, T1, TASK));
    tail.add(taskCreated(high, T1, TASK));
    assertThat(simulate.simulate("a", rule(P, "{{task.title}}", null)).matches())
        .extracting(AutomationMatch::eventId)
        .containsExactly(high, low);
  }

  @Test
  void s31_countsWhatTheTailReturnsAndMayFindNothing() {
    var empty = simulate.simulate("a", rule(P, "{{task.title}}", null));
    assertThat(empty.evaluatedEvents()).isZero();
    assertThat(empty.matches()).isEmpty();

    for (int index = 0; index < 5; index++)
      tail.add(
          new AutomationEvent(
              UUID.randomUUID(),
              "a",
              "ProjectCreated.v1",
              P,
              T1.minusSeconds(index),
              Map.of("name", "Marketing")));
    var noMatches = simulate.simulate("a", rule(P, "{{task.title}}", null));
    assertThat(noMatches.evaluatedEvents()).isEqualTo(5);
    assertThat(noMatches.matches()).isEmpty();
  }

  /** A deterministic id whose natural order follows the index, so the window is unambiguous. */
  private static UUID eventId(int index) {
    return new UUID(0x4000L, index);
  }

  /**
   * @s31 row 3: 130 events of which the 100 most recent are ProjectUpdated.v1 and the 30 oldest are
   *     TaskCreated.v1 leaves «evaluados 100, coincidencias []». The 100 is measured here, not
   *     echoed from the constant: were the window 130 the matches would stop being empty, and were
   *     it any smaller than 100 the count would drop with it.
   */
  @Test
  void s31_looksAtTheHundredMostRecentEventsAndLeavesTheOlderMatchesOut() {
    for (int index = 0; index < 30; index++)
      tail.add(taskCreated(eventId(index), T1.minusSeconds(1000 - index), TASK));
    for (int index = 0; index < 100; index++)
      tail.add(
          new AutomationEvent(
              eventId(100 + index),
              "a",
              "ProjectUpdated.v1",
              P,
              T1.minusSeconds(500 - index),
              Map.of("name", "Marketing")));

    var result = simulate.simulate("a", rule(P, "Revisar {{task.title}}", null));

    assertThat(result.evaluatedEvents()).isEqualTo(100);
    assertThat(result.matches()).isEmpty();
  }

  /**
   * @s31 row 4: 101 matching events leave «evaluados 100, 100 coincidencias sin el más antiguo».
   *     The oldest one is named, so the boundary of the window is asserted and not just its size.
   */
  @Test
  void s31_theHundredAndFirstEventFallsOutsideTheWindow() {
    for (int index = 0; index < 101; index++)
      tail.add(taskCreated(eventId(index), T1.minusSeconds(200 - index), TASK));
    var oldest = eventId(0);
    var secondOldest = eventId(1);

    var result = simulate.simulate("a", rule(P, "Revisar {{task.title}}", null));

    assertThat(result.evaluatedEvents()).isEqualTo(100);
    assertThat(result.matches()).hasSize(100);
    assertThat(result.matches())
        .extracting(AutomationMatch::eventId)
        .doesNotContain(oldest)
        .contains(secondOldest);
  }

  @Test
  void s32_anticipatesACompletedProjectWithoutRunningAnything() {
    tail.add(taskCreated(UUID.randomUUID(), T1, TASK));
    var preview = onlyPreview(rule(COMPLETED, "{{task.title}}", null));
    assertThat(preview).isInstanceOf(ActionPreview.Task.class);
    assertThat(((ActionPreview.Task) preview).wouldFail()).isEqualTo("PROJECT_COMPLETED");
  }

  @Test
  void s32_anticipatesATooLongTitleAndResolvesItWithoutTruncating() {
    tail.add(taskCreated(UUID.randomUUID(), T1, LONG_TASK));
    var preview = (ActionPreview.Task) onlyPreview(rule(P, "Revisar {{task.title}} ahora", null));
    assertThat(preview.wouldFail()).isEqualTo("TITLE_TOO_LONG");
    assertThat(preview.title()).isEqualTo("Revisar " + LONG_TITLE + " ahora");
    assertThat(preview.completionCriterion()).isEmpty();
  }

  @Test
  void s32_anticipatesATooLongCriterion() {
    tail.add(taskCreated(UUID.randomUUID(), T1, LONG_TASK));
    var criterion = "y".repeat(1900) + "{{task.title}}";
    var preview = (ActionPreview.Task) onlyPreview(rule(P, "Ok", criterion));
    assertThat(preview.wouldFail()).isEqualTo("CRITERION_TOO_LONG");
  }

  @Test
  void s32_reportsTheGuardWithoutHidingTheMatch() {
    tail.add(taskCreated(UUID.randomUUID(), T1, AUTOMATED_TASK));
    var match =
        simulate.simulate("a", rule(P, "Revisar {{task.title}}", null)).matches().getFirst();
    assertThat(match.loopGuarded()).isTrue();
    assertThat(((ActionPreview.Task) match.preview()).title())
        .isEqualTo("Revisar Redactar informe");
  }

  @Test
  void s32_aValidRuleOverAnActiveProjectFailsNothingAndIsNotGuarded() {
    tail.add(taskCreated(UUID.randomUUID(), T1, TASK));
    var match =
        simulate.simulate("a", rule(P, "Revisar {{task.title}}", null)).matches().getFirst();
    assertThat(match.loopGuarded()).isFalse();
    assertThat(((ActionPreview.Task) match.preview()).wouldFail()).isNull();
  }

  @Test
  void s32_theWebhookPreviewOnlyNamesTheEndpointAndTheEvent() {
    var eventId = UUID.randomUUID();
    tail.add(
        new AutomationEvent(
            eventId, "a", "ProjectStatusChanged.v1", P, T1, Map.of("fromStatus", "active")));
    var draft =
        new AutomationDraft(
            "R", true, "ProjectStatusChanged.v1", null, new NotifyWebhookAction(ENDPOINT));
    assertThat(simulate.simulate("a", draft).matches().getFirst().preview())
        .isEqualTo(new ActionPreview.Webhook(ENDPOINT, eventId));
  }

  @Test
  void s33_rejectsForeignReferencesWithTheSameErrorsAsCreating() {
    assertThatThrownBy(() -> simulate.simulate("a", rule(UUID.randomUUID(), "Ok", null)))
        .isInstanceOf(AutomationTargetNotFoundException.class);
    var unknownEndpoint =
        new AutomationDraft(
            "R", true, "ProjectStatusChanged.v1", null, new NotifyWebhookAction(UUID.randomUUID()));
    assertThatThrownBy(() -> simulate.simulate("a", unknownEndpoint))
        .isInstanceOf(WebhookEndpointNotFoundException.class);
  }

  @Test
  void s33_simulatingNeverConsumesAQuotaSlot() {
    var create =
        new CreateAutomation(
            rules, targets, endpoints, java.time.Clock.fixed(T1, java.time.ZoneOffset.UTC));
    for (int index = 0; index < AutomationRuleStore.RULE_LIMIT; index++)
      create.create("a", rule(P, "Revisar", null));
    tail.add(taskCreated(UUID.randomUUID(), T1, TASK));
    assertThat(simulate.simulate("a", rule(P, "Revisar", null)).matches()).hasSize(1);
    assertThat(rules.list("a")).hasSize(AutomationRuleStore.RULE_LIMIT);
  }

  private ActionPreview onlyPreview(AutomationDraft draft) {
    var matches = simulate.simulate("a", draft).matches();
    assertThat(matches).hasSize(1);
    return matches.getFirst().preview();
  }
}
