package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ExtendWorkSessionTest {
  @Test
  void s1_extendsTheEffectiveEndWithoutChangingStateOrWork() {
    var id = UUID.randomUUID();
    var start =
        new SessionStart(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00.123456Z"),
            60,
            Instant.parse("2026-09-07T11:00:00.123456Z"),
            "UTC");
    var state = new WorkSessionState(start, "running", 3, start.startedAt(), 0, start.startedAt());
    var context = new WorkSessionEnd(state, start.plannedEndAt(), state.changedAt());
    var key = UUID.randomUUID();
    var writes = new ArrayList<WorkSessionExtensionTransition>();
    WorkSessionExtending store =
        (owner, session, requestKey, expected, minutes, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(session).isEqualTo(id);
          assertThat(requestKey).isEqualTo(key);
          assertThat(expected).isEqualTo(new WorkSessionRevision(id, 3));
          assertThat(minutes).isEqualTo(1);
          var change = operation.apply(context);
          writes.add(change);
          return new WorkSessionTransitionConfirmation(change.receipt(), false);
        };
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T10:30:00.123457999Z"));
    ExtendWorkSessionUseCase useCase = new ExtendWorkSession(store, clock);
    var result = useCase.extend("owner", id, key, new WorkSessionRevision(id, 3), 1);
    var receipt = result.receipt();
    var at = Instant.parse("2026-09-07T10:30:00.123457Z");
    var end = Instant.parse("2026-09-07T11:01:00.123456Z");
    assertThat(result.replayed()).isFalse();
    assertThat(receipt.id()).isNotNull();
    assertThat(receipt.sessionId()).isEqualTo(id);
    assertThat(receipt.action()).isEqualTo("EXTEND");
    assertThat(receipt.occurredAt()).isEqualTo(at);
    assertThat(receipt.before()).isSameAs(state);
    assertThat(receipt.after())
        .isEqualTo(
            new WorkSessionState(start, "running", 4, state.changedAt(), 0, state.runningSince()));
    assertThat(receipt.closure()).isNull();
    assertThat(receipt.extension())
        .isEqualTo(new WorkSessionExtension(1, start.plannedEndAt(), end));
    assertThat(writes).hasSize(1);
    var event = writes.getFirst().event();
    assertThat(event.eventId()).isNotNull().isNotEqualTo(receipt.id()).isNotEqualTo(id);
    assertThat(event.aggregateId()).isEqualTo(id);
    assertThat(event.ownerId()).isEqualTo("owner");
    assertThat(event.occurredAt()).isEqualTo(at);
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.type()).isEqualTo("WorkSessionExtended.v1");
    assertThat(event.revision()).isEqualTo("4");
    assertThat(event.additionalMinutes()).isEqualTo(1);
    assertThat(event.previousEndAt()).isEqualTo(start.plannedEndAt());
    assertThat(event.effectiveEndAt()).isEqualTo(end);
    assertThat(event.status()).isEqualTo("running");
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }

  @Test
  void s2_extendsAPausedExpiredSessionFromTheConfirmationClock() {
    var at = Instant.parse("2026-09-07T11:30:00.000002Z");
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00.000001Z"),
            60,
            Instant.parse("2026-09-07T11:00:00.000001Z"),
            "UTC");
    var state = new WorkSessionState(start, "paused", 2, start.startedAt(), 0, null);
    WorkSessionExtending store =
        (owner, session, key, revision, minutes, operation) ->
            new WorkSessionTransitionConfirmation(
                operation
                    .apply(new WorkSessionEnd(state, start.plannedEndAt(), state.changedAt()))
                    .receipt(),
                false);
    var result =
        new ExtendWorkSession(store, Clock.fixed(at, ZoneOffset.UTC))
            .extend(
                "owner",
                start.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(start.id(), 2),
                1440);
    assertThat(result.receipt().extension().effectiveEndAt())
        .isEqualTo(Instant.parse("2026-09-08T11:30:00.000002Z"));
    assertThat(result.receipt().after())
        .isEqualTo(new WorkSessionState(start, "paused", 3, state.changedAt(), 0, null));
  }

  @Test
  void s9_rejectsAStaleRevisionBeforeConsultingTheClock() {
    var state = paused();
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(at(state), clock)
                    .extend(
                        "owner",
                        state.session().id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(state.session().id(), 1),
                        1))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("PRECONDITION_FAILED"));
    verifyNoInteractions(clock);
  }

  private static WorkSessionState paused() {
    var at = Instant.parse("2026-09-07T10:00:00Z");
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            at,
            25,
            at.plusSeconds(1500),
            "UTC");
    return new WorkSessionState(start, "paused", 2, at, 0, null);
  }

  private static WorkSessionExtending at(WorkSessionState state) {
    return (owner, session, key, revision, minutes, operation) ->
        new WorkSessionTransitionConfirmation(
            operation
                .apply(new WorkSessionEnd(state, state.session().plannedEndAt(), state.changedAt()))
                .receipt(),
            false);
  }

  @Test
  void s4_rejectsZeroMinutesBeforeEnteringTheStore() {
    var store = mock(WorkSessionExtending.class);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(store, clock)
                    .extend(
                        "owner",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(UUID.randomUUID(), 1),
                        0))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .containsExactly(
                        new FieldError(
                            "additionalMinutes",
                            "OUT_OF_RANGE",
                            "Debe estar entre 1 y 1440 minutos.")));
    verifyNoInteractions(store, clock);
  }

  @Test
  void s4_rejectsMoreThanOneDayBeforeEnteringTheStore() {
    var store = mock(WorkSessionExtending.class);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(store, clock)
                    .extend(
                        "owner",
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(UUID.randomUUID(), 1),
                        1441))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .containsExactly(
                        new FieldError(
                            "additionalMinutes",
                            "OUT_OF_RANGE",
                            "Debe estar entre 1 y 1440 minutos.")));
    verifyNoInteractions(store, clock);
  }

  @Test
  void s10_rejectsAClockEarlierThanTheLastExtension() {
    var state = paused();
    var last = state.changedAt().plusSeconds(100);
    WorkSessionExtending store =
        (owner, session, key, revision, minutes, operation) ->
            new WorkSessionTransitionConfirmation(
                operation
                    .apply(
                        new WorkSessionEnd(
                            state, state.session().plannedEndAt().plusSeconds(60), last))
                    .receipt(),
                false);
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(store, Clock.fixed(last.minusNanos(1000), ZoneOffset.UTC))
                    .extend(
                        "owner",
                        state.session().id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(state.session().id(), 2),
                        1))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s12_rejectsAClockInYearTenThousand() {
    var state = paused();
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(
                        at(state),
                        Clock.fixed(Instant.parse("+10000-01-01T00:00:00Z"), ZoneOffset.UTC))
                    .extend(
                        "owner",
                        state.session().id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(state.session().id(), 2),
                        1))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s12_rejectsAnEndBeyondYearNineThousandNineHundredNinetyNine() {
    var instant = Instant.parse("9999-12-31T23:58:00Z");
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            instant,
            1,
            instant.plusSeconds(60),
            "UTC");
    var state = new WorkSessionState(start, "running", 1, instant, 0, instant);
    assertThatThrownBy(
            () ->
                new ExtendWorkSession(at(state), Clock.fixed(instant, ZoneOffset.UTC))
                    .extend(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 1),
                        1))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }
}
