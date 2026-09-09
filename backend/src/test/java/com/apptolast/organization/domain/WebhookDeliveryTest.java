package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookDeliveryTest {
  private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");
  private static final Instant LATER = Instant.parse("2026-09-08T13:00:00Z");
  private static final UUID D = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final UUID E = UUID.fromString("11111111-1111-4111-8111-111111111111");

  @Test
  void s14_s17_aPingIsItsOwnEventAndStartsPendingWithoutAttempts() {
    var ping = WebhookDelivery.ping(D, NOW);
    assertEquals(D, ping.id());
    assertEquals(D, ping.eventId());
    assertEquals("webhook.ping.v1", ping.eventType());
    assertEquals("pending", ping.status());
    assertEquals(0, ping.attempt());
    assertNull(ping.httpStatus());
    assertNull(ping.latencyMs());
    assertNull(ping.errorClass());
    assertEquals(NOW, ping.nextAttemptAt());
    assertEquals(NOW, ping.createdAt());
    assertEquals(NOW, ping.updatedAt());
    assertTrue(ping.isPending());
    assertFalse(ping.isTerminal());
  }

  @Test
  void s30_onlySucceededAndExhaustedAreTerminal() {
    assertTrue(delivery("succeeded", 6).isTerminal());
    assertTrue(delivery("exhausted", 6).isTerminal());
    assertFalse(delivery("pending", 6).isTerminal());
    assertTrue(delivery("pending", 6).isPending());
    assertFalse(delivery("succeeded", 6).isPending());
  }

  @Test
  void s30_s31_requeueingKeepsIdentityAndClearsTheOutcome() {
    var requeued = delivery("succeeded", 6).requeued(LATER);
    assertEquals(D, requeued.id());
    assertEquals(E, requeued.eventId());
    assertEquals("TaskCreated.v1", requeued.eventType());
    assertEquals("pending", requeued.status());
    assertEquals(0, requeued.attempt());
    assertNull(requeued.httpStatus());
    assertNull(requeued.latencyMs());
    assertNull(requeued.errorClass());
    assertEquals(LATER, requeued.nextAttemptAt());
    assertEquals(NOW, requeued.createdAt());
    assertEquals(LATER, requeued.updatedAt());
  }

  private static WebhookDelivery delivery(String status, int attempt) {
    return new WebhookDelivery(
        D, E, "TaskCreated.v1", status, attempt, 200, 12, null, null, NOW, NOW);
  }
}
