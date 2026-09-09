package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExecuteAutomationsTest {
  private static final String OWNER = "a";
  private static final UUID P = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID TASK = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID E0 = UUID.fromString("00000000-0000-4000-8000-000000000000");
  private static final UUID E1 = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID E2 = UUID.fromString("00000000-0000-4000-8000-000000000002");
  private static final Instant T0 = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final Instant CREATED = T0.minusSeconds(3600);

  private final FakeWork work = new FakeWork();
  private final InMemoryAutomations rules = new InMemoryAutomations();
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
  private final AutomationFacts facts =
      new AutomationFacts() {
        @Override
        public Optional<String> projectName(String owner, UUID projectId) {
          return Optional.of("Marketing");
        }

        @Override
        public Optional<String> taskTitle(String owner, UUID taskId) {
          return Optional.of("Redactar informe");
        }

        @Override
        public boolean projectCompleted(String owner, UUID projectId) {
          return false;
        }
      };
  private final AutomationLoopGuard guard = (owner, taskId) -> false;
  private final AutomationMatcher matcher = new AutomationMatcher(projects, guard);
  private final WebhookEndpointLookup endpoints = (owner, endpoint) -> false;
  private final Clock clock = Clock.fixed(T0.plusSeconds(600), ZoneOffset.UTC);
  private final ExecuteAutomations execute =
      new ExecuteAutomations(work, rules, matcher, facts, endpoints, clock);

  @Test
  void s15_aCycleExecutesEveryEventAfterTheCursorAndLeavesTheCursorAtTheLast() {
    givenARuleThatCreatesTasks();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));
    work.outbox.add(taskCreated(E2, T0.plusSeconds(2)));

    execute.runCycle();

    assertThat(work.runs())
        .extracting(AutomationRun::eventId, AutomationRun::status, AutomationRun::attempt)
        .containsExactly(tuple(E1, "succeeded", 1), tuple(E2, "succeeded", 1));
    assertThat(work.createdTasks()).hasSize(2);
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(2), E2));
  }

  @Test
  void s16_theFirstRuleStartsTheCursorAtItsOwnInstantAndLeavesTheFortyEarlierEventsAlone() {
    work.owners.add(OWNER);
    rules.create(OWNER, ruleAt(taskAction(), T0));
    for (int age = 1; age <= 40; age++)
      work.outbox.add(taskCreated(numbered(age), T0.minusSeconds(age)));
    var e41 = numbered(41);
    work.outbox.add(taskCreated(e41, T0.plusSeconds(1)));

    execute.runCycle();

    assertThat(work.started)
        .containsExactly(new AutomationCursor(T0, AutomationCursor.START))
        .as("the cursor of a brand new owner starts at the instant of their first rule");
    assertThat(work.runs()).extracting(AutomationRun::eventId).containsExactly(e41);
    assertThat(work.createdTasks()).hasSize(1);
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), e41));
  }

  private void givenARuleThatCreatesTasks() {
    work.owners.add(OWNER);
    rules.create(OWNER, rule(taskAction()));
  }

  private static UUID numbered(int index) {
    return UUID.fromString("00000000-0000-4000-8000-%012d".formatted(index));
  }

  private static CreateTaskAction taskAction() {
    return new CreateTaskAction(P, "Revisar {{task.title}} en {{project.name}}", null, 30);
  }

  private static AutomationRule rule(AutomationAction action) {
    return ruleAt(action, CREATED);
  }

  private static AutomationRule ruleAt(AutomationAction action, Instant createdAt) {
    return new AutomationRule(
        UUID.randomUUID(),
        new AutomationDraft("R", true, "TaskCreated.v1", null, action),
        1,
        createdAt,
        createdAt);
  }

  private static AutomationCandidate taskCreated(UUID eventId, Instant occurredAt) {
    return new AutomationCandidate(
        new AutomationEvent(
            eventId,
            OWNER,
            "TaskCreated.v1",
            P,
            occurredAt,
            Map.of("taskId", TASK.toString(), "title", "Redactar informe")),
        false);
  }

  /** In-memory stand-in for the transactional port: applies effects and advances the cursor. */
  private static final class FakeWork implements AutomationWork {
    final List<String> owners = new ArrayList<>();
    final Map<String, AutomationCursor> cursors = new LinkedHashMap<>();
    final List<AutomationCandidate> outbox = new ArrayList<>();
    final List<AutomationCommit> commits = new ArrayList<>();
    final List<AutomationRun> recorded = new ArrayList<>();
    final List<AutomationCursor> started = new ArrayList<>();

    @Override
    public List<String> ownersWithRules() {
      return List.copyOf(owners);
    }

    @Override
    public Optional<AutomationCursor> cursor(String owner) {
      return Optional.ofNullable(cursors.get(owner));
    }

    @Override
    public void startCursor(String owner, AutomationCursor present) {
      started.add(present);
      cursors.put(owner, present);
    }

    @Override
    public List<AutomationCandidate> after(String owner, AutomationCursor from) {
      return outbox.stream()
          .filter(candidate -> candidate.event().ownerId().equals(owner))
          .filter(candidate -> from.precedes(candidate.event().occurredAt(), eventId(candidate)))
          // Mirrors PostgreSQL: instants first, then uuid as unsigned bytes.
          .sorted(
              Comparator.comparing(
                      (AutomationCandidate candidate) -> candidate.event().occurredAt())
                  .thenComparing(FakeWork::eventId, WebhookCursor::compareUnsigned))
          .toList();
    }

    @Override
    public List<AutomationRetry> pendingRetries(String owner) {
      return List.of();
    }

    @Override
    public void commit(AutomationCommit commit) {
      commits.add(commit);
      if (commit.reached() != null) cursors.put(commit.owner(), commit.reached());
    }

    @Override
    public void record(AutomationRun run) {
      recorded.add(run);
    }

    List<AutomationRun> runs() {
      return commits.stream()
          .flatMap(commit -> commit.outcomes().stream())
          .map(AutomationOutcome::run)
          .toList();
    }

    List<AutomationEffect.CreateTask> createdTasks() {
      return commits.stream()
          .flatMap(commit -> commit.outcomes().stream())
          .map(AutomationOutcome::effect)
          .filter(AutomationEffect.CreateTask.class::isInstance)
          .map(AutomationEffect.CreateTask.class::cast)
          .toList();
    }

    private static UUID eventId(AutomationCandidate candidate) {
      return candidate.event().eventId();
    }
  }
}
