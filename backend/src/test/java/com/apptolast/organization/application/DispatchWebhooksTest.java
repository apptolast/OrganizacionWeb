package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookAttempt;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DispatchWebhooksTest {
  private static final Instant T = Instant.parse("2026-09-08T12:00:00.000000Z");
  private static final Clock CLOCK = Clock.fixed(T, ZoneOffset.UTC);
  private static final UUID W = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final String OWNER = "owner-a";

  private static WebhookEndpoint active() {
    return new WebhookEndpoint(
        W, "https://example.com/h", "", List.of("TaskCreated.v1"), "active", null, null, T, T);
  }

  private static WebhookDelivery pending(int attempt) {
    var id = UUID.randomUUID();
    return new WebhookDelivery(
        id, id, "TaskCreated.v1", "pending", attempt, null, null, null, T, T, T);
  }

  /** Records what the worker claimed, sent and wrote back. */
  private static final class FakeWork implements WebhookWork {
    final Deque<ClaimedDelivery> claimable = new ArrayDeque<>();
    final List<WebhookDelivery> recorded = new ArrayList<>();
    final List<WebhookEndpoint> disabled = new ArrayList<>();
    int claims;

    @Override
    public Optional<ClaimedDelivery> claimNext(Instant now) {
      claims++;
      return Optional.ofNullable(claimable.poll());
    }

    @Override
    public void record(ClaimedDelivery claimed, WebhookDelivery result, WebhookEndpoint endpoint) {
      recorded.add(result);
      if (endpoint != null) disabled.add(endpoint);
    }
  }

  private static final class FakeSender implements WebhookSender {
    final List<String> sentEventIds = new ArrayList<>();
    WebhookAttempt outcome = WebhookAttempt.http(200, 5);

    @Override
    public WebhookAttempt send(String url, String secret, String eventId, String body) {
      sentEventIds.add(eventId);
      return outcome;
    }
  }

  private static ClaimedDelivery claim(WebhookDelivery delivery) {
    return new ClaimedDelivery(active(), OWNER, delivery, "{}", "whsec_x");
  }

  @Test
  void s20_aCycleSendsEachClaimedDeliveryInOrderAndRecordsTheOutcome() {
    var work = new FakeWork();
    var sender = new FakeSender();
    var first = pending(0);
    var second = pending(0);
    work.claimable.add(claim(first));
    work.claimable.add(claim(second));

    new DispatchWebhooks(work, sender, CLOCK).runCycle();

    assertEquals(
        List.of(first.eventId().toString(), second.eventId().toString()), sender.sentEventIds);
    assertEquals(List.of("succeeded", "succeeded"), work.recorded.stream()
        .map(WebhookDelivery::status)
        .toList());
    assertEquals(List.of(1, 1), work.recorded.stream().map(WebhookDelivery::attempt).toList());
    assertTrue(work.disabled.isEmpty());
  }

  @Test
  void s32_aCycleStopsAtTwentyDeliveriesAndLeavesTheRestForTheNextOne() {
    var work = new FakeWork();
    var sender = new FakeSender();
    for (var index = 0; index < 25; index++) work.claimable.add(claim(pending(0)));

    var dispatch = new DispatchWebhooks(work, sender, CLOCK);
    dispatch.runCycle();
    assertEquals(20, sender.sentEventIds.size());

    dispatch.runCycle();
    assertEquals(25, sender.sentEventIds.size());
  }

  @Test
  void s32_anEmptyQueueCostsASingleClaimAndNoSend() {
    var work = new FakeWork();
    var sender = new FakeSender();

    new DispatchWebhooks(work, sender, CLOCK).runCycle();

    assertEquals(1, work.claims);
    assertTrue(sender.sentEventIds.isEmpty());
    assertTrue(work.recorded.isEmpty());
  }

  @Test
  void s27_theSixthFailureExhaustsTheDeliveryAndDisablesTheEndpointTogether() {
    var work = new FakeWork();
    var sender = new FakeSender();
    sender.outcome = WebhookAttempt.http(500, 9);
    work.claimable.add(claim(pending(5)));

    new DispatchWebhooks(work, sender, CLOCK).runCycle();

    assertEquals("exhausted", work.recorded.getFirst().status());
    assertEquals(6, work.recorded.getFirst().attempt());
    assertNull(work.recorded.getFirst().nextAttemptAt());
    var endpoint = work.disabled.getFirst();
    assertEquals("disabled", endpoint.status());
    assertEquals("DELIVERY_EXHAUSTED", endpoint.disabledReason());
    assertEquals(T, endpoint.disabledAt());
  }

  @Test
  void s24_aFailureShortOfExhaustionRecordsNoEndpointChange() {
    var work = new FakeWork();
    var sender = new FakeSender();
    sender.outcome = WebhookAttempt.http(500, 9);
    work.claimable.add(claim(pending(0)));

    new DispatchWebhooks(work, sender, CLOCK).runCycle();

    assertEquals("pending", work.recorded.getFirst().status());
    assertTrue(work.disabled.isEmpty());
  }

  @Test
  void s16_theSenderReceivesTheStoredBodySecretAndUrlOfItsOwnEndpoint() {
    var work = new FakeWork();
    var seen = new ArrayList<String>();
    WebhookSender sender =
        (url, secret, eventId, body) -> {
          seen.add(url);
          seen.add(secret);
          seen.add(body);
          return WebhookAttempt.http(200, 1);
        };
    var delivery = pending(0);
    work.claimable.add(new ClaimedDelivery(active(), OWNER, delivery, "{\"a\":1}", "whsec_real"));

    new DispatchWebhooks(work, sender, CLOCK).runCycle();

    assertEquals(List.of("https://example.com/h", "whsec_real", "{\"a\":1}"), seen);
  }
}
