package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
  private static final UUID SIBLING_RUN = UUID.fromString("77777777-7777-4777-8777-777777777777");
  private static final UUID AUTOMATED = UUID.fromString("66666666-6666-4666-8666-666666666666");

  private String projectName = "Marketing";
  private String taskTitle = "Redactar informe";

  /** A title per task, so several events of one walk stop being indistinguishable. */
  private final Map<UUID, String> titleOfTask = new HashMap<>();

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
          return Optional.of(titleOfTask.getOrDefault(taskId, taskTitle));
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
  // El adaptador real, no un doble: @s19 afirma sobre la LINEA que se emite, y un
  // doble grabador solo probaria lo que el caso de uso pasa, no lo que se escribe.
  private final AutomationAudit audit =
      new com.apptolast.organization.adapter.logging.Slf4jAutomationAudit();
  private final Clock clock = Clock.fixed(T0.plusSeconds(3600), ZoneOffset.UTC);
  private final ExecuteAutomations execute =
      new ExecuteAutomations(work, rules, matcher, facts, endpoints, audit, clock);

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final ch.qos.logback.classic.Logger logger =
      (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("organization.automations");

  @BeforeEach
  void captureTheLog() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void releaseTheLog() {
    logger.detachAppender(appender);
  }

  @Test
  void s19_theLogNamesTheRunByItsIdentifiersAndNeverByTheTitleOrTheProjectName() {
    givenARuleThatCreatesTasks();
    var rule = rules.list(OWNER).getFirst();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));

    execute.runCycle();

    assertThat(logged())
        .contains(rule.id().toString(), E1.toString(), "succeeded", "attempt=1", "code=null")
        .doesNotContain("Redactar informe", "Marketing");
  }

  private String logged() {
    return appender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .collect(java.util.stream.Collectors.joining("\n"));
  }

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
    // Una tarea distinta por evento: con las tres tareas creadas byte a byte
    // iguales, cualquier permutacion -o repetir tres veces la misma- pasaba.
    var first = numbered(101);
    var second = numbered(102);
    var fourth = numbered(104);
    titleOfTask.put(first, "Primera");
    titleOfTask.put(second, "Segunda");
    titleOfTask.put(fourth, "Cuarta");
    work.outbox.add(taskCreatedOf(e2, T0.plusSeconds(1), second));
    work.outbox.add(taskCreatedOf(e1, T0.plusSeconds(1), first));
    work.outbox.add(blockedTaskCreated(blocked, T0.plusSeconds(2)));
    work.outbox.add(taskCreatedOf(e4, T0.plusSeconds(3), fourth));
    work.outbox.add(taskCreated(late, T0.minusSeconds(1)));

    execute.runCycle();

    assertThat(work.runs()).extracting(AutomationRun::eventId).containsExactly(e1, e2, e4);
    assertThat(work.runs())
        .extracting(AutomationRun::executedAt)
        .as("executedAt never goes backwards along the walk")
        .isSorted();
    assertThat(work.createdTasks()).hasSize(3);
    // «Y las 3 tareas creadas tienen createdAt en ese mismo orden». El puerto no
    // lleva instante -AutomationEffect.CreateTask no tiene reloj-, asi que lo que
    // se afirma aqui es la condicion que lo produce: cada tarea es la de SU
    // evento y llegan en el orden del paseo, que es el orden en que el adaptador
    // las sella. El instante real sigue sin oraculo (H3 del juez).
    assertThat(work.createdTasks())
        .extracting(AutomationEffect.CreateTask::title)
        .as("las 3 tareas se crean en el orden E1, E2, E4, cada una con su evento")
        .containsExactly(
            "Revisar Primera en Marketing",
            "Revisar Segunda en Marketing",
            "Revisar Cuarta en Marketing");
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

    new ExecuteAutomations(work, racing, matcher, facts, endpoints, audit, clock).runCycle();

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

  /**
   * @s21 en su ámbito mayor: «sin detener las demás reglas» se afirmaba sólo dentro de un evento de
   *     un propietario, y el ciclo no tiene ninguna frontera por propietario. {@code runCycle()}
   *     recorre {@code ownersWithRules()} sin envolver nada, y {@code process()} calcula los
   *     resultados —con {@code matcher.loopGuarded}, que pide {@code uuid("taskId")}— FUERA del
   *     try. Una fila de outbox TaskCreated.v1 sin taskId en el payload revienta con {@link
   *     IllegalArgumentException} y hoy aborta el ciclo entero: todos los propietarios que el
   *     {@code SELECT DISTINCT owner_id} devuelva después de él se quedan sin automatizaciones,
   *     indefinidamente y sin diagnóstico, porque {@code AutomationSchedule.tick} sólo apunta el
   *     nombre de la clase.
   *     <p>Por eso hay dos aserciones y no una: que el siguiente propietario ejecute lo suyo, y que
   *     el fallo quede nombrado por propietario, evento y causa. Un aislamiento mudo cambia una
   *     avería general por una avería invisible.
   */
  @Test
  void s21_aPoisonedEventOfOneOwnerNeverLeavesTheOtherAccountsWithoutAutomations() {
    var poisoned = "b";
    work.owners.add(poisoned);
    rules.create(poisoned, ruleWith(true, "Revisar de nuevo"));
    givenARuleThatCreatesTasks();
    work.cursors.put(poisoned, new AutomationCursor(T0, E0));
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreatedWithoutTaskId(poisoned, E1, T0.plusSeconds(1)));
    work.outbox.add(taskCreated(E2, T0.plusSeconds(2)));

    execute.runCycle();

    assertThat(work.runs())
        .extracting(AutomationRun::eventId, AutomationRun::status)
        .as("el dato envenenado de un propietario no deja sin automatizaciones a los demás")
        .containsExactly(tuple(E2, "succeeded"));
    assertThat(work.cursors.get(poisoned))
        .as("y su propio recorrido se queda donde estaba: nada se salta en silencio")
        .isEqualTo(new AutomationCursor(T0, E0));
    assertThat(logged())
        .as("propietario, evento y causa, que es lo que falta para diagnosticarlo")
        .contains(poisoned, E1.toString(), "IllegalArgumentException");
  }

  /** Una fila de outbox cuyo payload no trae el taskId que su propio tipo de evento promete. */
  private static AutomationCandidate taskCreatedWithoutTaskId(
      String owner, UUID eventId, Instant occurredAt) {
    return new AutomationCandidate(
        new AutomationEvent(
            eventId, owner, "TaskCreated.v1", P, occurredAt, Map.of("title", "Redactar informe")),
        false,
        List.of());
  }

  /**
   * La otra mitad de la frontera: lo que revienta antes de que haya un evento en la mano. {@code
   * walk()} deja escapar lo que lancen {@code work.cursor}, {@code startCursor} y {@code
   * work.after}, y ninguno de los tres está dentro del try por candidato. Basta con que la lectura
   * del cursor de un propietario falle —una fila corrupta, un fallo de almacenamiento— para que el
   * ciclo entero se caiga y los propietarios siguientes se queden sin ejecutar.
   *
   * <p>Aquí no hay evento que nombrar, y por eso el oráculo exige que la bitácora lo diga con
   * eventId nulo en lugar de callarse: el propietario y la causa siguen siendo obligatorios.
   */
  @Test
  void s21_anOwnerWhoseCursorCannotBeReadDoesNotTakeTheOtherAccountsDownWithHim() {
    var unreadable = "b";
    work.owners.add(unreadable);
    work.unreadable.add(unreadable);
    rules.create(unreadable, ruleWith(true, "Revisar de nuevo"));
    givenARuleThatCreatesTasks();
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E2, T0.plusSeconds(2)));

    execute.runCycle();

    assertThat(work.runs())
        .extracting(AutomationRun::eventId, AutomationRun::status)
        .as("un propietario ilegible no cancela el ciclo de los que van detrás")
        .containsExactly(tuple(E2, "succeeded"));
    assertThat(logged())
        .as("propietario y causa; el evento no existe todavía y se dice que no existe")
        .contains(unreadable, "eventId=null", "StorageUnavailableException");
  }

  /**
   * @s21, filas 4 y 5, en la ventana que el ejecutor no cubría: entre {@code
   *     endpoints.isActiveEndpointOf} —que se consulta FUERA de la transacción— y el INSERT de la
   *     entrega, el endpoint puede dejar de ser un endpoint activo de este propietario. No hace
   *     falta un atacante: el worker de la feature 25 desactiva endpoints solo, en el mismo
   *     proceso.
   *     <p>Hoy el adaptador contesta a eso con {@link AutomationClaimedException} y el ejecutor la
   *     lee como «otro worker se me adelantó»: devuelve true sin llamar a {@code record()}, con la
   *     transacción ya revertida. Se pierden la ejecución de la regla de webhook, la de CUALQUIER
   *     otra regla que casara el mismo evento —aquí, la tarea de R2— y hasta la línea de bitácora;
   *     y como el cursor del evento siguiente se escribe por posición absoluta, el evento saltado
   *     no se vuelve a leer jamás. Pérdida permanente y en silencio.
   *     <p>El contrato pide para esa regla exactamente lo mismo que para un endpoint borrado antes
   *     del ciclo: attempt 1, status failed, errorCode ENDPOINT_NOT_FOUND. Por eso el oráculo mira
   *     las dos reglas, el cursor y la bitácora: lo que se perdía no era sólo la fila del webhook.
   */
  @Test
  void s21_anEndpointGoneBetweenTheCheckAndTheWriteSettlesItsRuleAndSavesTheRest() {
    var notify = ruleFor(new NotifyWebhookAction(ENDPOINT));
    var sound = ruleWith(true, "Fijo de R2");
    work.owners.add(OWNER);
    rules.create(OWNER, notify);
    rules.create(OWNER, sound);
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(taskCreated(E1, T0.plusSeconds(1)));
    work.gone.add(ENDPOINT);
    var whileTheEndpointStillLooksActive =
        new ExecuteAutomations(
            work, rules, matcher, facts, (owner, endpoint) -> true, audit, clock);

    whileTheEndpointStillLooksActive.runCycle();

    assertThat(runOf(notify))
        .extracting(
            AutomationRun::attempt,
            AutomationRun::status,
            AutomationRun::errorCode,
            AutomationRun::createdTaskId,
            AutomationRun::deliveryId)
        .as("la regla que no llegó a su endpoint queda resuelta, no desaparecida")
        .containsExactly(1, "failed", "ENDPOINT_NOT_FOUND", null, null);
    assertThat(runOf(sound).status())
        .as("y la otra regla del mismo evento conserva su ejecución y su tarea")
        .isEqualTo("succeeded");
    assertThat(effectOf(sound)).isInstanceOf(AutomationEffect.CreateTask.class);
    assertThat(work.cursors.get(OWNER)).isEqualTo(new AutomationCursor(T0.plusSeconds(1), E1));
    assertThat(logged())
        .as("y queda línea de bitácora, que era lo primero que se perdía")
        .contains("ENDPOINT_NOT_FOUND");
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
    var rule = rules.list(OWNER).getFirst();
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
    // La bitácora del camino de fallo es la ÚNICA huella que sobrevive a la transacción
    // revertida junto con la fila `record`: sin ella, un almacenamiento caído deja al
    // propietario clavado en un evento sin una sola línea que diga cuál ni por qué. Sin esta
    // aserción, la línea 97 entera —el forEach y la llamada a log de dentro— se podía borrar
    // sin que cayera ninguna prueba.
    assertThat(logged())
        .as("the rolled back attempt is audited by identifiers, like the good one")
        .contains(rule.id().toString(), E1.toString(), "retry", "attempt=1")
        .contains("code=STORAGE_UNAVAILABLE");
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

  /**
   * El cruce de @s21 («sin detener las demás reglas») con @s22 (el reintento): un mismo evento con
   * ejecuciones previas de DOS reglas. Ninguna prueba del árbol le daba a un evento más de una
   * ejecución previa, así que el filtro que escoge la ejecución de ESTA regla —{@code run ->
   * rule.id().equals(run.ruleId())}, ExecuteAutomations:143— podía devolver siempre true y toda la
   * suite seguía verde.
   *
   * <p>Lo que ese mutante deja pasar: R1 quedó {@code failed} sobre E1 y R2 sigue en {@code retry}.
   * Sin el filtro, R2 hereda la ejecución de R1, la ve resuelta y se salta para siempre. Pérdida
   * permanente y silenciosa de una automatización del propietario, sin fila ni error.
   *
   * <p>La ejecución de R1 va primera en la lista a propósito: es la que {@code findFirst()}
   * devolvería si el filtro dejara de discriminar.
   */
  @Test
  void s22_eachRuleRetriesOnItsOwnPreviousRunAndNeverOnItsSiblings() {
    work.owners.add(OWNER);
    var settled = ruleWith(true, "Fijo de R1");
    var retrying = ruleWith(true, "Fijo de R2");
    rules.create(OWNER, settled);
    rules.create(OWNER, retrying);
    work.cursors.put(OWNER, new AutomationCursor(T0, E0));
    work.outbox.add(
        withRuns(
            E1,
            T0.plusSeconds(1),
            List.of(runOn(settled, RUN, 3, "failed"), runOn(retrying, SIBLING_RUN, 1, "retry"))));

    execute.runCycle();

    assertThat(runOf(retrying))
        .as("R2 reintenta sobre SU fila: misma identidad, un intento más")
        .extracting(AutomationRun::id, AutomationRun::attempt, AutomationRun::status)
        .containsExactly(SIBLING_RUN, 2, "succeeded");
    assertThat(work.commits.stream().flatMap(commit -> commit.outcomes().stream()))
        .as("y R1, ya resuelta, no se vuelve a intentar")
        .extracting(outcome -> outcome.run().ruleId())
        .containsExactly(retrying.id());
  }

  private static AutomationRun runOn(AutomationRule rule, UUID runId, int attempt, String status) {
    return new AutomationRun(
        runId,
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

  private static AutomationCandidate withRuns(
      UUID eventId, Instant occurredAt, List<AutomationRun> runs) {
    return new AutomationCandidate(taskCreated(eventId, occurredAt).event(), false, runs);
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
    final Set<String> unreadable = new HashSet<>();
    java.util.function.Predicate<AutomationCommit> failing = commit -> false;
    final Set<UUID> gone = new HashSet<>();

    @Override
    public List<String> ownersWithRules() {
      return List.copyOf(owners);
    }

    @Override
    public Optional<AutomationCursor> cursor(String owner) {
      if (unreadable.contains(owner))
        throw new StorageUnavailableException(new IllegalStateException("induced"));
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
      // Como el adaptador: el INSERT ... SELECT de la entrega no casa ninguna fila porque el
      // endpoint ya no es un endpoint activo de este propietario. Nada de la confirmación queda.
      for (var outcome : commit.outcomes())
        if (outcome.effect() instanceof AutomationEffect.Notify notify
            && gone.contains(notify.endpointId()))
          throw new AutomationEndpointGoneException(notify.endpointId());
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
