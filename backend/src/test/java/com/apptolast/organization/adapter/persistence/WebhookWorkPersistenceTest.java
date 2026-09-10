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

  /**
   * Un abridor de secretos que se niega con UN endpoint concreto, como hace {@code
   * AesGcmWebhookSecrets.decrypt} con una fila sellada por una clave ya rotada (project-spec.md,
   * «Rotación de APP_CONNECTOR_KEY invalida los secretos guardados»). El mensaje es el mismo de
   * producción: no dice nada del material de la clave ni del secreto.
   */
  private static PostgresWebhookWork workRefusing(UUID unreadable) {
    return new PostgresWebhookWork(
        WebhookPersistenceTest.Database.JDBC,
        WebhookPersistenceTest.Database.TRANSACTIONS,
        (owner, endpointId, ciphertext) -> {
          if (unreadable.equals(endpointId))
            throw new IllegalStateException("Webhook secret cannot be opened");
          return new String(ciphertext, StandardCharsets.UTF_8).replace("cipher:", "");
        });
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

  /**
   * @s24 «status pending y nextAttemptAt T + 1 min». El dominio calcula ese instante ({@link
   *     com.apptolast.organization.domain.RetrySchedule}) pero quien lo hace cumplir es una sola
   *     línea de SQL: la cláusula {@code AND d.next_attempt_at <= ?} de {@code claimNext}. Sin
   *     ella, la entrega recién fallada vuelve a ser reclamable en la misma pasada, y como {@code
   *     DispatchWebhooks.runCycle} reclama hasta veinte veces por ciclo, los seis intentos se
   *     queman de golpe contra el receptor caído: el webhook acaba {@code disabled} con {@code
   *     DELIVERY_EXHAUSTED} en un solo tick en lugar de a lo largo del backoff.
   *     <p>Ninguna prueba creaba jamás una entrega con vencimiento futuro —todas nacen con {@code
   *     nextAttemptAt = T}—, así que la cláusula se podía borrar entera y la suite seguía verde.
   *     PIT tampoco la ve: no muta literales de cadena y el SQL es uno.
   *     <p>El instante de vencimiento no se escribe a mano, se toma del resultado que devuelve el
   *     dominio: si algún día cambia la tabla de reintentos, esta prueba no caduca.
   */
  @Test
  void s24_aFailedDeliveryIsNotClaimableUntilTheClockReachesItsNextAttempt() {
    var owner = "backoff-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();
    var claimed = claimOwn(work, owner, T);

    var failed = claimed.delivery().recorded(WebhookAttempt.http(500, 1), T);
    work.record(claimed, failed, null);
    var due = failed.nextAttemptAt();

    assertEquals(T.plus(Duration.ofMinutes(1)), due, "@s24: tras el primer fallo, T + 1 min");
    assertTrue(
        claimedFor(work, owner, T).isEmpty(),
        "la entrega recién fallada no se vuelve a reclamar en la misma pasada");
    assertTrue(
        claimedFor(work, owner, due.minusMillis(1)).isEmpty(),
        "un instante antes del vencimiento sigue sin ser reclamable");

    var reclaimed = claimedFor(work, owner, due);
    assertEquals(1, reclaimed.size(), "cuando el reloj alcanza el vencimiento vuelve al ciclo");
    assertEquals(failed.id(), reclaimed.getFirst().delivery().id());
    assertEquals(1, reclaimed.getFirst().delivery().attempt(), "el intento fallido está contado");
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

  /**
   * Lo persistido, leído por SQL directo y por tanto <b>sin</b> el tope de página de la lectura.
   * Sin este atajo, la línea 367 del contrato («quedan persistidas exactamente 50 terminales y las
   * 2 pendientes») no tendría forma de comprobarse: el propio tope la ocultaría.
   */
  private static List<UUID> persistedIds(UUID endpointId, String predicate) {
    return WebhookPersistenceTest.Database.JDBC.queryForList(
        "SELECT id FROM webhook_deliveries WHERE endpoint_id=? AND "
            + predicate
            + " ORDER BY updated_at DESC, id DESC",
        UUID.class,
        endpointId);
  }

  /**
   * @s29, las dos líneas del escenario a la vez y con una aserción para cada una, porque hablan de
   *     cosas distintas: la 367 de lo que <b>se guarda</b> tras la poda y la 368 de lo que <b>se
   *     sirve</b> en una respuesta. Leerlas como una contradicción era el error; una cosa es la
   *     tabla y otra la página.
   *     <p>Resuelto por el propietario el 10-09-2026: se persisten las 52 filas y el GET devuelve
   *     50 como máximo. Ninguna de las dos líneas del contrato se enmienda.
   */
  @Test
  void s29_fiftyTwoRowsSurviveThePruneAndTheReadServesAtMostFifty() {
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

    // Línea 367: lo que la poda deja en la tabla.
    var persisted = persistedIds(endpoint.id(), "TRUE");
    assertEquals(52, persisted.size(), "fifty terminals plus the two pending ones are stored");
    var survivors = persistedIds(endpoint.id(), "status='succeeded'");
    // Which fifty, and in which order: cardinality alone would let a random prune pass.
    assertEquals(
        recorded.subList(5, 55).reversed(),
        survivors,
        "the fifty terminals of greatest updatedAt, newest first");
    assertTrue(
        java.util.Collections.disjoint(recorded.subList(0, 5), survivors),
        "the five oldest terminals are the ones pruned");
    assertEquals(
        List.of(pendingTwo.id(), pendingOne.id()).stream().sorted().toList(),
        persistedIds(endpoint.id(), "status='pending'").stream().sorted().toList(),
        "neither pending delivery is ever pruned");

    // Línea 368: lo que la lectura sirve. Es una respuesta, no la tabla.
    var served = store().list(owner, endpoint.id());
    assertEquals(50, served.size(), "«items de como máximo 50 elementos»");
    assertEquals(
        persisted.subList(0, 50),
        served.stream().map(WebhookDelivery::id).toList(),
        "the first fifty of updatedAt DESC then id DESC, and no others");
  }

  /**
   * B10 del panel de precierre. Un secreto que no se puede abrir detenía la cola de <b>todos</b>
   * los propietarios, en silencio y para siempre.
   *
   * <p>El descifrado vivía dentro del {@code RowMapper} de {@code claimNext}, es decir <b>antes</b>
   * del {@code UPDATE ... SET leased_until}. Cuando la fila estaba sellada con una clave ya rotada
   * —escenario que el propio project-spec declara esperado— el {@code RowMapper} lanzaba, la
   * transacción entera se deshacía y la fila quedaba <b>sin arrendar</b>. Como el orden de
   * reclamación es {@code next_attempt_at, d.id}, esa misma fila volvía a ser la primera en el tic
   * siguiente, un segundo después, para siempre. {@code WebhookSchedule.guarded} se tragaba la
   * excepción y sólo registraba el nombre de la clase en su propio logger, sin pasar por {@code
   * WebhookAudit}: ninguna entrega volvía a salir, ninguna quedaba marcada como fallida y {@code
   * GET /deliveries} no mostraba nada anómalo.
   *
   * <p>Dos oráculos, y los dos son necesarios: que la entrega del vecino <b>salga</b> (la cola no
   * se detiene) y que la fila ilegible <b>quede arrendada</b> (no vuelve a encabezar cada tic). Con
   * cualquiera de los dos por separado el defecto se colaba.
   */
  @Test
  void b10_anUnreadableSecretNeitherStopsTheQueueNorKeepsItsTurnForever() {
    var stuck = "stuck-" + UUID.randomUUID();
    var neighbour = "neighbour-" + UUID.randomUUID();
    var poisoned = given(stuck, "active");
    var sound = given(neighbour, "active");
    var first = enqueue(stuck, poisoned.id(), "{}");
    var second = enqueue(neighbour, sound.id(), "{}");
    // La fila envenenada encabeza el orden de reclamación, que es donde hace daño.
    WebhookPersistenceTest.Database.JDBC.update(
        "UPDATE webhook_deliveries SET next_attempt_at=? WHERE id=?",
        java.sql.Timestamp.from(T.minus(Duration.ofSeconds(10))),
        first.id());
    var work = workRefusing(poisoned.id());
    var mine = List.of(stuck, neighbour);

    var claimed = claimedForAny(work, mine, T);

    assertEquals(
        List.of(first.id(), second.id()),
        claimed.stream().map(each -> each.delivery().id()).toList(),
        "la entrega del vecino sale detrás de la ilegible: la cola no se detiene");
    assertNull(
        claimed.getFirst().secret(),
        "un secreto que no se puede abrir llega como ausente, no como una excepción");
    assertEquals(SECRET, claimed.get(1).secret(), "el secreto legible del vecino sigue abriéndose");
    assertTrue(
        claimedForAny(work, mine, T).isEmpty(),
        "la fila ilegible queda arrendada: no vuelve a ser la primera en el tic siguiente");
  }

  /** Reclama hasta agotar la cola y devuelve, en orden, lo que salió de los propietarios dados. */
  private static List<com.apptolast.organization.application.ClaimedDelivery> claimedForAny(
      PostgresWebhookWork work, List<String> owners, Instant now) {
    var claimed = new ArrayList<com.apptolast.organization.application.ClaimedDelivery>();
    for (var attempt = 0; attempt < 200; attempt++) {
      var next = work.claimNext(now);
      if (next.isEmpty()) break;
      if (owners.contains(next.get().ownerId())) claimed.add(next.get());
    }
    return claimed;
  }

  /**
   * B10, tercera pieza y la que casi se me escapa. Que la aplicación decida marcar la entrega como
   * fallida no sirve de nada si la fila <b>no se puede escribir</b>: {@code webhook_deliveries}
   * tiene un CHECK que enumera las clases de error permitidas, y una clase nueva rebota contra él.
   * Sin esta prueba el arreglo de la cola se habría cambiado por una excepción en {@code record},
   * es decir por el mismo silencio en otro sitio.
   */
  @Test
  void b10_aDeliveryFailedForAnUnreadableSecretCanActuallyBeWritten() {
    var owner = "unreadable-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();
    var claimed = claimOwn(work, owner, T);

    work.record(claimed, claimed.delivery().recorded(WebhookAttempt.unreadableSecret(), T), null);

    var stored = store().list(owner, endpoint.id()).getFirst();
    assertEquals("pending", stored.status(), "es un intento fallido más, con su reintento");
    assertEquals(1, stored.attempt());
    assertEquals(WebhookAttempt.SECRET_UNREADABLE, stored.errorClass());
    assertNull(stored.httpStatus(), "no hubo respuesta porque no hubo petición");
    assertEquals(0, stored.latencyMs(), "ni tiempo de intercambio que medir");
    assertEquals(T.plus(Duration.ofMinutes(1)), stored.nextAttemptAt());
  }

  /**
   * @s27 «si la transacción de desactivación falla, D1 no queda exhausted ni el webhook disabled».
   *     El único test del escenario era el camino feliz y no había ninguno que provocara el fallo
   *     de escritura (B8.a del panel): la atomicidad estaba declarada en un javadoc y en ningún
   *     oráculo.
   *     <p>El fallo se provoca con una razón de desactivación fuera del catálogo del esquema, que
   *     es un fallo real de la base y no un doble: lo rechaza el mismo CHECK que protegería la
   *     columna en producción. Lo que se mide es que la primera escritura de la transacción —la de
   *     la entrega— se deshace con la segunda.
   */
  @Test
  void s27_whenTheDisablingWriteFailsNeitherTheDeliveryNorTheEndpointChanges() {
    var owner = "atomic-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();
    var claimed = claimOwn(work, owner, T);
    var exhausted =
        new WebhookDelivery(
            claimed.delivery().id(),
            claimed.delivery().eventId(),
            claimed.delivery().eventType(),
            "exhausted",
            6,
            500,
            3,
            "HTTP_ERROR",
            null,
            claimed.delivery().createdAt(),
            T);
    var rejected =
        new WebhookEndpoint(
            endpoint.id(),
            endpoint.url(),
            endpoint.description(),
            endpoint.eventTypes(),
            "disabled",
            "RAZON_FUERA_DEL_CATALOGO",
            T,
            endpoint.createdAt(),
            T);

    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> work.record(claimed, exhausted, rejected));

    var stored = store().find(owner, endpoint.id()).orElseThrow();
    assertEquals("active", stored.status(), "el webhook no queda disabled");
    assertNull(stored.disabledReason());
    assertNull(stored.disabledAt());
    var log = store().list(owner, endpoint.id());
    assertEquals(1, log.size());
    assertEquals("pending", log.getFirst().status(), "D1 no queda exhausted");
    assertEquals(0, log.getFirst().attempt(), "ni con el intento contado");
    assertNull(log.getFirst().errorClass());
  }

  /**
   * @s20 «el receptor recibe exactamente tres POST con X-OrganizationWeb-Event-Id E1, E2 y E3 en
   *     ese orden» y «un ping encolado entre E1 y E2 se entrega sin alterar el orden relativo».
   *     <p>El orden real se medía con un {@code FakeSender}, dos entregas <b>ya reclamadas</b> y un
   *     solo ciclo, y el ping intercalado no se encolaba en ninguna parte (B8.b del panel). Eso
   *     mide que el caso de uso envía en el orden en que le den las entregas, no que la cadena
   *     entera —caminar la outbox, encolar de una en una, reclamar por {@code next_attempt_at, id}—
   *     conserve el orden de la outbox. Aquí corren los adaptadores reales contra la base, ciclo a
   *     ciclo.
   *     <p>El ping se encola después de que E1 haya salido. No se afirma en qué posición absoluta
   *     llega —comparte instante de vencimiento con E2 y el desempate es por identificador, que es
   *     aleatorio—, sino lo que dice el contrato: que llega, y que E1, E2 y E3 conservan su orden
   *     relativo entre ellos.
   */
  @Test
  void s20_theThreeEventsReachTheReceiverInOrderAndAnInterleavedPingDoesNotDisturbThem() {
    var owner = "order-" + UUID.randomUUID();
    var project = givenProject(owner);
    var endpoint = givenSubscribedToProjects(owner);
    var first = givenEvent(owner, project, T.plusSeconds(1));
    var second = givenEvent(owner, project, T.plusSeconds(2));
    var third = givenEvent(owner, project, T.plusSeconds(3));
    var received = new ArrayList<UUID>();
    var worker = worker(received);

    worker.run();
    var ping = WebhookDelivery.ping(UUID.randomUUID(), T);
    store().enqueuePing(owner, endpoint.id(), ping, "{}");
    worker.run();
    worker.run();
    worker.run();

    var events = List.of(first, second, third);
    assertEquals(
        events,
        received.stream().filter(events::contains).toList(),
        "«tres POST con X-OrganizationWeb-Event-Id E1, E2 y E3 en ese orden»");
    assertTrue(received.contains(ping.id()), "«un ping encolado entre E1 y E2 se entrega»");
  }

  /** Un ciclo completo del worker con los adaptadores reales: encolar y después entregar. */
  private static Runnable worker(List<UUID> received) {
    var clock = java.time.Clock.fixed(T.plus(Duration.ofMinutes(1)), java.time.ZoneOffset.UTC);
    var audit = noAudit();
    var outbox =
        new PostgresWebhookOutbox(
            WebhookPersistenceTest.Database.JDBC,
            WebhookPersistenceTest.Database.TRANSACTIONS,
            new com.fasterxml.jackson.databind.ObjectMapper());
    var enqueue =
        new com.apptolast.organization.application.EnqueueWebhookDeliveries(outbox, audit, clock);
    com.apptolast.organization.application.WebhookSender sender =
        (url, secret, eventId, body) -> {
          received.add(UUID.fromString(eventId));
          return WebhookAttempt.http(200, 1);
        };
    var dispatch =
        new com.apptolast.organization.application.DispatchWebhooks(
            work(), sender, audit, alwaysKeyed(), clock);
    return () -> {
      enqueue.runCycle();
      dispatch.runCycle();
    };
  }

  private static com.apptolast.organization.application.WebhookAudit noAudit() {
    return new com.apptolast.organization.application.WebhookAudit() {
      @Override
      public void attempt(UUID endpointId, UUID eventId, String status, String errorClass) {}

      @Override
      public void discarded(UUID endpointId, UUID eventId, String code) {}

      @Override
      public void workerError(String code) {}
    };
  }

  private static com.apptolast.organization.application.WebhookSecrets alwaysKeyed() {
    return new com.apptolast.organization.application.WebhookSecrets() {
      @Override
      public boolean available() {
        return true;
      }

      @Override
      public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static WebhookEndpoint givenSubscribedToProjects(String owner) {
    var endpoint =
        new WebhookEndpoint(
            UUID.randomUUID(),
            "https://example.com/h",
            "",
            List.of("ProjectCreated.v1"),
            "active",
            null,
            null,
            T,
            T);
    store().insert(owner, endpoint, ("cipher:" + SECRET).getBytes(StandardCharsets.UTF_8));
    return endpoint;
  }

  /** outbox_events.aggregate_id apunta a projects(id), así que hace falta un proyecto real. */
  private static UUID givenProject(String owner) {
    var id = UUID.randomUUID();
    WebhookPersistenceTest.Database.JDBC.update(
        """
        INSERT INTO projects (id, owner_id, name, description, status, version, created_at,
          updated_at)
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

  private static UUID givenEvent(String owner, UUID project, Instant occurredAt) {
    var eventId = UUID.randomUUID();
    var payload =
        "{\"eventId\":\""
            + eventId
            + "\",\"aggregateId\":\""
            + project
            + "\",\"ownerId\":\""
            + owner
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":1,\"type\":\"ProjectCreated.v1\",\"name\":\"Proyecto\"}";
    WebhookPersistenceTest.Database.JDBC.update(
        """
        INSERT INTO outbox_events (
          event_id, aggregate_id, owner_id, event_type, schema_version, occurred_at, payload, status)
        VALUES (?,?,?,?,?,?,?::jsonb,?)
        """,
        eventId,
        project,
        owner,
        "ProjectCreated.v1",
        1,
        java.sql.Timestamp.from(occurredAt),
        payload,
        "pending");
    return eventId;
  }
}
