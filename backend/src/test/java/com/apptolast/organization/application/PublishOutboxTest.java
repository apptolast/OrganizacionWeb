package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.OutboxMessage;
import com.apptolast.organization.domain.PublicationAttempt;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class PublishOutboxTest {
  @Test
  void taskStatus_s37_publishesValidTransitionWithOriginalEnvelope() throws Exception {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "TaskStatusChanged.v1");
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("fromStatus", "pending");
    payload.put("toStatus", "completed");
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "TaskStatusChanged.v1",
            1,
            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload),
            payload,
            0);
    var work = new Work(event);
    var sent = new ArrayList<OutboxMessage>();
    new PublishOutbox(
            work,
            delivered -> {
              sent.add(delivered);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).containsExactly(event);
    assertThat(sent.getFirst()).isSameAs(event);
    assertThat(work.persisted)
        .containsExactly(new PublicationAttempt(event.eventId(), "published", 1, NOW, null, null));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"valid", "type", "format", "same"})
  void subtask_s37_preservesParentIdentityAndInvalidClassification(String parentCase)
      throws Exception {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "SubtaskCreated.v1");
    payload.put("title", "Preparar material");
    var task = UUID.randomUUID().toString();
    payload.put("taskId", task);
    payload.put(
        "parentTaskId",
        switch (parentCase) {
          case "type" -> 42;
          case "format" -> "1-1-1-1-1";
          case "same" -> task;
          default -> UUID.randomUUID().toString();
        });
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "SubtaskCreated.v1",
            1,
            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload),
            payload,
            0);
    var work = new Work(event);
    var sent = new ArrayList<OutboxMessage>();
    new PublishOutbox(
            work,
            delivered -> {
              sent.add(delivered);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    if (parentCase.equals("valid")) {
      assertThat(sent).containsExactly(event);
      assertThat(sent.getFirst()).isSameAs(event);
      assertThat(work.persisted)
          .containsExactly(
              new PublicationAttempt(event.eventId(), "published", 1, NOW, null, null));
    } else {
      assertThat(sent).isEmpty();
      assertThat(work.persisted)
          .containsExactly(
              new PublicationAttempt(event.eventId(), "blocked", 0, NOW, null, "INVALID_EVENT"));
    }
  }

  static OutboxMessage blockMessage() {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "BlockPlanned.v1");
    payload.put("blockId", UUID.randomUUID().toString());
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("startAt", "2030-01-07T10:00:00Z");
    payload.put("endAt", "2030-01-07T11:00:00Z");
    payload.put("zoneId", "Historical/Removed");
    payload.put("durationMinutes", 60);
    return new OutboxMessage(
        source.eventId(),
        source.aggregateId(),
        source.ownerId(),
        source.occurredAt(),
        "BlockPlanned.v1",
        1,
        source.json(),
        payload,
        0);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {1, 60, 1440})
  void block_s36_s37_publishesHistoricalZoneWithoutCatalogResolution(int minutes) throws Exception {
    var source = blockMessage();
    var payload = new HashMap<>(source.payload());
    payload.put("durationMinutes", minutes);
    payload.put(
        "endAt",
        Instant.parse((String) payload.get("startAt")).plusSeconds(minutes * 60L).toString());
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            source.type(),
            source.schemaVersion(),
            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload),
            payload,
            source.attempts());
    var work = new Work(event);
    var sent = new ArrayList<OutboxMessage>();
    new PublishOutbox(
            work,
            delivered -> {
              sent.add(delivered);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).containsExactly(event);
    assertThat(sent.getFirst()).isSameAs(event);
    assertThat(event.payload())
        .hasSize(12)
        .doesNotContainKeys("objective", "requestKey", "Idempotency-Key");
    assertThat(work.persisted)
        .containsExactly(new PublicationAttempt(event.eventId(), "published", 1, NOW, null, null));
  }

  static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "blockId",
        "taskId",
        "zoneEmpty",
        "zoneType",
        "startType",
        "startInvalid",
        "endInvalid",
        "nonIncreasing",
        "durationMismatch",
        "durationZero",
        "durationLong",
        "durationDecimal",
        "fractionalInstants",
        "yearZero",
        "year10000",
        "extra",
        "type",
        "version",
        "missing:eventId",
        "missing:aggregateId",
        "missing:ownerId",
        "missing:occurredAt",
        "missing:schemaVersion",
        "missing:type",
        "missing:blockId",
        "missing:taskId",
        "missing:startAt",
        "missing:endAt",
        "missing:zoneId",
        "missing:durationMinutes"
      })
  void block_s37_blocksInvalidEnvelopeWithoutPublishing(String defect) {
    var source = blockMessage();
    var body = new HashMap<>(source.payload());
    if (defect.startsWith("missing:")) body.remove(defect.substring(8));
    else
      switch (defect) {
        case "blockId", "taskId" -> body.put(defect, "1-1-1-1-1");
        case "zoneEmpty" -> body.put("zoneId", "  ");
        case "zoneType" -> body.put("zoneId", 1);
        case "startType" -> body.put("startAt", 1);
        case "startInvalid" -> body.put("startAt", "invalid");
        case "endInvalid" -> body.put("endAt", "invalid");
        case "nonIncreasing" -> body.put("endAt", body.get("startAt"));
        case "durationMismatch" -> body.put("durationMinutes", 59);
        case "durationZero" -> body.put("durationMinutes", 0);
        case "durationLong" -> {
          body.put("durationMinutes", 1441);
          body.put("endAt", "2030-01-08T10:01:00Z");
        }
        case "durationDecimal" -> body.put("durationMinutes", 60.0);
        case "fractionalInstants" -> {
          body.put("startAt", "2030-01-07T10:00:00.000001Z");
          body.put("endAt", "2030-01-07T11:00:00.000001Z");
        }
        case "yearZero" -> {
          body.put("startAt", "0000-01-07T10:00:00Z");
          body.put("endAt", "0000-01-07T11:00:00Z");
        }
        case "year10000" -> {
          body.put("startAt", "+10000-01-07T10:00:00Z");
          body.put("endAt", "+10000-01-07T11:00:00Z");
        }
        case "extra" -> body.put("objective", "private");
        default -> {}
      }
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            defect.equals("type") ? "BlockPlanned.v2" : source.type(),
            defect.equals("version") ? 2 : 1,
            source.json(),
            body,
            3);
    var work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              throw new AssertionError("Invalid block published");
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(
                event.eventId(),
                "blocked",
                3,
                NOW,
                null,
                defect.equals("type") || defect.equals("version")
                    ? "UNSUPPORTED_EVENT"
                    : "INVALID_EVENT"));
  }

  final List<PublicationAttempt> logs = new ArrayList<>();
  final List<String> workerErrors = new ArrayList<>();
  final PublicationAudit audit =
      new PublicationAudit() {
        public void event(PublicationAttempt attempt) {
          logs.add(attempt);
        }

        public void workerError(String code) {
          workerErrors.add(code);
        }
      };

  static OutboxMessage message(long attempts) {
    UUID id = UUID.randomUUID();
    UUID aggregate = UUID.randomUUID();
    Map<String, Object> body =
        Map.of(
            "eventId",
            id.toString(),
            "aggregateId",
            aggregate.toString(),
            "ownerId",
            "persona-a",
            "occurredAt",
            NOW.toString(),
            "schemaVersion",
            1,
            "name",
            "Idea",
            "type",
            "ProjectCreated.v1");
    return new OutboxMessage(
        id, aggregate, "persona-a", NOW, "ProjectCreated.v1", 1, "{stored-json}", body, attempts);
  }

  static final class Work implements OutboxWork {
    final Deque<OutboxMessage> waiting = new ArrayDeque<>();
    final List<PublicationAttempt> persisted = new ArrayList<>();

    Work(OutboxMessage... events) {
      waiting.addAll(List.of(events));
    }

    public Optional<PublicationAttempt> processNext(
        Instant now, Set<UUID> excluded, Function<OutboxMessage, PublicationAttempt> operation) {
      if (waiting.isEmpty()) return Optional.empty();
      PublicationAttempt attempt = operation.apply(waiting.remove());
      persisted.add(attempt);
      return Optional.of(attempt);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"BROKER_UNAVAILABLE", "BROKER_NACK", "UNROUTABLE", "CONFIRM_TIMEOUT"})
  void s5_preservesFailedAttemptWithStableCode(String code) {
    OutboxMessage original = message(0);
    Work work = new Work(original);
    new PublishOutbox(
            work, event -> DeliveryOutcome.valueOf(code), audit, Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(original.eventId(), "retry", 1, NOW, NOW.plusSeconds(1), code));
    assertThat(logs).isEqualTo(work.persisted);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"0,1", "1,2", "5,32", "6,60", "100,60"})
  void s7_boundsExponentialRetryWithoutOverflow(long previous, long delay) {
    OutboxMessage original = message(previous);
    Work work = new Work(original);
    new PublishOutbox(
            work,
            event -> DeliveryOutcome.BROKER_UNAVAILABLE,
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(
                original.eventId(),
                "retry",
                previous + 1,
                NOW,
                NOW.plusSeconds(delay),
                "BROKER_UNAVAILABLE"));
  }

  @Test
  void s17_limitsCycleToTwentyDistinctRecords() {
    Work work =
        new Work(
            java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> message(0))
                .toArray(OutboxMessage[]::new));
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).hasSize(20).doesNotHaveDuplicates();
    assertThat(work.waiting).hasSize(1);
    assertThat(logs).hasSize(20);
  }

  @Test
  void s5_neverRetriesSameEventWithinOneLongCycle() {
    OutboxMessage original = message(0);
    OutboxWork work =
        (now, excluded, operation) ->
            excluded.contains(original.eventId())
                ? Optional.empty()
                : Optional.of(operation.apply(original));
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.BROKER_UNAVAILABLE;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).containsExactly(original);
    assertThat(logs).hasSize(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"Other.v1,1", "ProjectCreated.v1,2"})
  void s15_blocksUnsupportedWithoutSendingAndContinues(String type, int version) {
    OutboxMessage original = message(0);
    OutboxMessage invalid =
        new OutboxMessage(
            original.eventId(),
            original.aggregateId(),
            original.ownerId(),
            original.occurredAt(),
            type,
            version,
            original.json(),
            original.payload(),
            0);
    OutboxMessage valid = message(0);
    Work work = new Work(invalid, valid);
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).containsExactly(valid);
    assertThat(work.persisted.getFirst())
        .isEqualTo(
            new PublicationAttempt(
                invalid.eventId(), "blocked", 0, NOW, null, "UNSUPPORTED_EVENT"));
    assertThat(work.persisted.getLast().outcome()).isEqualTo("published");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "eventId",
        "aggregateId",
        "ownerId",
        "occurredAt",
        "schemaVersion",
        "type",
        "nameType",
        "nameEmpty",
        "nameLong",
        "nameWhitespace",
        "description",
        "missing",
        "versionDecimal",
        "null"
      })
  void s15_blocksIncompatiblePayloadWithoutSending(String defect) {
    OutboxMessage original = message(3);
    Map<String, Object> body = new HashMap<>(original.payload());
    switch (defect) {
      case "nameType" -> body.put("name", 42);
      case "nameEmpty" -> body.put("name", "");
      case "nameLong" -> body.put("name", "a".repeat(121));
      case "nameWhitespace" -> body.put("name", " Idea ");
      case "description" -> body.put("description", "private");
      case "missing" -> body.remove("name");
      case "versionDecimal" -> body.put("schemaVersion", 1.0);
      case "null" -> body.put("ownerId", null);
      default -> body.put(defect, "incompatible");
    }
    OutboxMessage invalid =
        new OutboxMessage(
            original.eventId(),
            original.aggregateId(),
            original.ownerId(),
            original.occurredAt(),
            original.type(),
            1,
            original.json(),
            body,
            3);
    Work work = new Work(invalid);
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).isEmpty();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(invalid.eventId(), "blocked", 3, NOW, null, "INVALID_EVENT"));
  }

  @Test
  void s5_retryDelayUsesOneCompletionInstant() {
    Work work = new Work(message(0));
    Clock clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenReturn(NOW, NOW.plusMillis(20), NOW.plusMillis(21));
    new PublishOutbox(work, event -> DeliveryOutcome.CONFIRM_TIMEOUT, audit, clock).runCycle();
    PublicationAttempt attempt = work.persisted.getFirst();
    assertThat(attempt.completedAt()).isEqualTo(NOW.plusMillis(20));
    assertThat(attempt.nextAttemptAt()).isEqualTo(attempt.completedAt().plusSeconds(1));
  }

  @Test
  void s18_storageFailureNeverSendsAndReportsWorkerError() {
    OutboxWork work =
        (now, excluded, operation) -> {
          throw new StorageUnavailableException(new RuntimeException("private"));
        };
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).isEmpty();
    assertThat(logs).isEmpty();
    assertThat(workerErrors).containsExactly("STORAGE_UNAVAILABLE");
  }

  @Test
  void s21_topologyMismatchAbortsClaimWithoutEventResult() {
    Work work = new Work(message(0));
    new PublishOutbox(
            work,
            event -> {
              throw new TopologyMismatchException();
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted).isEmpty();
    assertThat(logs).isEmpty();
    assertThat(workerErrors).containsExactly("TOPOLOGY_MISMATCH");
  }

  @Test
  void eventSnapshotCannotBeChangedThroughCallerMap() {
    OutboxMessage original = message(0);
    Map<String, Object> mutable = new HashMap<>(original.payload());
    OutboxMessage snapshot =
        new OutboxMessage(
            original.eventId(),
            original.aggregateId(),
            original.ownerId(),
            original.occurredAt(),
            original.type(),
            1,
            original.json(),
            mutable,
            0);
    mutable.put("description", "private");
    assertThat(snapshot.validationCode()).isNull();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> snapshot.payload().put("ownerId", "other"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      value = DeliveryOutcome.class,
      names = {"ACCEPTED", "BROKER_UNAVAILABLE"})
  void publicationTimesKeepPostgresPrecision(DeliveryOutcome outcome) {
    Work work = new Work(message(0));
    new PublishOutbox(
            work, event -> outcome, audit, Clock.fixed(NOW.plusNanos(123456789), ZoneOffset.UTC))
        .runCycle();
    var attempt = work.persisted.getFirst();
    assertThat(attempt.completedAt()).isEqualTo(NOW.plusNanos(123456000));
    if (outcome != DeliveryOutcome.ACCEPTED)
      assertThat(attempt.nextAttemptAt()).isEqualTo(attempt.completedAt().plusSeconds(1));
  }

  @Test
  void shutdownInterruptionDoesNotClaimAnotherEvent() {
    Work work = new Work(message(0), message(0));
    List<OutboxMessage> sent = new ArrayList<>();
    try {
      new PublishOutbox(
              work,
              event -> {
                sent.add(event);
                Thread.currentThread().interrupt();
                return DeliveryOutcome.BROKER_UNAVAILABLE;
              },
              audit,
              Clock.fixed(NOW, ZoneOffset.UTC))
          .runCycle();
      assertThat(sent).hasSize(1);
      assertThat(work.waiting).hasSize(1);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void s1_recordsPublishedOnlyAfterAcceptedOriginalMessage() {
    OutboxMessage original = message(0);
    Work work = new Work(original);
    List<OutboxMessage> sent = new ArrayList<>();
    PublishOutbox publisher =
        new PublishOutbox(
            work,
            event -> {
              sent.add(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC));
    publisher.runCycle();
    assertThat(sent).containsExactly(original);
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(original.eventId(), "published", 1, NOW, null, null));
    assertThat(logs).isEqualTo(work.persisted);
    assertThat(workerErrors).isEmpty();
  }

  @Test
  void s1_acceptsMaximumUnicodeNameAndPreservesBrokerEnvelope() {
    OutboxMessage source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.put("name", "😀".repeat(120));
    OutboxMessage event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            source.type(),
            1,
            source.json(),
            payload,
            0);
    Work work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              assertThat(delivered.json()).isEqualTo("{stored-json}");
              assertThat(delivered.schemaVersion()).isEqualTo(1);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .singleElement()
        .extracting(PublicationAttempt::outcome)
        .isEqualTo("published");
  }

  @Test
  void s15_blocksValidTimestampThatDiffersFromStoredColumn() {
    OutboxMessage source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.put("occurredAt", NOW.plusSeconds(1).toString());
    Work work =
        new Work(
            new OutboxMessage(
                source.eventId(),
                source.aggregateId(),
                source.ownerId(),
                source.occurredAt(),
                source.type(),
                1,
                source.json(),
                payload,
                0));
    new PublishOutbox(
            work,
            event -> {
              throw new AssertionError("Invalid event sent");
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .singleElement()
        .extracting(PublicationAttempt::code)
        .isEqualTo("INVALID_EVENT");
  }

  @Test
  void edit_s15_publishesUpdatedWithOriginalEnvelope() {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.put("type", "ProjectUpdated.v1");
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "ProjectUpdated.v1",
            1,
            source.json(),
            payload,
            0);
    var work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              assertThat(delivered).isSameAs(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(new PublicationAttempt(event.eventId(), "published", 1, NOW, null, null));
  }

  @Test
  void states_s13_publishesExactStatusEnvelope() {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "ProjectStatusChanged.v1");
    payload.put("fromStatus", "idea");
    payload.put("toStatus", "active");
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "ProjectStatusChanged.v1",
            1,
            source.json(),
            payload,
            0);
    var work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              assertThat(delivered).isSameAs(event);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(new PublicationAttempt(event.eventId(), "published", 1, NOW, null, null));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"fromType", "toType", "same", "impossible", "unknown", "extra", "missing"})
  void states_s13_blocksIncompatibleStatusPayload(String defect) {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "ProjectStatusChanged.v1");
    payload.put("fromStatus", "idea");
    payload.put("toStatus", "active");
    switch (defect) {
      case "fromType" -> payload.put("fromStatus", 1);
      case "toType" -> payload.put("toStatus", false);
      case "same" -> payload.put("toStatus", "idea");
      case "impossible" -> payload.put("toStatus", "paused");
      case "unknown" -> payload.put("fromStatus", "other");
      case "extra" -> payload.put("name", "private");
      default -> payload.remove("fromStatus");
    }
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "ProjectStatusChanged.v1",
            1,
            source.json(),
            payload,
            0);
    var work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              throw new AssertionError("Invalid event sent");
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .singleElement()
        .extracting(PublicationAttempt::code)
        .isEqualTo("INVALID_EVENT");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "type",
        "version",
        "extra",
        "taskPartial",
        "taskType",
        "fromType",
        "toType",
        "fromUnknown",
        "toUnknown",
        "same",
        "missing:eventId",
        "missing:aggregateId",
        "missing:ownerId",
        "missing:occurredAt",
        "missing:schemaVersion",
        "missing:type",
        "missing:taskId",
        "missing:fromStatus",
        "missing:toStatus"
      })
  void taskStatus_s32_blocksEveryIncompatibleEnvelopeWithoutSending(String defect)
      throws Exception {
    var source = message(0);
    var payload = new HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "TaskStatusChanged.v1");
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("fromStatus", "pending");
    payload.put("toStatus", "completed");
    if (defect.startsWith("missing:")) payload.remove(defect.substring(8));
    else
      switch (defect) {
        case "extra" -> payload.put("title", "private");
        case "taskPartial" -> payload.put("taskId", "1-1-1-1-1");
        case "taskType" -> payload.put("taskId", 42);
        case "fromType" -> payload.put("fromStatus", 42);
        case "toType" -> payload.put("toStatus", 42);
        case "fromUnknown" -> payload.put("fromStatus", "unknown");
        case "toUnknown" -> payload.put("toStatus", "unknown");
        case "same" -> payload.put("toStatus", "pending");
        default -> {}
      }
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            defect.equals("type") ? "TaskStatusChanged.v2" : "TaskStatusChanged.v1",
            defect.equals("version") ? 2 : 1,
            new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload),
            payload,
            0);
    var work = new Work(event);
    new PublishOutbox(
            work,
            delivered -> {
              throw new AssertionError("Invalid status event sent");
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(
                event.eventId(),
                "blocked",
                0,
                NOW,
                null,
                defect.equals("type") || defect.equals("version")
                    ? "UNSUPPORTED_EVENT"
                    : "INVALID_EVENT"));
  }
}
