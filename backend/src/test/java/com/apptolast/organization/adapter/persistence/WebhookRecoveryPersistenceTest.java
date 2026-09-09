package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.adapter.webhook.WebhookSignature;
import com.apptolast.organization.application.ClaimedDelivery;
import com.apptolast.organization.application.DispatchWebhooks;
import com.apptolast.organization.application.EnqueueWebhookDeliveries;
import com.apptolast.organization.application.ManageWebhook;
import com.apptolast.organization.application.WebhookAudit;
import com.apptolast.organization.application.WebhookSecrets;
import com.apptolast.organization.application.WebhookSender;
import com.apptolast.organization.domain.WebhookAttempt;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** End-to-end over the real schema: reactivation (@s28) and redelivery (@s31). */
class WebhookRecoveryPersistenceTest {
  private static final Instant T = Instant.parse("2026-09-08T12:00:00.000000Z");
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";

  /** Reversible stand-in: the stored bytes are the secret itself. */
  private static final WebhookSecrets SECRETS =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return true;
        }

        @Override
        public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
          return secret.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
          return new String(ciphertext, StandardCharsets.UTF_8);
        }
      };

  private static final WebhookAudit SILENT =
      new WebhookAudit() {
        @Override
        public void attempt(UUID endpointId, UUID eventId, String status, String errorClass) {}

        @Override
        public void discarded(UUID endpointId, UUID eventId, String code) {}

        @Override
        public void workerError(String code) {}
      };

  private record Sent(String eventId, String body, String signature) {}

  /** Records every outgoing attempt and signs exactly as the real adapter does. */
  private static final class RecordingSender implements WebhookSender {
    final List<Sent> sent = new ArrayList<>();
    Instant now = T;
    WebhookAttempt outcome = WebhookAttempt.http(200, 4);

    @Override
    public WebhookAttempt send(String url, String secret, String eventId, String body) {
      sent.add(
          new Sent(
              eventId,
              body,
              WebhookSignature.header(
                  secret, now.getEpochSecond(), body.getBytes(StandardCharsets.UTF_8))));
      return outcome;
    }
  }

  private static PostgresWebhookStore store() {
    return new PostgresWebhookStore(
        WebhookPersistenceTest.Database.JDBC, WebhookPersistenceTest.Database.TRANSACTIONS);
  }

  private static PostgresWebhookWork work() {
    return new PostgresWebhookWork(
        WebhookPersistenceTest.Database.JDBC,
        WebhookPersistenceTest.Database.TRANSACTIONS,
        SECRETS::decrypt);
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
            UUID.randomUUID(),
            "https://example.com/hooks?token=abc",
            "",
            types,
            "active",
            null,
            null,
            T,
            T);
    store().insert(owner, endpoint, SECRETS.encrypt(owner, endpoint.id(), SECRET));
    return endpoint;
  }

  /** Drains the queue for one owner, one delivery at a time, like repeated worker cycles. */
  private static void drain(RecordingSender sender, String owner, Instant now) {
    var work = work();
    for (var cycle = 0; cycle < 30; cycle++) {
      var claimed = claimOwn(work, owner, now);
      if (claimed == null) return;
      var outcome = sender.send(claimed.endpoint().url(), claimed.secret(),
          claimed.delivery().eventId().toString(), claimed.body());
      var result = claimed.delivery().recorded(outcome, now);
      work.record(
          claimed,
          result,
          WebhookDelivery.EXHAUSTED.equals(result.status())
              ? claimed.endpoint().disabledByExhaustion(now)
              : null);
    }
  }

  private static ClaimedDelivery claimOwn(PostgresWebhookWork work, String owner, Instant now) {
    for (var attempt = 0; attempt < 200; attempt++) {
      var next = work.claimNext(now);
      if (next.isEmpty()) return null;
      if (next.get().ownerId().equals(owner)) return next.get();
    }
    return null;
  }

  @Test
  void s28_reactivatingResumesFromTheKeptCursorAndNeverResendsTheExhaustedDelivery() {
    var owner = "reactivate-" + UUID.randomUUID();
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    var store = store();

    // D1 exhausted, D2 still pending, endpoint disabled by exhaustion.
    var exhausted = WebhookDelivery.ping(UUID.randomUUID(), T);
    store.enqueuePing(owner, endpoint.id(), exhausted, "{\"d\":1}");
    var pending = WebhookDelivery.ping(UUID.randomUUID(), T.plusSeconds(1));
    store.enqueuePing(owner, endpoint.id(), pending, "{\"d\":2}");
    WebhookPersistenceTest.Database.JDBC.update(
        """
        UPDATE webhook_deliveries
           SET status='exhausted', attempt=6, next_attempt_at=NULL, error_class='HTTP_ERROR'
         WHERE id=?
        """,
        exhausted.id());
    store.changeStatus(owner, endpoint.id(), "disabled", T);
    WebhookPersistenceTest.Database.JDBC.update(
        "UPDATE webhook_endpoints SET disabled_reason='DELIVERY_EXHAUSTED' WHERE id=?",
        endpoint.id());

    var cursorBefore =
        WebhookPersistenceTest.Database.JDBC.queryForObject(
            "SELECT cursor_occurred_at FROM webhook_endpoints WHERE id=?",
            java.sql.Timestamp.class,
            endpoint.id());

    // While disabled the worker must not send anything at all.
    var sender = new RecordingSender();
    drain(sender, owner, T.plusSeconds(10));
    assertTrue(sender.sent.isEmpty(), "a disabled endpoint delivers nothing");

    // Reactivating through the use case, exactly as the API would.
    var manage =
        new ManageWebhook(
            store, store, SECRETS, Clock.fixed(T.plusSeconds(20), ZoneOffset.UTC));
    var reactivated = manage.changeStatus(owner, endpoint.id(), "active");
    assertEquals("active", reactivated.status());
    assertNull(reactivated.disabledReason());
    assertNull(reactivated.disabledAt());
    assertEquals(
        cursorBefore,
        WebhookPersistenceTest.Database.JDBC.queryForObject(
            "SELECT cursor_occurred_at FROM webhook_endpoints WHERE id=?",
            java.sql.Timestamp.class,
            endpoint.id()),
        "reactivating never rewinds nor advances the cursor");

    sender.now = T.plusSeconds(30);
    drain(sender, owner, T.plusSeconds(30));

    assertEquals(
        List.of(pending.eventId().toString()),
        sender.sent.stream().map(Sent::eventId).toList(),
        "only D2 goes out; D1 is terminal and is never resent");
    assertEquals(
        "exhausted",
        store.list(owner, endpoint.id()).stream()
            .filter(delivery -> delivery.id().equals(exhausted.id()))
            .findFirst()
            .orElseThrow()
            .status());
    assertEquals(
        "succeeded",
        store.list(owner, endpoint.id()).stream()
            .filter(delivery -> delivery.id().equals(pending.id()))
            .findFirst()
            .orElseThrow()
            .status());
  }

  @Test
  void s31_aRedeliveredDeliveryKeepsItsBodyAndEventAndGoesOutWithAFreshSignature() {
    var owner = "redeliver-" + UUID.randomUUID();
    var endpoint = given(owner, List.of("ProjectCreated.v1"));
    var store = store();
    var body = "{\"eventId\":\"11111111-1111-4111-8111-111111111111\",\"n\":1}";
    // @s31 fixes the first attempt at 10:00, so the delivery must be due then.
    var firstAt = Instant.parse("2026-09-08T10:00:00Z");
    var delivery = WebhookDelivery.ping(UUID.randomUUID(), firstAt);
    store.enqueuePing(owner, endpoint.id(), delivery, body);

    var first = new RecordingSender();
    first.now = firstAt;
    drain(first, owner, first.now);
    assertEquals(1, first.sent.size());
    assertEquals("t=1788861600", first.sent.getFirst().signature().split(",")[0]);

    var rowsAfterFirst = countRows(endpoint.id());

    // The owner asks for a redelivery at 13:00.
    var redeliverAt = Instant.parse("2026-09-08T13:00:00Z");
    var manage =
        new ManageWebhook(store, store, SECRETS, Clock.fixed(redeliverAt, ZoneOffset.UTC));
    var reopened = manage.redeliver(owner, endpoint.id(), delivery.id());
    assertEquals("pending", reopened.status());
    assertEquals(0, reopened.attempt());
    assertEquals(delivery.eventId(), reopened.eventId());

    var second = new RecordingSender();
    second.now = redeliverAt;
    drain(second, owner, redeliverAt);

    assertEquals(1, second.sent.size());
    assertEquals(body, second.sent.getFirst().body(), "the original body goes out untouched");
    assertEquals(
        first.sent.getFirst().eventId(),
        second.sent.getFirst().eventId(),
        "the same event id identifies the redelivery");
    assertEquals("t=1788872400", second.sent.getFirst().signature().split(",")[0]);
    assertNotEquals(
        first.sent.getFirst().signature(),
        second.sent.getFirst().signature(),
        "a new instant means a new signature over the same bytes");

    var settled =
        store.list(owner, endpoint.id()).stream()
            .filter(row -> row.id().equals(delivery.id()))
            .findFirst()
            .orElseThrow();
    assertEquals("succeeded", settled.status());
    assertEquals(1, settled.attempt());
    assertEquals(rowsAfterFirst, countRows(endpoint.id()), "a redelivery adds no row to the log");
  }

  private static int countRows(UUID endpointId) {
    return WebhookPersistenceTest.Database.JDBC.queryForObject(
        "SELECT count(*) FROM webhook_deliveries WHERE endpoint_id=?", Integer.class, endpointId);
  }
}
