package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookCursor;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EnqueueWebhookDeliveriesTest {
  private static final Instant CREATED = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final UUID W = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID NIL = new UUID(0L, 0L);
  private static final UUID A = UUID.fromString("aaaaaaaa-1111-4111-8111-111111111111");
  private static final UUID B = UUID.fromString("bbbbbbbb-1111-4111-8111-111111111111");
  private static final String OWNER = "owner-a";

  private static WebhookEndpoint endpoint(List<String> types) {
    return new WebhookEndpoint(
        W, "https://example.com/h", "", types, "active", null, null, CREATED, CREATED);
  }

  private record Enqueued(UUID eventId, String eventType, String body, WebhookCursor cursor) {}

  private static final class FakeOutbox implements WebhookOutbox {
    final List<OutboxCandidate> candidates = new ArrayList<>();
    final List<Enqueued> enqueued = new ArrayList<>();
    final List<WebhookCursor> skipped = new ArrayList<>();
    final List<String> audited = new ArrayList<>();
    List<ReadyEndpoint> ready = List.of();

    @Override
    public List<ReadyEndpoint> readyEndpoints() {
      return ready;
    }

    @Override
    public List<OutboxCandidate> after(String owner, WebhookCursor cursor, Instant horizon) {
      return candidates.stream()
          .filter(
              candidate ->
                  candidate.occurredAt().isBefore(horizon)
                      || candidate.occurredAt().equals(horizon))
          .filter(candidate -> cursor.precedes(candidate.occurredAt(), candidate.eventId()))
          // Mirrors PostgreSQL: instants first, then uuid as unsigned bytes.
          .sorted(
              java.util.Comparator.comparing(OutboxCandidate::occurredAt)
                  .thenComparing(OutboxCandidate::eventId, WebhookCursor::compareUnsigned))
          .toList();
    }

    @Override
    public void enqueue(UUID endpointId, OutboxCandidate candidate, UUID deliveryId, Instant now) {
      enqueued.add(
          new Enqueued(
              candidate.eventId(),
              candidate.eventType(),
              candidate.payload(),
              new WebhookCursor(candidate.occurredAt(), candidate.eventId())));
    }

    @Override
    public void skip(UUID endpointId, WebhookCursor cursor, String auditCode) {
      skipped.add(cursor);
      if (auditCode != null) audited.add(auditCode);
    }
  }

  private static OutboxCandidate candidate(
      UUID eventId, String type, Instant occurredAt, String status, int schemaVersion) {
    return new OutboxCandidate(
        eventId, type, occurredAt, status, schemaVersion, "{\"name\":\"x\"}", true);
  }

  /** Collects the discards the enqueuer audited. */
  private static final class FakeAudit implements WebhookAudit {
    final List<String> discards = new ArrayList<>();

    @Override
    public void attempt(UUID endpointId, UUID eventId, String status, String errorClass) {}

    @Override
    public void discarded(UUID endpointId, UUID eventId, String code) {
      discards.add(eventId + " " + code);
    }

    @Override
    public void workerError(String code) {}
  }

  private static FakeAudit lastAudit;

  private static EnqueueWebhookDeliveries enqueuer(FakeOutbox outbox, Instant now) {
    lastAudit = new FakeAudit();
    return new EnqueueWebhookDeliveries(outbox, lastAudit, Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void s21_s35_aDiscardedRowIsAuditedWithItsEventAndItsCode() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", CREATED.plusSeconds(1), "pending", 2));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertEquals(List.of(A + " UNSUPPORTED_EVENT"), lastAudit.discards);
  }

  @Test
  void s21_aBlockedRowIsNotAudited() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", CREATED.plusSeconds(1), "blocked", 1));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertTrue(lastAudit.discards.isEmpty(), "a blocked row is skipped, not a failure to report");
  }

  private static void ready(FakeOutbox outbox, WebhookEndpoint endpoint, WebhookCursor cursor) {
    outbox.ready = List.of(new ReadyEndpoint(endpoint, OWNER, cursor));
  }

  @Test
  void s18_onlyASubscribedTypeOfTheOwnerIsEnqueuedAndTheCursorFollowsIt() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("TaskCreated.v1")), new WebhookCursor(CREATED, NIL));
    var wanted = CREATED.plusSeconds(1);
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", wanted, "pending", 1));
    outbox.candidates.add(candidate(B, "TaskCreated.v1", wanted.plusSeconds(1), "pending", 1));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertEquals(1, outbox.enqueued.size());
    assertEquals(B, outbox.enqueued.getFirst().eventId());
    assertEquals("TaskCreated.v1", outbox.enqueued.getFirst().eventType());
    assertEquals("{\"name\":\"x\"}", outbox.enqueued.getFirst().body());
    assertEquals(new WebhookCursor(wanted.plusSeconds(1), B), outbox.enqueued.getFirst().cursor());
  }

  @ParameterizedTest
  @CsvSource({
    "2026-09-08T09:59:59.999999Z,false",
    "2026-09-08T10:00:00.000000Z,true",
    "2026-09-08T10:00:06.000000Z,false",
    "2026-09-08T10:00:05.000000Z,true"
  })
  void s19_theCursorRespectsCreationAndTheFiveSecondGraceWindow(
      String occurredAt, boolean enqueued) {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(
        candidate(A, "ProjectCreated.v1", Instant.parse(occurredAt), "pending", 1));

    enqueuer(outbox, Instant.parse("2026-09-08T10:00:10.000000Z")).runCycle();

    assertEquals(enqueued, !outbox.enqueued.isEmpty());
    if (!enqueued) assertTrue(outbox.skipped.isEmpty(), "the cursor must not move");
  }

  @Test
  void s19_twoEventsSharingAnInstantAreOrderedByTheSmallerEventId() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    var shared = CREATED.plusSeconds(1);
    outbox.candidates.add(candidate(B, "ProjectCreated.v1", shared, "pending", 1));
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", shared, "pending", 1));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertEquals(A, outbox.enqueued.getFirst().eventId());
  }

  @Test
  void s21_aBlockedRowIsSkippedWithoutAuditAndTheCursorKeepsMoving() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", CREATED.plusSeconds(1), "blocked", 1));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertTrue(outbox.enqueued.isEmpty());
    assertEquals(List.of(new WebhookCursor(CREATED.plusSeconds(1), A)), outbox.skipped);
    assertTrue(outbox.audited.isEmpty());
  }

  @Test
  void s21_anUnsupportedVersionIsAuditedAndTheCursorAdvancesPastIt() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(candidate(A, "ProjectCreated.v1", CREATED.plusSeconds(1), "pending", 2));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertTrue(outbox.enqueued.isEmpty());
    assertEquals(List.of("UNSUPPORTED_EVENT"), outbox.audited);
    assertEquals(List.of(new WebhookCursor(CREATED.plusSeconds(1), A)), outbox.skipped);
  }

  @Test
  void s21_anInvalidPayloadIsAuditedAndTheCursorAdvancesPastIt() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("ProjectCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(
        new OutboxCandidate(
            A, "ProjectCreated.v1", CREATED.plusSeconds(1), "pending", 1, "{}", false));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertTrue(outbox.enqueued.isEmpty());
    assertEquals(List.of("INVALID_EVENT"), outbox.audited);
    assertEquals(List.of(new WebhookCursor(CREATED.plusSeconds(1), A)), outbox.skipped);
  }

  @Test
  void s20_atMostOneOutboxDeliveryIsEnqueuedPerEndpointAndCycle() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("TaskCreated.v1")), new WebhookCursor(CREATED, NIL));
    outbox.candidates.add(candidate(A, "TaskCreated.v1", CREATED.plusSeconds(1), "pending", 1));
    outbox.candidates.add(candidate(B, "TaskCreated.v1", CREATED.plusSeconds(2), "pending", 1));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertEquals(1, outbox.enqueued.size());
    assertEquals(A, outbox.enqueued.getFirst().eventId());
  }

  @Test
  void s18_anEndpointWithNothingNewEnqueuesNothingAndLeavesTheCursorAlone() {
    var outbox = new FakeOutbox();
    ready(outbox, endpoint(List.of("TaskCreated.v1")), new WebhookCursor(CREATED, NIL));

    enqueuer(outbox, CREATED.plusSeconds(60)).runCycle();

    assertTrue(outbox.enqueued.isEmpty());
    assertTrue(outbox.skipped.isEmpty());
  }
}
