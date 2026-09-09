package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExecuteAutomationsTest {
  private static final String OWNER = "a";
  private static final UUID P = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID TASK = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID E0 = UUID.fromString("00000000-0000-4000-8000-000000000000");
  private static final UUID E1 = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final UUID E2 = UUID.fromString("00000000-0000-4000-8000-000000000002");
  private static final Instant T0 = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final Instant CREATED = T0.minusSeconds(3600);
  private static final Instant OCCURRED = Instant.parse("2026-09-08T10:15:30.123456Z");
  private static final UUID COMPLETED = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final UUID ENDPOINT = UUID.fromString("44444444-4444-4444-8444-444444444444");
  private static final UUID RUN = UUID.fromString("55555555-5555-4555-8555-555555555555");
  private static final UUID AUTOMATED = UUID.fromString("66666666-6666-4666-8666-666666666666");

  private String projectName = "Marketing";
  private String taskTitle = "Redactar informe";

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
          return Optional.of(projectName);
        }

        @Override
        public Optional<String> taskTitle(String owner, UUID taskId) {
          return Optional.of(taskTitle);
        }

        @Override
        public boolean projectCompleted(String owner, UUID projectId) {
          return projectId.equals(COMPLETED);
        }
      };
  private final List<UUID> guardConsultations = new ArrayList<>();
  private final AutomationLoopGuard guard =
      (owner, taskId) -> {
        guardConsultations.add(taskId);
        return AUTOMATED.equals(taskId);
      };
  private final AutomationMatcher matcher = new AutomationMatcher(projects, guard);
  private final WebhookEndpointLookup endpoints = (owner, endpoint) -> false;
  private final Clock clock = Clock.fixed(T0.plusSeconds(3600), ZoneOffset.UTC);
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

  @Test
  void s17_walksInTupleOrderSkippingTheBlockedRowAndTheLateCommitBehindTheCursor() {
    givenARuleThatCreatesTasks();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    var e1 = numbered(1);
    var e2 = numbered(2);
    var blocked = numbered(3);
    var e4 = numbered(4);
    var late = numbered(9);
    work.outbox.add(taskCreated(e2, T0.plusSeconds(1)));
    work.outbox.add(taskCreated(e1, T0.plusSeconds(1)));
    work.outbox.add(blockedTaskCreated(blocked, T0.plusSeconds(2)));
    work.outbox.add(taskCreated(e4, T0.plusSeconds(3)));
    work.outbox.add(taskCreated(late, T0.minusSeconds(1)));

    execute.runCycle();

    assertThat(work.runs()).extracting(AutomationRun::eventId).containsExactly(e1, e2, e4);
    assertThat(work.runs())
        .extracting(AutomationRun::executedAt)
        .as("executedAt never goes backwards along the walk")
        .isSorted();
    assertThat(work.createdTasks()).hasSize(3);
    assertThat(work.commits)
        .extracting(commit -> commit.reached().eventId(), commit -> commit.outcomes().size())
        .as("the blocked row moves the cursor without producing anything")
        .containsExactly(tuple(e1, 1), tuple(e2, 1), tuple(blocked, 0), tuple(e4, 1));
  }

  @ParameterizedTest
  @CsvSource({
    "Marketing, Revisar Redactar informe en Marketing",
    "Marketing 2027, Revisar Redactar informe en Marketing 2027"
  })
  void s18_theTemplateResolvesWithTheValuesInForceAtTheInstantOfTheRun(
      String nameBeforeTheCycle, String title) {
    work.owners.add(OWNER);
    rules.create(
        OWNER,
        rule(
            new CreateTaskAction(
                P,
                "Revisar {{task.title}} en {{project.name}}",
                "{{event.type}} a las {{occurredAt}}",
                30)));
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, OCCURRED));
    projectName = nameBeforeTheCycle;

    execute.runCycle();

    assertThat(work.createdTasks())
        .containsExactly(
            new AutomationEffect.CreateTask(
                P, title, "TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z", 30));
  }

  @Test
  void s24_aRuleDisabledBeforeTheEventNeverRunsButTheCursorStillMovesPastIt() {
    work.owners.add(OWNER);
    var disabled = ruleWith(false, "Revisar {{task.title}}");
    rules.create(OWNER, disabled);
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));
    var reached = new AutomationCursor(T0.plusSeconds(1), E1);

    execute.runCycle();

    assertThat(work.runs()).isEmpty();
    assertThat(work.createdTasks()).isEmpty();
    assertThat(work.cursors.get(OWNER)).isEqualTo(reached);

    rules.replace(
        OWNER,
        disabled.id(),
        1,
        new AutomationDraft("R", true, "TaskCreated.v1", null, taskAction()),
        T0.plusSeconds(2));
    execute.runCycle();

    assertThat(work.runs()).as("reactivating the rule must not resurrect a passed event").isEmpty();
    assertThat(work.createdTasks()).isEmpty();
    assertThat(work.cursors.get(OWNER)).isEqualTo(reached);
  }

  @Test
  void s24_aPutRacingTheEvaluationLeavesOneWholeVersionAndNeverATaskWithoutARun() {
    var racing =
        new RacingRules(
            ruleWith(true, "Antiguo {{task.title}}"), ruleWith(false, "Nuevo {{task.title}}"));
    work.owners.add(OWNER);
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));

    new ExecuteAutomations(work, racing, matcher, facts, endpoints, clock).runCycle();

    assertThat(racing.reads).as("one snapshot of the rules per event, never two").isEqualTo(1);
    assertThat(work.runs()).extracting(AutomationRun::status).containsExactly("succeeded");
    assertThat(work.createdTasks())
        .as("the task uses one version of the template in full, never a mix")
        .extracting(AutomationEffect.CreateTask::title)
        .containsExactly("Antiguo Redactar informe");
    assertThat(work.createdTasks()).hasSameSizeAs(work.runs());
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), E1));
  }

  @ParameterizedTest
  @CsvSource({
    "completed project, PROJECT_COMPLETED",
    "161 code point title, TITLE_TOO_LONG",
    "2001 code point criterion, CRITERION_TOO_LONG",
    "deleted endpoint, ENDPOINT_NOT_FOUND",
    "disabled endpoint, ENDPOINT_NOT_FOUND"
  })
  void s21_aDeterministicFailureIsFailedAtTheFirstAttemptAndNeverStopsTheOtherRule(
      String situation, String code) {
    var broken = brokenRule(situation);
    var sound = ruleWith(true, "Fijo de R2");
    work.owners.add(OWNER);
    rules.create(OWNER, broken);
    rules.create(OWNER, sound);
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));

    execute.runCycle();

    assertThat(runOf(broken))
        .extracting(
            AutomationRun::attempt,
            AutomationRun::status,
            AutomationRun::errorCode,
            AutomationRun::createdTaskId,
            AutomationRun::deliveryId)
        .containsExactly(1, "failed", code, null, null);
    assertThat(effectOf(broken)).isEqualTo(new AutomationEffect.None());
    assertThat(runOf(sound).status()).isEqualTo("succeeded");
    assertThat(effectOf(sound)).isInstanceOf(AutomationEffect.CreateTask.class);
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), E1));

    execute.runCycle();

    assertThat(work.runs()).as("a deterministic failure is never retried").hasSize(2);
  }

  private AutomationRule brokenRule(String situation) {
    return switch (situation) {
      case "completed project" ->
          ruleFor(new CreateTaskAction(COMPLETED, "Revisar lo cerrado", null, 30));
      case "161 code point title" -> {
        taskTitle = "x".repeat(161);
        yield ruleFor(new CreateTaskAction(P, "{{task.title}}", null, 30));
      }
      case "2001 code point criterion" -> {
        taskTitle = "x".repeat(2001);
        yield ruleFor(new CreateTaskAction(P, "Titulo corto", "{{task.title}}", 30));
      }
      // The port answers the same for a deleted and for a disabled endpoint, and so does the
      // contract: both rows expect ENDPOINT_NOT_FOUND.
      default -> ruleFor(new NotifyWebhookAction(ENDPOINT));
    };
  }

  private AutomationRun runOf(AutomationRule rule) {
    return outcomeOf(rule).run();
  }

  private AutomationEffect effectOf(AutomationRule rule) {
    return outcomeOf(rule).effect();
  }

  private AutomationOutcome outcomeOf(AutomationRule rule) {
    return work.commits.stream()
        .flatMap(commit -> commit.outcomes().stream())
        .filter(outcome -> rule.id().equals(outcome.run().ruleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no run for rule " + rule.id()));
  }

  @Test
  void s20_aFailedConfirmationLeavesNothingBehindAndStrandsTheWalkOnTheEvent() {
    givenARuleThatCreatesTasks();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));
    work.outbox.add(taskCreated(E2, T0.plusSeconds(2)));
    work.failing = commit -> E1.equals(commit.reached().eventId());

    execute.runCycle();

    assertThat(work.commits).as("no task and no new event survive the rollback").isEmpty();
    assertThat(work.recorded)
        .extracting(
            AutomationRun::eventId,
            AutomationRun::attempt,
            AutomationRun::status,
            AutomationRun::errorCode,
            AutomationRun::createdTaskId,
            AutomationRun::deliveryId)
        .containsExactly(tuple(E1, 1, "retry", "STORAGE_UNAVAILABLE", null, null));
    assertThat(work.cursors.get(OWNER))
        .as("the cursor never jumps over the event that did not confirm")
        .isEqualTo(new AutomationCursor(T0, E0));
  }

  @Test
  void s22_theStrandedEventGoesFirstAndItsThirdAttemptSettlesWithoutStoppingTheNewOne() {
    givenARuleThatCreatesTasks();
    var rule = rules.list(OWNER).getFirst();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(withRuns(E1, T0.plusSeconds(1), openRun(rule, 2)));
    work.outbox.add(taskCreated(E2, T0.plusSeconds(2)));
    work.failing = commit -> commit.outcomes().stream().anyMatch(fires -> E1.equals(event(fires)));

    execute.runCycle();

    assertThat(work.attempted)
        .as("the stranded event is tried before the new one")
        .containsExactly(E1, E2);
    assertThat(work.recorded)
        .extracting(
            AutomationRun::id,
            AutomationRun::eventId,
            AutomationRun::attempt,
            AutomationRun::status,
            AutomationRun::errorCode)
        .containsExactly(tuple(RUN, E1, 3, "failed", "STORAGE_UNAVAILABLE"));
    assertThat(work.runs())
        .extracting(AutomationRun::eventId, AutomationRun::attempt, AutomationRun::status)
        .containsExactly(tuple(E2, 1, "succeeded"));
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(2), E2));

    work.failing = commit -> false;
    execute.runCycle();

    assertThat(work.recorded).as("no fourth attempt, ever").hasSize(1);
    assertThat(work.runs()).extracting(AutomationRun::eventId).containsExactly(E2);
  }

  @Test
  void s22_anEventAlreadySettledIsNeverAttemptedAgainWhenTheCursorRereadsIt() {
    givenARuleThatCreatesTasks();
    var rule = rules.list(OWNER).getFirst();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(withRuns(E1, T0.plusSeconds(1), settledRun(rule)));

    execute.runCycle();

    assertThat(work.runs()).as("a settled row is never attempted again").isEmpty();
    assertThat(work.recorded).isEmpty();
    assertThat(work.createdTasks()).isEmpty();
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), E1));
  }

  private static UUID event(AutomationOutcome outcome) {
    return outcome.run().eventId();
  }

  private static AutomationRun openRun(AutomationRule rule, int attempt) {
    return recordedRun(rule, attempt, "retry");
  }

  private static AutomationRun settledRun(AutomationRule rule) {
    return recordedRun(rule, 3, "failed");
  }

  private static AutomationRun recordedRun(AutomationRule rule, int attempt, String status) {
    return new AutomationRun(
        RUN,
        rule.id(),
        OWNER,
        E1,
        "TaskCreated.v1",
        T0.plusSeconds(1),
        attempt,
        status,
        null,
        null,
        "STORAGE_UNAVAILABLE",
        T0.plusSeconds(1));
  }

  private static AutomationCandidate withRuns(UUID eventId, Instant occurredAt, AutomationRun run) {
    return new AutomationCandidate(taskCreated(eventId, occurredAt).event(), false, List.of(run));
  }

  @ParameterizedTest
  @CsvSource({"R1 still exists, 2", "R1 deleted before the event is read, 1"})
  void s28_theTaskCreatedOfAnAutomatedTaskFiresNoRuleAtAll(String state, int surviving) {
    work.owners.add(OWNER);
    for (int index = 0; index < surviving; index++) rules.create(OWNER, ruleWith(true, "Encadena"));
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreatedOf(E1, T0.plusSeconds(1), AUTOMATED));

    execute.runCycle();

    assertThat(work.runs()).as(state).isEmpty();
    assertThat(work.createdTasks()).isEmpty();
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), E1));
  }

  @Test
  void s29_aHumanChangeOnTheAutomatedTaskDoesFireAndNeverConsultsTheGuard() {
    work.owners.add(OWNER);
    rules.create(OWNER, ruleOn("TaskStatusChanged.v1"));
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(statusChanged(E1, T0.plusSeconds(1), AUTOMATED));

    execute.runCycle();

    assertThat(work.runs())
        .extracting(AutomationRun::eventId, AutomationRun::status)
        .containsExactly(tuple(E1, "succeeded"));
    assertThat(guardConsultations)
        .as("the guard is only ever asked about TaskCreated.v1 and SubtaskCreated.v1")
        .isEmpty();
  }

  private static AutomationRule ruleOn(String eventType) {
    return new AutomationRule(
        UUID.randomUUID(),
        new AutomationDraft(
            "R", true, eventType, null, new CreateTaskAction(P, "Revisar de nuevo", null, 30)),
        1,
        CREATED,
        CREATED);
  }

  private static AutomationCandidate statusChanged(UUID eventId, Instant occurredAt, UUID taskId) {
    return new AutomationCandidate(
        new AutomationEvent(
            eventId,
            OWNER,
            "TaskStatusChanged.v1",
            P,
            occurredAt,
            Map.of("taskId", taskId.toString(), "status", "completed")),
        false,
        List.of());
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

  private static AutomationRule ruleWith(boolean enabled, String titleTemplate) {
    return new AutomationRule(
        UUID.randomUUID(),
        new AutomationDraft(
            "R", enabled, "TaskCreated.v1", null, new CreateTaskAction(P, titleTemplate, null, 30)),
        1,
        CREATED,
        CREATED);
  }

  private static AutomationRule rule(AutomationAction action) {
    return ruleAt(action, CREATED);
  }

  private static AutomationRule ruleFor(AutomationAction action) {
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

  private static AutomationCandidate blockedTaskCreated(UUID eventId, Instant occurredAt) {
    return new AutomationCandidate(taskCreated(eventId, occurredAt).event(), true, List.of());
  }

  private static AutomationCandidate taskCreated(UUID eventId, Instant occurredAt) {
    return taskCreatedOf(eventId, occurredAt, TASK);
  }

  private static AutomationCandidate taskCreatedOf(UUID eventId, Instant occurredAt, UUID taskId) {
    return new AutomationCandidate(
        new AutomationEvent(
            eventId,
            OWNER,
            "TaskCreated.v1",
            P,
            occurredAt,
            Map.of("taskId", taskId.toString(), "title", "Redactar informe")),
        false,
        List.of());
  }

  /** One rule replaced by a PUT between two reads: the race the contract describes. */
  private static final class RacingRules implements AutomationRuleStore {
    private final AutomationRule before;
    private final AutomationRule after;
    int reads;

    RacingRules(AutomationRule before, AutomationRule after) {
      this.before = before;
      this.after = after;
    }

    @Override
    public List<AutomationRule> list(String owner) {
      reads++;
      return List.of(reads == 1 ? before : after);
    }

    @Override
    public AutomationRule create(String owner, AutomationRule rule) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<AutomationRule> find(String owner, UUID id) {
      return list(owner).stream().filter(rule -> rule.id().equals(id)).findFirst();
    }

    @Override
    public AutomationRule replace(
        String owner, UUID id, long expectedVersion, AutomationDraft draft, Instant now) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String owner, UUID id, long expectedVersion) {
      throw new UnsupportedOperationException();
    }
  }

  /** In-memory stand-in for the transactional port: applies effects and advances the cursor. */
  private static final class FakeWork implements AutomationWork {
    final List<String> owners = new ArrayList<>();
    final Map<String, AutomationCursor> cursors = new LinkedHashMap<>();
    final List<AutomationCandidate> outbox = new ArrayList<>();
    final List<AutomationCommit> commits = new ArrayList<>();
    final List<AutomationRun> recorded = new ArrayList<>();
    final List<AutomationCursor> started = new ArrayList<>();
    final List<UUID> attempted = new ArrayList<>();
    java.util.function.Predicate<AutomationCommit> failing = commit -> false;

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
    public void commit(AutomationCommit commit) {
      attempted.add(commit.reached().eventId());
      if (failing.test(commit))
        throw new StorageUnavailableException(new IllegalStateException("induced"));
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
