package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookCursor;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookOutboxPersistenceTest {
  private static final Instant T = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final UUID NIL = new UUID(0L, 0L);

  /** Los eventIds B y A de la última fila de @s19, fijos para que «menor» no dependa del azar. */
  private static final UUID SMALLER = UUID.fromString("11111111-1111-4111-8111-111111111111");

  private static final UUID GREATER = UUID.fromString("22222222-2222-4222-8222-222222222222");

  private static PostgresWebhookStore store() {
    return new PostgresWebhookStore(
        WebhookPersistenceTest.Database.JDBC, WebhookPersistenceTest.Database.TRANSACTIONS);
  }

  private static PostgresWebhookOutbox outbox() {
    return new PostgresWebhookOutbox(
        WebhookPersistenceTest.Database.JDBC,
        WebhookPersistenceTest.Database.TRANSACTIONS,
        new ObjectMapper());
  }

  private static WebhookEndpoint given(String owner, List<String> types) {
    var endpoint =
        new WebhookEndpoint(
            UUID.randomUUID(), "https://example.com/h", "", types, "active", null, null, T, T);
    store().insert(owner, endpoint, "cipher:whsec_x".getBytes(StandardCharsets.UTF_8));
    return endpoint;
  }

  /** A project row is needed because outbox_events.aggregate_id references projects(id). */
  private static UUID givenProject(String owner) {
    var id = UUID.randomUUID();
    WebhookPersistenceTest.Database.JDBC.update(
        """
        INSERT INTO projects (id, owner_id, name, description, status, version, created_at, updated_at)
        VALUES (?,?,?,?,?,?,?,?)
        """,
        id,
        owner,
        "Proyecto",
        "",
        "active",
        1L,
        java.sql.Timestamp.from(T),
        java.sql.Timestamp.from(T));
    return id;
  }

  private static UUID givenEvent(
      String owner, UUID projectId, String type, Instant occurredAt, String status, int version) {
    return givenEventWithId(
        owner, projectId, UUID.randomUUID(), type, occurredAt, status, version);
  }

  private static UUID givenEventWithId(
      String owner,
      UUID projectId,
      UUID eventId,
      String type,
      Instant occurredAt,
      String status,
      int version) {
    var payload =
        "{\"eventId\":\""
            + eventId
            + "\",\"aggregateId\":\""
            + projectId
            + "\",\"ownerId\":\""
            + owner
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":"
            + version
            + ",\"type\":\""
            + type
            + "\",\"name\":\"Proyecto\"}";
    WebhookPersistenceTest.Database.JDBC.update(
        """
        INSERT INTO outbox_events (
          event_id, aggregate_id, owner_id, event_type, schema_version, occurred_at, payload, status)
        VALUES (?,?,?,?,?,?,?::jsonb,?)
        """,
        eventId,
        projectId,
        owner,
        type,
        version,
        java.sql.Timestamp.from(occurredAt),
        payload,
        status);
    return eventId;
  }

  @Test
  void s18_onlyTheOwnersRowsAfterTheCursorAndBeforeTheHorizonAreCandidates() {
    var owner = "outbox-" + UUID.randomUUID();
    var stranger = "other-" + UUID.randomUUID();
    var project = givenProject(owner);
    var strangerProject = givenProject(stranger);
    var wanted = givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(1), "pending", 1);
    givenEvent(stranger, strangerProject, "ProjectCreated.v1", T.plusSeconds(1), "pending", 1);
    givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(600), "pending", 1);

    var candidates =
        outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60)).stream()
            .map(candidate -> candidate.eventId())
            .toList();

    assertEquals(List.of(wanted), candidates);
  }

  @Test
  void s18_theCandidateCarriesTheRowPayloadStatusAndVersionUntouched() {
    var owner = "payload-" + UUID.randomUUID();
    var project = givenProject(owner);
    var eventId = givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(1), "blocked", 1);

    var candidate = outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60)).getFirst();

    assertEquals(eventId, candidate.eventId());
    assertEquals("ProjectCreated.v1", candidate.eventType());
    assertEquals("blocked", candidate.status());
    assertTrue(candidate.isBlocked());
    assertEquals(1, candidate.schemaVersion());
    assertTrue(candidate.payloadValid());
    // jsonb is a normalised representation, so "the payload of the row" is what jsonb renders:
    // the delivery body must equal exactly this string, which the enqueue test asserts.
    assertTrue(candidate.payload().replace(" ", "").contains("\"name\":\"Proyecto\""));
  }

  @Test
  void s21_anUnsupportedVersionAndABrokenPayloadAreReportedAsInvalidCandidates() {
    var owner = "invalid-" + UUID.randomUUID();
    var project = givenProject(owner);
    givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(1), "pending", 2);

    var unsupported =
        outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60)).getFirst();
    assertFalse(unsupported.isSupported());

    var broken = UUID.randomUUID();
    WebhookPersistenceTest.Database.JDBC.update(
        """
        INSERT INTO outbox_events (
          event_id, aggregate_id, owner_id, event_type, schema_version, occurred_at, payload, status)
        VALUES (?,?,?,?,?,?,?::jsonb,?)
        """,
        broken,
        project,
        owner,
        "ProjectCreated.v1",
        1,
        java.sql.Timestamp.from(T.plusSeconds(2)),
        "{\"eventId\":\"" + broken + "\"}",
        "pending");

    var candidates = outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60));
    var brokenCandidate =
        candidates.stream().filter(c -> c.eventId().equals(broken)).findFirst().orElseThrow();
    assertTrue(brokenCandidate.isSupported());
    assertFalse(brokenCandidate.payloadValid(), "a payload without name is not deliverable");
  }

  @Test
  void s18_enqueueingWritesTheDeliveryAndMovesTheCursorWithoutTouchingTheOutbox() {
    var owner = "enqueue-" + UUID.randomUUID();
    var project = givenProject(owner);
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    var eventId = givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(1), "pending", 1);
    var candidate = outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60)).getFirst();

    outbox().enqueue(endpoint.id(), candidate, UUID.randomUUID(), T.plusSeconds(60));

    var log = store().list(owner, endpoint.id());
    assertEquals(1, log.size());
    assertEquals(eventId, log.getFirst().eventId());
    assertEquals("ProjectCreated.v1", log.getFirst().eventType());
    assertEquals(WebhookDelivery.PENDING, log.getFirst().status());
    assertEquals(0, log.getFirst().attempt());
    assertEquals(
        candidate.payload(),
        WebhookPersistenceTest.Database.JDBC.queryForObject(
            "SELECT body FROM webhook_deliveries WHERE endpoint_id=?", String.class, endpoint.id()),
        "the body is the outbox payload byte for byte");
    assertEquals(new WebhookCursor(T.plusSeconds(1), eventId), cursorOf(endpoint.id()));

    var row =
        WebhookPersistenceTest.Database.JDBC.queryForMap(
            "SELECT status, attempts, published_at FROM outbox_events WHERE event_id=?", eventId);
    assertEquals(Map.of("status", "pending", "attempts", 0L), rowWithoutNulls(row));
  }

  /**
   * @s19, última fila del Examples: «dos eventos con el mismo occurredAt y eventIds B y A → se
   *     encola primero el de eventId menor A». Quien produce ese orden es el {@code ORDER BY
   *     occurred_at, event_id} del adaptador, y la parte del desempate no tenía oráculo: la prueba
   *     que cubría esa línea, {@code EnqueueWebhookDeliveriesTest}, corre contra un {@code
   *     FakeOutbox} <b>que se ordena a sí mismo</b>, así que demuestra que el caso de uso respeta
   *     el orden que le den, no que el adaptador lo produzca.
   *     <p>Lo que está en juego no es la estética del orden: {@code enqueueFirstEligible} adelanta
   *     el cursor al candidato que encola, y {@code WebhookCursor.precedes} excluye para siempre lo
   *     que quede por detrás. Si el desempate se cae, el hermano de identificador menor se pierde
   *     en silencio y de forma permanente. Por eso la segunda mitad de la prueba vuelve a pedir
   *     candidatos con el cursor ya movido: el oráculo del orden y el de la no pérdida son cosas
   *     distintas.
   *     <p>El mayor se inserta primero a propósito: sin desempate, el orden que devuelve la tabla
   *     es el de escritura, y entonces la primera aserción cae. Con dos identificadores fijos,
   *     además, «menor» significa lo mismo en PostgreSQL y en Java.
   */
  @Test
  void s19_ofTwoEventsOfTheSameInstantTheSmallerEventIdGoesFirstAndTheOtherIsNotLost() {
    var owner = "tie-" + UUID.randomUUID();
    var project = givenProject(owner);
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    var sameInstant = T.plusSeconds(1);
    givenEventWithId(owner, project, GREATER, "ProjectCreated.v1", sameInstant, "pending", 1);
    givenEventWithId(owner, project, SMALLER, "ProjectCreated.v1", sameInstant, "pending", 1);

    var candidates = outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60));

    assertEquals(
        List.of(SMALLER, GREATER),
        candidates.stream().map(candidate -> candidate.eventId()).toList(),
        "«se encola primero el de eventId menor A» (features/webhooks.feature:258)");

    outbox().enqueue(endpoint.id(), candidates.getFirst(), UUID.randomUUID(), sameInstant);

    assertEquals(
        new WebhookCursor(sameInstant, SMALLER),
        cursorOf(endpoint.id()),
        "el cursor avanza hasta el que se encoló, no más allá");
    assertEquals(
        List.of(GREATER),
        outbox().after(owner, cursorOf(endpoint.id()), T.plusSeconds(60)).stream()
            .map(candidate -> candidate.eventId())
            .toList(),
        "el hermano queda por delante del cursor, no detrás: no se pierde");
  }

  @Test
  void s21_skippingMovesTheCursorWithoutWritingAnyDelivery() {
    var owner = "skip-" + UUID.randomUUID();
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    var reached = new WebhookCursor(T.plusSeconds(5), UUID.randomUUID());

    outbox().skip(endpoint.id(), reached, "INVALID_EVENT");

    assertEquals(reached, cursorOf(endpoint.id()));
    assertTrue(store().list(owner, endpoint.id()).isEmpty());
  }

  @Test
  void s20_anEndpointWithAnOutboxDeliveryInFlightIsNotReadyButAPingDoesNotBlockIt() {
    var owner = "ready-" + UUID.randomUUID();
    var project = givenProject(owner);
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    assertTrue(isReady(endpoint.id()), "a fresh endpoint is ready");

    store().enqueuePing(owner, endpoint.id(), WebhookDelivery.ping(UUID.randomUUID(), T), "{}");
    assertTrue(isReady(endpoint.id()), "a pending ping never blocks the outbox walk");

    givenEvent(owner, project, "ProjectCreated.v1", T.plusSeconds(1), "pending", 1);
    var candidate = outbox().after(owner, new WebhookCursor(T, NIL), T.plusSeconds(60)).getFirst();
    outbox().enqueue(endpoint.id(), candidate, UUID.randomUUID(), T.plusSeconds(60));

    assertFalse(isReady(endpoint.id()), "one outbox delivery in flight is the limit");
  }

  private static boolean isReady(UUID endpointId) {
    return outbox().readyEndpoints().stream()
        .anyMatch(ready -> ready.endpoint().id().equals(endpointId));
  }

  private static WebhookCursor cursorOf(UUID endpointId) {
    var row =
        WebhookPersistenceTest.Database.JDBC.queryForMap(
            "SELECT cursor_occurred_at, cursor_event_id FROM webhook_endpoints WHERE id=?",
            endpointId);
    return new WebhookCursor(
        ((java.sql.Timestamp) row.get("cursor_occurred_at")).toInstant(),
        (UUID) row.get("cursor_event_id"));
  }

  private static Map<String, Object> rowWithoutNulls(Map<String, Object> row) {
    var copy = new java.util.HashMap<>(row);
    copy.values().removeIf(java.util.Objects::isNull);
    return copy;
  }
}
