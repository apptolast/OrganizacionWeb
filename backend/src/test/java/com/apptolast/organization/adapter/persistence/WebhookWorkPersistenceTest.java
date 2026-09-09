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

    var claimed = work().claimNext(T).orElseThrow();

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

    assertTrue(work.claimNext(T).isPresent());
    assertTrue(claimedFor(work, owner, T).isEmpty(), "the lease must hide it from the next cycle");

    // The worker died before recording: once the lease lapses the same delivery is retried.
    var recovered = claimedFor(work, owner, T.plus(Duration.ofMinutes(10)));
    assertEquals(1, recovered.size());
    assertEquals(0, recovered.getFirst().delivery().attempt());
  }

  private static List<com.apptolast.organization.application.ClaimedDelivery> claimedFor(
      PostgresWebhookWork work, String owner, Instant now) {
    var claimed = new ArrayList<com.apptolast.organization.application.ClaimedDelivery>();
    for (var attempt = 0; attempt < 20; attempt++) {
      var next = work.claimNext(now);
      if (next.isEmpty()) break;
      if (next.get().ownerId().equals(owner)) claimed.add(next.get());
    }
    return claimed;
  }

  @Test
  void s22_aDisabledEndpointNeverYieldsItsPendingDeliveries() {
    var owner = "off-" + UUID.randomUUID();
    var endpoint = given(owner, "disabled");
    enqueue(owner, endpoint.id(), "{}");

    assertTrue(claimedFor(work(), owner, T).isEmpty());
  }

  @Test
  void s23_twoWorkersNeverClaimTheSameDeliveryAndNeitherWaitsForTheOther() throws Exception {
    var owner = "race-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    for (var index = 0; index < 10; index++) enqueue(owner, endpoint.id(), "{}");

    Callable<List<UUID>> worker =
        () ->
            claimedFor(work(), owner, T).stream()
                .map(claimed -> claimed.delivery().id())
                .toList();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var results = pool.invokeAll(List.of(worker, worker));
      var all = new ArrayList<UUID>();
      for (var result : results) all.addAll(result.get());
      assertEquals(10, all.size(), "each delivery is claimed exactly once");
      assertEquals(10, java.util.Set.copyOf(all).size(), "no delivery is claimed twice");
    }
  }

  @Test
  void s27_recordingAnExhaustedDeliveryDisablesItsEndpointInTheSameWrite() {
    var owner = "exhaust-" + UUID.randomUUID();
    var endpoint = given(owner, "active");
    enqueue(owner, endpoint.id(), "{}");
    var work = work();
    var claimed = work.claimNext(T).orElseThrow();
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
    for (var index = 0; index < 55; index++) {
      var delivery = WebhookDelivery.ping(UUID.randomUUID(), T);
      store().enqueuePing(owner, endpoint.id(), delivery, "{}");
      var claimed = work.claimNext(T.plusSeconds(index)).orElseThrow();
      work.record(
          claimed,
          claimed.delivery().recorded(WebhookAttempt.http(200, 1), T.plusSeconds(index)),
          null);
    }
    var pendingOne = enqueue(owner, endpoint.id(), "{}");
    var pendingTwo = enqueue(owner, endpoint.id(), "{}");

    var log = store().list(owner, endpoint.id());
    assertEquals(52, log.size(), "fifty terminals plus the two pending ones");
    assertEquals(
        50, log.stream().filter(delivery -> "succeeded".equals(delivery.status())).count());
    var ids = log.stream().map(WebhookDelivery::id).toList();
    assertTrue(ids.contains(pendingOne.id()));
    assertTrue(ids.contains(pendingTwo.id()));
  }
}
