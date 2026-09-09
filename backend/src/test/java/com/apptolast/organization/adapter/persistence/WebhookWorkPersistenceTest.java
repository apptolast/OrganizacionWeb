package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookAttempt;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class WebhookWorkPersistenceTest {
  private static final Instant T = Instant.parse("2026-09-08T12:00:00.000000Z");
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";
  // Las diez entregas y el receptor que tarda en responder, ambos del Given de @s23.
  private static final int DELIVERIES = 10;
  private static final long RECEIVER_MILLIS = 120;

  private static PostgresWebhookStore store() {
    return new PostgresWebhookStore(
        WebhookPersistenceTest.Database.JDBC, WebhookPersistenceTest.Database.TRANSACTIONS);
  }

  private static PostgresWebhookWork work() {
    return new PostgresWebhookWork(
        WebhookPersistenceTest.Database.JDBC,
        WebhookPersistenceTest.Database.TRANSACTIONS,
        (owner, endpointId, ciphertext) ->
            new String(ciphertext, StandardCharsets.UTF_8).replace("cipher:", ""));
  }

  private static WebhookEndpoint endpointOf(String status) {
    return new WebhookEndpoint(
        UUID.randomUUID(),
        "https://example.com/h",
        "",
        List.of("TaskCreated.v1"),
        status,
        "disabled".equals(status) ? "MANUAL" : null,
        "disabled".equals(status) ? T : null,
        T,
        T);
  }

  private static WebhookEndpoint given(String owner, String status) {
    var endpoint = endpointOf(status);
    store().insert(owner, endpoint, ("cipher:" + SECRET).getBytes(StandardCharsets.UTF_8));
    if ("disabled".equals(status)) store().changeStatus(owner, endpoint.id(), "disabled", T);
    return endpoint;
  }

  private static WebhookDelivery enqueue(String owner, UUID endpointId, String body) {
    var delivery = WebhookDelivery.ping(UUID.randomUUID(), T);
    store().enqueuePing(owner, endpointId, delivery, body);
    return delivery;
  }

  @Test
  void s22_aClaimHandsOverTheBodyAndTheDecryptedSecretOfItsEndpoint() {
    var owner = "claim-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    var delivery = enqueue(owner, endpoint.id(), "{\"a\":1}");

    var claimed = claimOwn(work(), owner, T);

    assertEquals(delivery.id(), claimed.delivery().id());
    assertEquals("{\"a\":1}", claimed.body());
    assertEquals(SECRET, claimed.secret());
    assertEquals(endpoint.id(), claimed.endpoint().id());
    assertEquals(owner, claimed.ownerId());
    assertFalse(claimed.toString().contains(SECRET));
  }

  @Test
  void s22_aLeasedDeliveryIsNotClaimedAgainUntilItsLeaseExpires() {
    var owner = "lease-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();

    assertNotNull(claimOwn(work, owner, T));
    assertTrue(claimedFor(work, owner, T).isEmpty(), "the lease must hide it from the next cycle");

    // The worker died before recording: once the lease lapses the same delivery is retried.
    var recovered = claimedFor(work, owner, T.plus(Duration.ofMinutes(10)));
    assertEquals(1, recovered.size());
    assertEquals(0, recovered.getFirst().delivery().attempt());
  }

  /**
   * The worker claims across every owner by design, and this container is shared with the sibling
   * persistence test, so a test may only ever assert about the rows of its own owner.
   */
  private static List<com.apptolast.organization.application.ClaimedDelivery> claimedFor(
      PostgresWebhookWork work, String owner, Instant now) {
    var claimed = new ArrayList<com.apptolast.organization.application.ClaimedDelivery>();
    for (var attempt = 0; attempt < 200; attempt++) {
      var next = work.claimNext(now);
      if (next.isEmpty()) break;
      if (next.get().ownerId().equals(owner)) claimed.add(next.get());
    }
    return claimed;
  }

  /** Claims until the owner's own next delivery shows up, ignoring everyone else's. */
  private static com.apptolast.organization.application.ClaimedDelivery claimOwn(
      PostgresWebhookWork work, String owner, Instant now) {
    for (var attempt = 0; attempt < 200; attempt++) {
      var next = work.claimNext(now);
      if (next.isEmpty()) break;
      if (next.get().ownerId().equals(owner)) return next.get();
    }
    throw new AssertionError("no delivery of " + owner + " was claimable");
  }

  @Test
  void s22_aDisabledEndpointNeverYieldsItsPendingDeliveries() {
    var owner = "off-" + UUID.randomUUID();
    var endpoint = given(owner, "disabled");
    enqueue(owner, endpoint.id(), "{}");

    assertTrue(claimedFor(work(), owner, T).isEmpty());
  }

  /**
   * @s23 el Given nombra un receptor que tarda en responder 200, y el Then pide tres cosas: «el
   *     receptor recibe exactamente 10 peticiones, una por eventId», «ninguna instancia espera al
   *     bloqueo de fila de la otra» y «las 10 entregas quedan succeeded con attempt 1».
   *     <p>La versión anterior de esta prueba sólo reclamaba: sin receptor, sin {@code record} y
   *     sin ninguna aserción sobre la espera. Eso la dejaba ciega al único defecto que este
   *     escenario existe para cazar. Si en {@code PostgresWebhookWork} se cambia {@code FOR UPDATE
   *     OF d SKIP LOCKED} por un {@code FOR UPDATE} a secas, las diez filas comparten {@code
   *     next_attempt_at} y las dos instancias eligen la misma primera fila; la perdedora se
   *     bloquea, al desbloquearse reevalúa el predicado en READ COMMITTED, ya no lo cumple, recibe
   *     {@code Optional.empty} y abandona. La ganadora drena las diez sin contención y las dos
   *     aserciones de cardinalidad seguían saliendo verdes.
   *     <p>El oráculo del no bloqueo es <b>que las dos instancias reclamen algo</b>, no un tiempo
   *     de pared: con cinco carriles compitiendo por esta máquina, un umbral temporal daría falsos
   *     rojos, y una prueba que falla por la carga ajena deja de creerse. El retardo del receptor
   *     es lo que hace discriminante a esa aserción: sin él las dos instancias podrían turnarse sin
   *     solaparse nunca.
   */
  @Test
  void s23_twoWorkersNeverClaimTheSameDeliveryAndNeitherWaitsForTheOther() throws Exception {
    var owner = "race-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    for (var index = 0; index < DELIVERIES; index++) enqueue(owner, endpoint.id(), "{}");
    var sent = new java.util.concurrent.CopyOnWriteArrayList<UUID>();

    Callable<List<UUID>> worker = () -> deliverAll(owner, sent);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var results = pool.invokeAll(List.of(worker, worker));
      var first = results.get(0).get();
      var second = results.get(1).get();
      var all = new ArrayList<UUID>();
      all.addAll(first);
      all.addAll(second);

      assertEquals(DELIVERIES, all.size(), "each delivery is claimed exactly once");
      assertEquals(DELIVERIES, java.util.Set.copyOf(all).size(), "no delivery is claimed twice");
      // La cláusula «ninguna instancia espera al bloqueo de fila de la otra». Con FOR UPDATE
      // bloqueante la perdedora acaba con cero reclamaciones y esta aserción muere.
      assertTrue(
          !first.isEmpty() && !second.isEmpty(),
          "both workers made progress: " + first.size() + " and " + second.size());
      // «El receptor recibe exactamente 10 peticiones, una por eventId».
      assertEquals(DELIVERIES, sent.size(), "the receiver got one request per delivery");
      assertEquals(DELIVERIES, java.util.Set.copyOf(sent).size(), "one request per eventId");
      // «Las 10 entregas quedan succeeded con attempt 1».
      var log = store().list(owner, endpoint.id());
      assertEquals(DELIVERIES, log.size());
      assertTrue(
          log.stream().allMatch(delivery -> "succeeded".equals(delivery.status())),
          "every delivery settled as succeeded");
      assertTrue(
          log.stream().allMatch(delivery -> delivery.attempt() == 1),
          "every delivery settled on its first attempt");
    }
  }

  /**
   * @s23 «ninguna instancia espera al bloqueo de fila de la otra», con un oráculo que <b>sí</b>
   *     distingue.
   *     <p>Conviene dejar escrito lo que se intentó antes y por qué no valía, para que nadie lo
   *     repita: el oráculo natural parecía ser «las dos instancias reclaman algo», pero <b>no
   *     discrimina</b>. Sustituí a mano {@code SKIP LOCKED} por {@code FOR UPDATE} en producción y
   *     la prueba de arriba <b>siguió verde</b>: con diez filas libres, la instancia que pierde el
   *     bloqueo no se queda sin trabajo, sólo espera un instante y reevalúa quedándose con otra de
   *     las nueve. La espera existe, pero es invisible para cualquier aserción de cardinalidad. La
   *     teoría de que la perdedora acaba con cero reclamaciones no se sostiene cuando hay cola.
   *     <p>Lo que sí distingue es preguntar por la <b>identidad</b> de lo reclamado mientras otra
   *     transacción retiene la primera fila del orden de reclamación: con {@code SKIP LOCKED} la
   *     consulta la salta y devuelve la siguiente <b>sin esperar</b>; con {@code FOR UPDATE} se
   *     queda bloqueada hasta que el tenedor confirme, y entonces devuelve justo la que estaba
   *     retenida. Dos conductas incompatibles, sin cronómetro de por medio.
   */
  @Test
  void s23_aRowHeldByAnotherTransactionIsSkippedInsteadOfWaitedFor() throws Exception {
    var owner = "skip-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    enqueue(owner, endpoint.id(), "{}");
    var held =
        WebhookPersistenceTest.Database.JDBC.queryForObject(
            "SELECT id FROM webhook_deliveries WHERE endpoint_id=? ORDER BY next_attempt_at, id"
                + " LIMIT 1",
            UUID.class,
            endpoint.id());

    var holding = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var holder =
          pool.submit(
              () ->
                  new org.springframework.transaction.support.TransactionTemplate(
                          WebhookPersistenceTest.Database.TRANSACTIONS)
                      .execute(
                          status -> {
                            WebhookPersistenceTest.Database.JDBC.queryForObject(
                                "SELECT id FROM webhook_deliveries WHERE id=? FOR UPDATE",
                                UUID.class,
                                held);
                            holding.countDown();
                            try {
                              release.await(10, java.util.concurrent.TimeUnit.SECONDS);
                            } catch (InterruptedException interrupted) {
                              Thread.currentThread().interrupt();
                            }
                            return null;
                          }));
      assertTrue(
          holding.await(10, java.util.concurrent.TimeUnit.SECONDS), "the row was never held");

      var claim = pool.submit(() -> claimOwn(work(), owner, T));
      try {
        // Si la consulta esperase al bloqueo, aquí no habría respuesta: el tenedor sigue dentro de
        // su transacción y no la confirmará hasta el `release` de más abajo. El tiempo de espera es
        // el detector del bloqueo, no el oráculo: el oráculo es la identidad de la fila.
        var claimed = claim.get(10, java.util.concurrent.TimeUnit.SECONDS);
        assertNotEquals(
            held,
            claimed.delivery().id(),
            "the held row must be skipped, not handed over after waiting for its lock");
      } catch (java.util.concurrent.TimeoutException waited) {
        claim.cancel(true);
        fail("claimNext waited for the row lock of another instance instead of skipping it");
      } finally {
        release.countDown();
        holder.get(10, java.util.concurrent.TimeUnit.SECONDS);
      }
    }
  }

  /**
   * Reclama, «envía» al receptor lento y liquida, hasta que no queda nada que reclamar. Devuelve
   * los identificadores de lo que esta instancia entregó, para poder afirmar que ambas avanzaron.
   */
  private static List<UUID> deliverAll(String owner, List<UUID> sent) throws InterruptedException {
    var work = work();
    var mine = new ArrayList<UUID>();
    for (var attempt = 0; attempt < 200; attempt++) {
      var next = work.claimNext(T);
      if (next.isEmpty()) break;
      var claimed = next.get();
      if (!claimed.ownerId().equals(owner)) continue;
      // El receptor del Given tarda en responder: es lo que solapa a las dos instancias.
      Thread.sleep(RECEIVER_MILLIS);
      sent.add(claimed.delivery().eventId());
      work.record(claimed, claimed.delivery().recorded(WebhookAttempt.http(200, 1), T), null);
      mine.add(claimed.delivery().id());
    }
    return mine;
  }

  @Test
  void s27_recordingAnExhaustedDeliveryDisablesItsEndpointInTheSameWrite() {
    var owner = "exhaust-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();
    var claimed = claimOwn(work, owner, T);
    var result = claimed.delivery().recorded(WebhookAttempt.http(500, 3), T);
    var exhausted =
        new WebhookDelivery(
            result.id(),
            result.eventId(),
            result.eventType(),
            "exhausted",
            6,
            500,
            3,
            "HTTP_ERROR",
            null,
            result.createdAt(),
            T);

    work.record(claimed, exhausted, claimed.endpoint().disabledByExhaustion(T));

    var stored = store().find(owner, endpoint.id()).orElseThrow();
    assertEquals("disabled", stored.status());
    assertEquals("DELIVERY_EXHAUSTED", stored.disabledReason());
    assertEquals(T, stored.disabledAt());
    var log = store().list(owner, endpoint.id());
    assertEquals(1, log.size());
    assertEquals("exhausted", log.getFirst().status());
    assertEquals(6, log.getFirst().attempt());
  }

  @Test
  void s29_onlyTheFiftyMostRecentTerminalsSurviveAndPendingOnesAreNeverPruned() {
    var owner = "prune-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    var work = work();
    // Recorded oldest first: index 0 has the smallest updatedAt, index 54 the largest.
    var recorded = new ArrayList<UUID>();
    for (var index = 0; index < 55; index++) {
      var delivery = WebhookDelivery.ping(UUID.randomUUID(), T);
      recorded.add(delivery.id());
      store().enqueuePing(owner, endpoint.id(), delivery, "{}");
      var claimed = claimOwn(work, owner, T.plusSeconds(index));
      work.record(
          claimed,
          claimed.delivery().recorded(WebhookAttempt.http(200, 1), T.plusSeconds(index)),
          null);
    }
    var pendingOne = enqueue(owner, endpoint.id(), "{}");
    var pendingTwo = enqueue(owner, endpoint.id(), "{}");

    var log = store().list(owner, endpoint.id());
    assertEquals(52, log.size(), "fifty terminals plus the two pending ones");
    var survivors =
        log.stream()
            .filter(delivery -> "succeeded".equals(delivery.status()))
            .map(WebhookDelivery::id)
            .toList();
    // Which fifty, and in which order: cardinality alone would let a random prune pass.
    assertEquals(
        recorded.subList(5, 55).reversed(),
        survivors,
        "the fifty terminals of greatest updatedAt, newest first");
    assertTrue(
        java.util.Collections.disjoint(recorded.subList(0, 5), survivors),
        "the five oldest terminals are the ones pruned");
    var ids = log.stream().map(WebhookDelivery::id).toList();
    assertTrue(ids.contains(pendingOne.id()));
    assertTrue(ids.contains(pendingTwo.id()));
  }
}
