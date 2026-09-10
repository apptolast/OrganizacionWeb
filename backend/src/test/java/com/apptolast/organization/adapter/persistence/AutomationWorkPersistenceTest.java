package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.AutomationCandidate;
import com.apptolast.organization.application.AutomationClaimedException;
import com.apptolast.organization.application.AutomationCommit;
import com.apptolast.organization.application.AutomationEffect;
import com.apptolast.organization.application.AutomationOutcome;
import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationRun;
import com.apptolast.organization.domain.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
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

  private final PostgresAutomationWork work =
      new PostgresAutomationWork(
          AutomationPersistenceTest.Database.JDBC,
          AutomationPersistenceTest.Database.TRANSACTIONS,
          new ObjectMapper(),
          AutomationWorkPersistenceTest::noTaskHere);

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
