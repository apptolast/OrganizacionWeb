package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookEndpoint;
import com.apptolast.organization.domain.WebhookInvalidException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManageWebhookTest {
  private static final Instant NOW = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final UUID W = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID D = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final UUID UNKNOWN = UUID.fromString("44444444-4444-4444-8444-444444444444");
  private static final String OWNER = "owner-a";

  private static WebhookEndpoint endpoint(String status) {
    return new WebhookEndpoint(
        W,
        "https://example.com/h",
        "",
        List.of("TaskCreated.v1"),
        status,
        null,
        null,
        NOW,
        NOW);
  }

  private static ManageWebhook manage(
      FakeWebhookEndpoints endpoints, FakeWebhookDeliveries deliveries, WebhookSecrets secrets) {
    return new ManageWebhook(endpoints, deliveries, secrets, CLOCK);
  }

  private static FakeWebhookEndpoints withActiveEndpoint() {
    var endpoints = new FakeWebhookEndpoints();
    endpoints.stored.add(endpoint("active"));
    return endpoints;
  }

  private static WebhookOperationException.Code codeOf(
      org.junit.jupiter.api.function.Executable call) {
    return assertThrows(WebhookOperationException.class, call).code();
  }

  @Test
  void s14_s17_pingEnqueuesASyntheticDeliveryWhoseEventIdIsItsOwnId() {
    var deliveries = new FakeWebhookDeliveries();
    var delivery = manage(withActiveEndpoint(), deliveries, FakeWebhookSecrets.KEYED).ping(OWNER, W);
    assertSame(deliveries.enqueued, delivery);
    assertEquals(W, deliveries.enqueuedFor);
    assertEquals(delivery.id(), delivery.eventId());
    assertEquals("webhook.ping.v1", delivery.eventType());
    assertEquals("pending", delivery.status());
    assertEquals(0, delivery.attempt());
    assertNull(delivery.httpStatus());
    assertNull(delivery.latencyMs());
    assertNull(delivery.errorClass());
    assertEquals(NOW, delivery.nextAttemptAt());
    assertEquals(NOW, delivery.createdAt());
    assertEquals(NOW, delivery.updatedAt());
    assertEquals(
        "{\"eventId\":\""
            + delivery.id()
            + "\",\"aggregateId\":\"22222222-2222-4222-8222-222222222222\","
            + "\"ownerId\":\"owner-a\",\"occurredAt\":\"2026-09-08T10:00:00.000000Z\","
            + "\"schemaVersion\":1,\"type\":\"webhook.ping.v1\"}",
        deliveries.enqueuedBody);
  }

  @Test
  void s14_pingIsRefusedOnADisabledEndpointAndWhenAnotherPingIsPending() {
    var disabled = new FakeWebhookEndpoints();
    disabled.stored.add(endpoint("disabled"));
    var deliveries = new FakeWebhookDeliveries();
    assertEquals(
        WebhookOperationException.Code.DISABLED,
        codeOf(() -> manage(disabled, deliveries, FakeWebhookSecrets.KEYED).ping(OWNER, W)));
    var pending = new FakeWebhookDeliveries();
    pending.pendingPing = true;
    assertEquals(
        WebhookOperationException.Code.DELIVERY_PENDING,
        codeOf(() -> manage(withActiveEndpoint(), pending, FakeWebhookSecrets.KEYED).ping(OWNER, W)));
    assertNull(deliveries.enqueued);
    assertNull(pending.enqueued);
  }

  @Test
  void s14_aPendingOutboxDeliveryDoesNotBlockAPing() {
    var deliveries = new FakeWebhookDeliveries();
    deliveries.give(D, "pending", NOW);
    var delivery = manage(withActiveEndpoint(), deliveries, FakeWebhookSecrets.KEYED).ping(OWNER, W);
    assertEquals("pending", delivery.status());
    assertNotNull(deliveries.enqueued);
  }

  @Test
  void s11_s14_pingOnAMissingEndpointIsNotFoundBeforeTouchingDeliveries() {
    var deliveries = new FakeWebhookDeliveries();
    assertEquals(
        WebhookOperationException.Code.NOT_FOUND,
        codeOf(
            () ->
                manage(new FakeWebhookEndpoints(), deliveries, FakeWebhookSecrets.KEYED)
                    .ping(OWNER, W)));
    assertNull(deliveries.enqueued);
  }

  @Test
  void s9_pingAndRedeliverNeedTheConnectorKeyButReadsStatusAndDeleteDoNot() {
    var endpoints = withActiveEndpoint();
    var deliveries = new FakeWebhookDeliveries();
    deliveries.give(D, "succeeded", NOW);
    var manage = manage(endpoints, deliveries, WebhookSecrets.DISABLED);
    assertEquals(
        WebhookOperationException.Code.CONNECTORS_DISABLED, codeOf(() -> manage.ping(OWNER, W)));
    assertEquals(
        WebhookOperationException.Code.CONNECTORS_DISABLED,
        codeOf(() -> manage.redeliver(OWNER, W, D)));
    assertNull(deliveries.enqueued);
    assertNull(deliveries.requeued);
    assertEquals(1, manage.list(OWNER).size());
    assertEquals(1, manage.deliveries(OWNER, W).size());
    assertEquals("disabled", manage.changeStatus(OWNER, W, "disabled").status());
    assertDoesNotThrow(() -> manage.delete(OWNER, W));
  }

  @Test
  void s12_disablingStampsTheClockAndReactivatingClearsTheReason() {
    var endpoints = withActiveEndpoint();
    var manage = manage(endpoints, new FakeWebhookDeliveries(), WebhookSecrets.DISABLED);
    var disabled = manage.changeStatus(OWNER, W, "disabled");
    assertEquals("disabled", disabled.status());
    assertEquals("MANUAL", disabled.disabledReason());
    assertEquals(NOW, disabled.disabledAt());
    var reactivated = manage.changeStatus(OWNER, W, "active");
    assertEquals("active", reactivated.status());
    assertNull(reactivated.disabledReason());
    assertNull(reactivated.disabledAt());
  }

  @Test
  void s12_aStatusOutsideActiveOrDisabledIsAFieldErrorBeforeAnyLookup() {
    var endpoints = new FakeWebhookEndpoints();
    var manage = manage(endpoints, new FakeWebhookDeliveries(), WebhookSecrets.DISABLED);
    for (var status : Arrays.asList("paused", "", "ACTIVE", null)) {
      var error =
          assertThrows(
              WebhookInvalidException.class, () -> manage.changeStatus(OWNER, UNKNOWN, status));
      assertEquals("status", error.errors().getFirst().field());
    }
  }

  @Test
  void s11_s13_readsStatusDeleteAndRedeliverAllReportNotFoundForAnUnknownId() {
    var manage =
        manage(withActiveEndpoint(), new FakeWebhookDeliveries(), FakeWebhookSecrets.KEYED);
    assertEquals(WebhookOperationException.Code.NOT_FOUND, codeOf(() -> manage.find(OWNER, UNKNOWN)));
    assertEquals(
        WebhookOperationException.Code.NOT_FOUND, codeOf(() -> manage.deliveries(OWNER, UNKNOWN)));
    assertEquals(
        WebhookOperationException.Code.NOT_FOUND,
        codeOf(() -> manage.changeStatus(OWNER, UNKNOWN, "disabled")));
    assertEquals(WebhookOperationException.Code.NOT_FOUND, codeOf(() -> manage.delete(OWNER, UNKNOWN)));
    assertEquals(
        WebhookOperationException.Code.NOT_FOUND, codeOf(() -> manage.redeliver(OWNER, UNKNOWN, D)));
  }

  @Test
  void s13_deletingTwiceSucceedsOnceAndThenReportsNotFound() {
    var endpoints = withActiveEndpoint();
    var manage = manage(endpoints, new FakeWebhookDeliveries(), WebhookSecrets.DISABLED);
    manage.delete(OWNER, W);
    assertTrue(endpoints.stored.isEmpty());
    assertEquals(WebhookOperationException.Code.NOT_FOUND, codeOf(() -> manage.delete(OWNER, W)));
  }

  @Test
  void s30_redeliveringATerminalDeliveryReopensItWithTheClock() {
    for (var terminal : List.of("succeeded", "exhausted")) {
      var deliveries = new FakeWebhookDeliveries();
      var original = deliveries.give(D, terminal, Instant.parse("2026-09-01T00:00:00Z"));
      var result =
          manage(withActiveEndpoint(), deliveries, FakeWebhookSecrets.KEYED)
              .redeliver(OWNER, W, D);
      assertSame(deliveries.requeued, result);
      assertEquals(original.id(), result.id());
      assertEquals(original.eventId(), result.eventId());
      assertEquals("pending", result.status());
      assertEquals(0, result.attempt());
      assertNull(result.httpStatus());
      assertNull(result.errorClass());
      assertEquals(NOW, result.nextAttemptAt());
      assertEquals(original.createdAt(), result.createdAt());
      assertEquals(NOW, result.updatedAt());
    }
  }

  @Test
  void s30_redeliveringIsRefusedWhilePendingAndOnADisabledEndpoint() {
    var pending = new FakeWebhookDeliveries();
    pending.give(D, "pending", NOW);
    assertEquals(
        WebhookOperationException.Code.DELIVERY_PENDING,
        codeOf(
            () ->
                manage(withActiveEndpoint(), pending, FakeWebhookSecrets.KEYED)
                    .redeliver(OWNER, W, D)));
    assertNull(pending.requeued);
    var disabled = new FakeWebhookEndpoints();
    disabled.stored.add(endpoint("disabled"));
    var exhausted = new FakeWebhookDeliveries();
    exhausted.give(D, "exhausted", NOW);
    assertEquals(
        WebhookOperationException.Code.DISABLED,
        codeOf(
            () -> manage(disabled, exhausted, FakeWebhookSecrets.KEYED).redeliver(OWNER, W, D)));
    assertNull(exhausted.requeued);
  }

  @Test
  void s30_aDeliveryOfAnotherWebhookIsNotFound() {
    var deliveries = new FakeWebhookDeliveries();
    deliveries.give(D, "succeeded", NOW);
    assertEquals(
        WebhookOperationException.Code.NOT_FOUND,
        codeOf(
            () ->
                manage(withActiveEndpoint(), deliveries, FakeWebhookSecrets.KEYED)
                    .redeliver(OWNER, W, UNKNOWN)));
    assertNull(deliveries.requeued);
  }
}
