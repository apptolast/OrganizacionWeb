package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.AutomationCandidate;
import com.apptolast.organization.application.AutomationClaimedException;
import com.apptolast.organization.application.AutomationCommit;
import com.apptolast.organization.application.AutomationEffect;
import com.apptolast.organization.application.AutomationEndpointGoneException;
import com.apptolast.organization.application.AutomationOutcome;
import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * El SQL de {@link PostgresAutomationWork} contra PostgreSQL de verdad. Existe porque la mitad de
 * la conducta de esta clase vive en literales de cadena —el desempate del recorrido, la guarda del
 * reintento, la bandera de bloqueado, la ventana de ejecuciones previas— y ningún doble en memoria
 * los ejecuta: {@code ExecuteAutomationsTest} corre contra un {@code FakeWork} que se ordena a sí
 * mismo y fabrica las banderas a mano, y {@code AutomationExecutionTest} nunca tiene más de una
 * fila candidata ni deja nunca una fila en {@code retry} antes de un ciclo.
 *
 * <p>Comparte contenedor con {@link AutomationPersistenceTest} a propósito: son de la misma familia
 * y no hacen falta dos.
 */
class AutomationWorkPersistenceTest {
  private static final Instant T0 = Instant.parse("2026-09-08T10:00:00Z");
  private static final String TRIGGER = "TaskCreated.v1";

  /**
   * Los eventIds E1 y E2 de @s17, fijos para que «menor» signifique lo mismo en PostgreSQL —que
   * ordena uuid como bytes sin signo— y en la lectura de la prueba.
   */
  private static final UUID SMALLER = UUID.fromString("11111111-1111-4111-8111-111111111111");

  private static final UUID GREATER = UUID.fromString("22222222-2222-4222-8222-222222222222");

  /** El instante del encolado, siempre posterior al de los eventos de esta clase. */
  private static final Instant NOW = Instant.parse("2026-09-09T08:30:00Z");

  private final PostgresAutomationWork work =
      new PostgresAutomationWork(
          AutomationPersistenceTest.Database.JDBC,
          AutomationPersistenceTest.Database.TRANSACTIONS,
          new ObjectMapper(),
          AutomationWorkPersistenceTest::noTaskHere,
          Clock.fixed(NOW, ZoneOffset.UTC));

  private static Task noTaskHere(
      String owner, UUID project, String title, String criterion, Integer minutes) {
    throw new AssertionError("Los efectos de esta clase son None: nadie debería crear tareas.");
  }

  /**
   * @s17, la penúltima fila del Given: «eventos posteriores E1 y E2 con el mismo occurred_at y
   *     event_id de E1 menor ... las ejecuciones registradas tienen executedAt no decreciente en el
   *     orden E1, E2, E4» (features/automations.feature:232-236). Quien produce ese orden es el
   *     {@code ORDER BY occurred_at, event_id} del adaptador, y la parte del desempate no tenía
   *     oráculo: la prueba que cubre esa línea del contrato corre contra un {@code FakeWork} que
   *     ordena en Java dentro del propio test, así que demuestra que el caso de uso respeta el
   *     orden que le den, no que el adaptador lo produzca.
   *     <p>Lo que está en juego no es la estética: el cursor sólo avanza hacia delante, así que si
   *     el desempate se cae, el hermano de identificador menor queda por detrás del cursor para
   *     siempre y no dispara ninguna regla jamás. Por eso la segunda mitad vuelve a pedir
   *     candidatos con el cursor ya movido: el oráculo del orden y el de la no pérdida son cosas
   *     distintas.
   *     <p>El mayor se inserta primero a propósito: sin desempate el orden que devuelve la tabla es
   *     el de escritura, y entonces la primera aserción cae.
   */
  @Test
  void s17_ofTwoEventsOfTheSameInstantTheSmallerEventIdGoesFirstAndTheOtherIsNotLost() {
    var owner = owner();
    var project = project(owner);
    var sameInstant = T0.plusSeconds(1);
    outbox(owner, project, GREATER, sameInstant, "pending");
    outbox(owner, project, SMALLER, sameInstant, "pending");

    var candidates = work.after(owner, start());

    assertThat(idsOf(candidates))
        .as("«E1 y E2 con el mismo occurred_at y event_id de E1 menor», y E1 va primero")
        .containsExactly(SMALLER, GREATER);

    var processed = candidates.getFirst().event();
    var reached = new AutomationCursor(processed.occurredAt(), processed.eventId());
    work.commit(new AutomationCommit(owner, reached, List.of()));

    assertThat(work.cursor(owner))
        .as("el cursor avanza hasta el que se procesó, no más allá")
        .hasValue(new AutomationCursor(sameInstant, SMALLER));
    assertThat(idsOf(work.after(owner, reached)))
        .as("el hermano queda por delante del cursor, no detrás: no se pierde")
        .containsExactly(GREATER);
  }

  /**
   * @s17, la otra mitad: «E3 posterior en estado blocked ... no existe ejecución para E3»
   *     (features/automations.feature:233 y 238). La bandera la produce {@code
   *     "blocked".equals(row.getString("status"))} y ninguna prueba la hacía pasar por este
   *     adaptador: se fabricaba a mano en el doble. Si el literal deja de coincidir, {@code
   *     blocked()} es siempre false y las reglas se ejecutan sobre eventos que la outbox retuvo a
   *     propósito: tareas y webhooks a partir de lo que el sistema decidió no publicar.
   *     <p>Se afirman los dos lados —el pendiente false y el bloqueado true— porque un literal
   *     invertido («pending» en lugar de «blocked») sólo se distingue mirando los dos.
   */
  @Test
  void s17_theBlockedFlagOfEachOutboxRowReachesTheWorker() {
    var owner = owner();
    var project = project(owner);
    var open = outbox(owner, project, T0.plusSeconds(1), "pending");
    var held = outbox(owner, project, T0.plusSeconds(2), "blocked");

    var candidates = work.after(owner, start());

    assertThat(idsOf(candidates)).containsExactly(open, held);
    assertThat(candidates.getFirst().blocked())
        .as("una fila pending no está retenida y sus reglas deben dispararse")
        .isFalse();
    assertThat(candidates.getLast().blocked())
        .as("una fila blocked está retenida: se salta, no dispara nada")
        .isTrue();
  }

  /**
   * @s22: «una ejecución retry attempt 2 para el evento E1 ... la ejecución de E1 tiene attempt 3,
   *     status failed» (features/automations.feature:290-294). La escalera entera la sostiene el
   *     {@code AND status = 'retry'} del UPDATE, y sólo se probaba con el doble en memoria, que no
   *     ejecuta ese UPDATE: ninguna prueba de integración dejaba una fila en {@code retry} antes de
   *     un ciclo.
   *     <p>Si el literal cambia, el UPDATE no afecta a ninguna fila, {@code claim} lanza {@link
   *     AutomationClaimedException}, el caso de uso lo lee como «otro worker ganó» y la ejecución
   *     se queda clavada en su intento para siempre, sin error visible. Si el literal desaparece,
   *     pasa lo contrario y una fila ya resuelta se puede reescribir: por eso la tercera parte pide
   *     renovar una {@code failed} y exige que se rechace.
   */
  @Test
  void s22_onlyARowStillInRetryCanBeRenewedByALaterAttempt() {
    var owner = owner();
    var project = project(owner);
    var event = outbox(owner, project, T0.plusSeconds(1), "pending");
    var rule = rule(owner);
    given(rule, owner, event, 1, "retry", "STORAGE_UNAVAILABLE", T0.plusSeconds(2));

    work.commit(commitOf(owner, run(rule, owner, event, 2, "retry", "STORAGE_UNAVAILABLE", T0)));

    assertThat(rowOf(rule, event))
        .as("el segundo intento renueva su propia fila")
        .containsEntry("attempt", 2)
        .containsEntry("status", "retry")
        .containsEntry("executed_at", Timestamp.from(T0));

    work.commit(
        commitOf(
            owner, run(rule, owner, event, 3, "failed", "STORAGE_UNAVAILABLE", T0.plusSeconds(9))));

    assertThat(rowOf(rule, event))
        .as("y el tercero la cierra")
        .containsEntry("attempt", 3)
        .containsEntry("status", "failed");

    assertThatThrownBy(
            () ->
                work.commit(
                    commitOf(
                        owner, run(rule, owner, event, 3, "succeeded", null, T0.plusSeconds(10)))))
        .as("una fila ya resuelta no la reescribe nadie")
        .isInstanceOf(AutomationClaimedException.class);
    assertThat(rowOf(rule, event)).containsEntry("status", "failed");
  }

  /**
   * @s22, la pieza de la que sale el número de intento: el ejecutor decide si reintenta y con qué
   *     attempt leyendo las ejecuciones previas de cada candidato, y eso lo hace la consulta de
   *     {@code withTheirRuns} con su {@code event_id IN (ventana)} y su {@code owner_id = ?}.
   *     Ninguna prueba de integración dejaba de forma determinista una fila en {@code
   *     automation_runs} antes de que un ciclo la leyera; la idempotencia sólo se probaba con el
   *     doble.
   *     <p>Si la consulta devolviera vacío —columna mal escrita, IN mal armado, filtro de owner de
   *     más— el intento se recalcularía siempre como 1, la escalera se colapsaría y el {@code ON
   *     CONFLICT DO NOTHING} enmascararía el destrozo. Y si el filtro de propietario se cayera, un
   *     candidato arrastraría ejecuciones de otra cuenta sobre su mismo evento. Aquí hay una de
   *     cada: dos ejecuciones propias del mismo evento, una ajena sobre ese evento y un evento sin
   *     ninguna.
   */
  @Test
  void s22_eachCandidateCarriesItsOwnPreviousRunsAndNobodyElses() {
    var owner = owner();
    var stranger = owner();
    var project = project(owner);
    var first = outbox(owner, project, T0.plusSeconds(1), "pending");
    var second = outbox(owner, project, T0.plusSeconds(2), "pending");
    var task = UUID.randomUUID();
    var retrying = rule(owner);
    var settled = rule(owner);
    var mine = given(retrying, owner, first, 2, "retry", "STORAGE_UNAVAILABLE", T0.plusSeconds(3));
    var sibling = given(settled, owner, first, 1, "succeeded", null, T0.plusSeconds(4), task);
    given(rule(stranger), stranger, first, 1, "succeeded", null, T0.plusSeconds(5));

    var candidates = work.after(owner, start());

    assertThat(idsOf(candidates)).containsExactly(first, second);
    assertThat(candidates.getFirst().runs())
        .as("las ejecuciones del evento, todas las suyas y sólo las suyas")
        .containsExactlyInAnyOrder(
            new AutomationRun(
                mine,
                retrying,
                owner,
                first,
                TRIGGER,
                T0.plusSeconds(1),
                2,
                "retry",
                null,
                null,
                "STORAGE_UNAVAILABLE",
                T0.plusSeconds(3)),
            new AutomationRun(
                sibling,
                settled,
                owner,
                first,
                TRIGGER,
                T0.plusSeconds(1),
                1,
                "succeeded",
                task,
                null,
                null,
                T0.plusSeconds(4)));
    assertThat(candidates.getLast().runs())
        .as("un evento sin ejecuciones previas llega limpio: su primer intento será el 1")
        .isEmpty();
  }

  /**
   * @s21, filas 4 y 5 del Outline, y el invariante «existe exactamente una ejecución» de @s19: el
   *     ejecutor comprueba que el endpoint está activo FUERA de la transacción, y entre esa
   *     comprobación y el INSERT de la entrega el endpoint puede dejar de serlo —el worker de la
   *     feature 25 desactiva endpoints solo, en el mismo proceso—. Aquí eso se reproduce sin
   *     carrera ninguna: se confirma una Notify hacia un endpoint que ya no está activo.
   *     <p>El INSERT no lleva ON CONFLICT y el id de la entrega es un UUID recién sorteado, así que
   *     {@code affected == 0} NO puede significar nunca que otro worker se adelantara: sólo puede
   *     significar que el SELECT no casó. Confundir las dos cosas es lo que hace que el ejecutor
   *     conteste «ya lo hizo otro», siga el paseo, y el evento entero —con las reglas que también
   *     lo casaban— se pierda para siempre detrás del cursor, sin fila de ejecución y sin bitácora.
   *     <p>Las dos últimas aserciones son las que dan sentido a la primera: si algo hubiera quedado
   *     escrito, la distinción sería otro problema.
   */
  @Test
  void s21_anEndpointNoLongerActiveIsNotAClaimAnotherWorkerWon() {
    var owner = owner();
    var project = project(owner);
    var event = outbox(owner, project, T0.plusSeconds(1), "pending");
    var endpoint = endpoint(owner, "disabled");
    var rule = rule(owner);

    assertThatThrownBy(() -> work.commit(notifying(owner, rule, endpoint, event)))
        .as("«el endpoint ya no está activo» y «otro worker se me adelantó» no son la misma cosa")
        .isInstanceOf(AutomationEndpointGoneException.class);

    assertThat(deliveriesOf(owner)).as("y no se encoló ninguna entrega").isZero();
    assertThat(runsOf(owner)).as("ni quedó fila de ejecución: la transacción revirtió").isZero();
  }

  /**
   * M9: el aislamiento por propietario de {@code queue()} vive en dos literales de SQL —{@code
   * e.owner_id = ?} y {@code e.status = 'active'}— que ninguna prueba distinguía, porque el único
   * test que llegaba a este INSERT usaba un endpoint activo del propio propietario y PIT no muta
   * cadenas. Borrar {@code AND e.owner_id = ?} dejaba la suite verde y entregaba el payload de A a
   * la cola de B: el {@code INSERT ... SELECT} toma {@code e.owner_id} y la URL del endpoint, y
   * {@code o.payload} del evento de A.
   *
   * <p>Por eso el segundo propietario tiene su endpoint ACTIVO: con uno inactivo el predicado de
   * estado tapa al de propietario y la prueba volvería a no discriminar.
   */
  @Test
  void queueNeverHandsTheEventOfOneOwnerToTheEndpointOfAnother() {
    var mine = owner();
    var stranger = owner();
    var project = project(mine);
    var event = outbox(mine, project, T0.plusSeconds(1), "pending");
    var theirs = endpoint(stranger, "active");
    var rule = rule(mine);

    assertThatThrownBy(() -> work.commit(notifying(mine, rule, theirs, event)))
        .as("un endpoint ajeno no es alcanzable ni siquiera nombrándolo por su id")
        .isInstanceOf(AutomationEndpointGoneException.class);

    assertThat(deliveriesOf(stranger))
        .as("nada del propietario A llega a la cola de entregas de B")
        .isZero();
    assertThat(deliveriesOf(mine)).isZero();
  }

  /**
   * M11: {@code queue()} sella {@code next_attempt_at}, {@code created_at} y {@code updated_at} de
   * la entrega con {@code notify.event().occurredAt()}, el instante del EVENTO. En el propio @s15
   * —el worker apagado que se enciende y camina el atraso— toda entrega de automatización nace con
   * las tres marcas en el pasado: el {@code createdAt} que el propietario ve en el registro de la
   * feature 25 (@s29) deja de ser cuando se creó la entrega, y el índice {@code
   * webhook_deliveries_due (next_attempt_at, id) WHERE status='pending'} las coloca por delante de
   * todas las entregas legítimas. El {@code enqueue} de la propia feature 25 sella con {@code now}:
   * este adaptador era el único que no.
   *
   * <p>El oráculo de @s26 afirma endpoint_id, event_id, event_type, status y body y ninguna de las
   * tres marcas, así que hasta aquí nada podía notarlo, ni en un sentido ni en el otro.
   */
  @Test
  void s26_aQueuedDeliveryIsStampedWhenItIsQueuedAndNotWhenTheEventHappened() {
    var owner = owner();
    var project = project(owner);
    var event = outbox(owner, project, T0.plusSeconds(1), "pending");
    var endpoint = endpoint(owner, "active");

    work.commit(notifying(owner, rule(owner), endpoint, event));

    assertThat(deliveryOf(owner))
        .as("las tres marcas son las del encolado, no las del evento que lo provocó")
        .containsEntry("created_at", Timestamp.from(NOW))
        .containsEntry("updated_at", Timestamp.from(NOW))
        .containsEntry("next_attempt_at", Timestamp.from(NOW));
  }

  private static Map<String, Object> deliveryOf(String owner) {
    return AutomationPersistenceTest.Database.JDBC.queryForMap(
        "SELECT * FROM webhook_deliveries WHERE owner_id = ?", owner);
  }

  private AutomationCommit notifying(String owner, UUID rule, UUID endpoint, UUID event) {
    var effect =
        new AutomationEffect.Notify(
            endpoint,
            new AutomationEvent(
                event, owner, TRIGGER, UUID.randomUUID(), T0.plusSeconds(1), Map.of()));
    return new AutomationCommit(
        owner,
        null,
        List.of(
            new AutomationOutcome(
                run(rule, owner, event, 1, "succeeded", null, T0.plusSeconds(2)), effect)));
  }

  /**
   * Un endpoint del propietario en el estado que pida la prueba. El secreto es un byte de relleno,
   * y la razón y el instante de desactivación van juntos porque el CHECK de la tabla los exige.
   */
  private static UUID endpoint(String owner, String status) {
    var disabled = "disabled".equals(status);
    var id = UUID.randomUUID();
    AutomationPersistenceTest.Database.JDBC.update(
        "INSERT INTO webhook_endpoints(id,owner_id,url,description,event_types,status,"
            + "disabled_reason,disabled_at,"
            + "secret_ciphertext,cursor_occurred_at,cursor_event_id,created_at,updated_at)"
            + " VALUES (?,?,'https://example.com/h','',ARRAY['TaskCreated.v1']::text[],?,?,?,"
            + "'\\x01'::bytea,?,?,?,?)",
        id,
        owner,
        status,
        disabled ? "MANUAL" : null,
        disabled ? Timestamp.from(T0) : null,
        Timestamp.from(T0),
        new UUID(0L, 0L),
        Timestamp.from(T0),
        Timestamp.from(T0));
    return id;
  }

  private static long deliveriesOf(String owner) {
    return AutomationPersistenceTest.Database.JDBC.queryForObject(
        "SELECT count(*) FROM webhook_deliveries WHERE owner_id = ?", Long.class, owner);
  }

  private static long runsOf(String owner) {
    return AutomationPersistenceTest.Database.JDBC.queryForObject(
        "SELECT count(*) FROM automation_runs WHERE owner_id = ?", Long.class, owner);
  }

  /**
   * M9: el aislamiento del recorrido. La ventana de candidatos es {@code WHERE owner_id = ? AND
   * (occurred_at, event_id) > (?, ?)}, y hasta aquí ninguna prueba ponía dos propietarios con
   * eventos en la misma base: cada test se inventaba un propietario único, así que el predicado de
   * propietario no tenía nada contra lo que discriminar y PIT no muta cadenas.
   *
   * <p>Sin él, el ciclo de A leería los eventos de B, sus reglas se dispararían sobre ellos y le
   * crearía tareas —o le encolaría entregas— a partir de datos de otra cuenta. El evento del
   * extraño ocurre ANTES que el propio a propósito: sin el predicado sería el primero de la lista,
   * y el ciclo de A lo procesaría antes que nada suyo.
   */
  @Test
  void theWalkOfOneOwnerNeverReadsTheOutboxOfAnother() {
    var mine = owner();
    var stranger = owner();
    outbox(stranger, project(stranger), T0.plusSeconds(1), "pending");
    var own = outbox(mine, project(mine), T0.plusSeconds(2), "pending");

    assertThat(idsOf(work.after(mine, start())))
        .as("la ventana de candidatos es la de un propietario, no la de la instalación")
        .containsExactly(own);
  }

  private static AutomationCursor start() {
    return new AutomationCursor(T0, AutomationCursor.START);
  }

  private static List<UUID> idsOf(List<AutomationCandidate> candidates) {
    return candidates.stream().map(candidate -> candidate.event().eventId()).toList();
  }

  private static AutomationCommit commitOf(String owner, AutomationRun run) {
    return new AutomationCommit(
        owner, null, List.of(new AutomationOutcome(run, new AutomationEffect.None())));
  }

  private static AutomationRun run(
      UUID rule,
      String owner,
      UUID event,
      int attempt,
      String status,
      String errorCode,
      Instant executedAt) {
    return new AutomationRun(
        UUID.randomUUID(),
        rule,
        owner,
        event,
        TRIGGER,
        T0.plusSeconds(1),
        attempt,
        status,
        null,
        null,
        errorCode,
        executedAt);
  }

  private static String owner() {
    return "work-" + UUID.randomUUID();
  }

  private static UUID project(String owner) {
    var id = UUID.randomUUID();
    AutomationPersistenceTest.Database.JDBC.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at)"
            + " VALUES (?,?, 'Marketing', '', 'idea', ?, ?)",
        id,
        owner,
        Timestamp.from(T0),
        Timestamp.from(T0));
    return id;
  }

  private static UUID rule(String owner) {
    var id = UUID.randomUUID();
    AutomationPersistenceTest.Database.JDBC.update(
        "INSERT INTO automation_rules(id,owner_id,name,enabled,event_type,condition_project_id,"
            + "action,version,created_at,updated_at)"
            + " VALUES (?,?, 'Regla', true, ?, NULL, '{}'::jsonb, 1, ?, ?)",
        id,
        owner,
        TRIGGER,
        Timestamp.from(T0),
        Timestamp.from(T0));
    return id;
  }

  private static UUID outbox(String owner, UUID project, Instant occurredAt, String status) {
    return outbox(owner, project, UUID.randomUUID(), occurredAt, status);
  }

  private static UUID outbox(
      String owner, UUID project, UUID eventId, Instant occurredAt, String status) {
    var payload =
        ("{\"eventId\":\"%s\",\"aggregateId\":\"%s\",\"ownerId\":\"%s\",\"occurredAt\":\"%s\","
                + "\"schemaVersion\":1,\"type\":\"%s\",\"taskId\":\"%s\"}")
            .formatted(eventId, project, owner, occurredAt, TRIGGER, UUID.randomUUID());
    AutomationPersistenceTest.Database.JDBC.update(
        "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,"
            + "occurred_at,payload,status) VALUES (?,?,?,?,1,?,?::jsonb,?)",
        eventId,
        project,
        owner,
        TRIGGER,
        Timestamp.from(occurredAt),
        payload,
        status);
    return eventId;
  }

  private static UUID given(
      UUID rule,
      String owner,
      UUID event,
      int attempt,
      String status,
      String errorCode,
      Instant executedAt) {
    return given(rule, owner, event, attempt, status, errorCode, executedAt, null);
  }

  private static UUID given(
      UUID rule,
      String owner,
      UUID event,
      int attempt,
      String status,
      String errorCode,
      Instant executedAt,
      UUID createdTaskId) {
    var id = UUID.randomUUID();
    AutomationPersistenceTest.Database.JDBC.update(
        "INSERT INTO automation_runs(id,rule_id,owner_id,event_id,event_type,occurred_at,attempt,"
            + "status,created_task_id,delivery_id,error_code,executed_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,NULL,?,?)",
        id,
        rule,
        owner,
        event,
        TRIGGER,
        Timestamp.from(T0.plusSeconds(1)),
        attempt,
        status,
        createdTaskId,
        errorCode,
        Timestamp.from(executedAt));
    return id;
  }

  private static Map<String, Object> rowOf(UUID rule, UUID event) {
    return AutomationPersistenceTest.Database.JDBC.queryForMap(
        "SELECT * FROM automation_runs WHERE rule_id = ? AND event_id = ?", rule, event);
  }
}
