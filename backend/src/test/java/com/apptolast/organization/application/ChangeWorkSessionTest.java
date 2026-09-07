package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import com.apptolast.organization.domain.WorkSessionState;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeWorkSessionTest {
  @Test
  void s6_pauseAcrossDstCountsInstantsAndPreservesTheFixedEnd() {
    var at = Instant.parse("2026-10-25T00:59:59.999999Z");
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            at,
            25,
            at.plusSeconds(1500),
            "Europe/Madrid");
    var before = new WorkSessionState(start, "running", 1, at, 0, at);
    var result =
        invoke(
            before,
            1,
            Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), java.time.ZoneOffset.UTC),
            true);
    assertThat(result.receipt().after().workedMicroseconds()).isEqualTo(86400000001L);
    assertThat(result.receipt().after().session()).isSameAs(start);
  }

  @Test
  void s14_resumeRejectsRunningBeforeClock() {
    var before = state("running", 1, 0);
    var clock = mock(Clock.class);
    assertThatThrownBy(() -> invoke(before, 1, clock, false))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_STATE_CONFLICT"));
    verifyNoInteractions(clock);
  }

  @Test
  void s15_penultimateRevisionCanReachTheBigintMaximum() {
    var before = state("running", Long.MAX_VALUE - 1, 0);
    var result =
        invoke(
            before,
            Long.MAX_VALUE - 1,
            Clock.fixed(before.changedAt(), java.time.ZoneOffset.UTC),
            true);
    assertThat(result.receipt().after().revision()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void s5_accumulatesFractionalIntervalsWithoutRounding() {
    var before = state("running", 1, 0);
    var first =
        invoke(
                before,
                1,
                Clock.fixed(before.changedAt().plusNanos(1000), java.time.ZoneOffset.UTC),
                true)
            .receipt()
            .after();
    var resumed =
        invoke(
                first,
                2,
                Clock.fixed(before.changedAt().plusSeconds(1), java.time.ZoneOffset.UTC),
                false)
            .receipt()
            .after();
    var second =
        invoke(
                resumed,
                3,
                Clock.fixed(
                    before.changedAt().plusSeconds(61).minusNanos(1000), java.time.ZoneOffset.UTC),
                true)
            .receipt()
            .after();
    assertThat(second.workedMicroseconds()).isEqualTo(60000000);
  }

  @Test
  void s28_fullUtcRangeRemainsExactBeyondNumberAndNanoseconds() {
    var at = Instant.parse("0001-01-01T00:00:00Z");
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            at,
            25,
            at.plusSeconds(1500),
            "UTC");
    var before = new WorkSessionState(start, "running", 1, at, 0, at);
    var result =
        invoke(
            before,
            1,
            Clock.fixed(Instant.parse("9999-12-31T23:59:59.999999Z"), java.time.ZoneOffset.UTC),
            true);
    assertThat(result.receipt().after().workedMicroseconds()).isEqualTo(315537897599999999L);
    assertThat(result.receipt().after().session()).isSameAs(start);
  }

  @Test
  void s4_resumeInTheSameMicrosecondAddsZero() {
    var before = state("paused", 2, 0);
    var result =
        invoke(before, 2, Clock.fixed(before.changedAt(), java.time.ZoneOffset.UTC), false);
    assertThat(result.receipt().after().workedMicroseconds()).isZero();
    assertThat(result.receipt().after().runningSince()).isEqualTo(before.changedAt());
    assertThat(result.receipt().after().revision()).isEqualTo(3);
  }

  @Test
  void s4_pauseInTheSameMicrosecondAddsZero() {
    var before = state("running", 1, 0);
    var result = invoke(before, 1, Clock.fixed(before.changedAt(), java.time.ZoneOffset.UTC), true);
    assertThat(result.receipt().after().workedMicroseconds()).isZero();
    assertThat(result.receipt().after().revision()).isEqualTo(2);
  }

  @Test
  void s17_receiptIntentIncludesSessionIdentity() {
    var before = state("running", 1, 0);
    var receipt =
        invoke(before, 1, Clock.fixed(before.changedAt(), java.time.ZoneOffset.UTC), true)
            .receipt();
    assertThatThrownBy(() -> receipt.requireIntent(UUID.randomUUID(), "PAUSE", 1))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
  }

  @Test
  void s7_rejectsYearTenThousand() {
    var state = state("paused", 2, 0);
    var clock = Clock.fixed(Instant.parse("+10000-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
    assertThatThrownBy(() -> invoke(state, 2, clock, false))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s7_rejectsBackwardClockInsteadOfClamping() {
    var state = state("running", 1, 0);
    var clock = Clock.fixed(state.changedAt().minusNanos(1000), java.time.ZoneOffset.UTC);
    assertThatThrownBy(() -> invoke(state, 1, clock, true))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s15_exhaustedRevisionPrecedesClock() {
    var state = state("running", Long.MAX_VALUE, 0);
    var clock = mock(Clock.class);
    assertThatThrownBy(() -> invoke(state, Long.MAX_VALUE, clock, true))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_REVISION_EXHAUSTED"));
    verifyNoInteractions(clock);
  }

  @Test
  void s14_pauseRejectsPausedBeforeClock() {
    var state = state("paused", 2, 0);
    var clock = mock(Clock.class);
    assertThatThrownBy(() -> invoke(state, 2, clock, true))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_STATE_CONFLICT"));
    verifyNoInteractions(clock);
  }

  private WorkSessionState state(String status, long revision, long worked) {
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
    return new WorkSessionState(
        start, status, revision, at, worked, status.equals("running") ? at : null);
  }

  private WorkSessionTransitionConfirmation invoke(
      WorkSessionState state, long expected, Clock clock, boolean pause) {
    WorkSessionChanging store =
        (owner, session, key, action, token, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(state).receipt(), false);
    var useCase = new ChangeWorkSession(store, clock);
    var token = new WorkSessionRevision(state.session().id(), expected);
    return pause
        ? useCase.pause("owner", state.session().id(), UUID.randomUUID(), token)
        : useCase.resume("owner", state.session().id(), UUID.randomUUID(), token);
  }

  @Test
  void s12_rejectsStaleRevisionBeforeStateAndClock() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00Z"),
            25,
            Instant.parse("2026-09-07T10:25:00Z"),
            "UTC");
    var before = new WorkSessionState(start, "paused", 2, start.startedAt(), 0, null);
    WorkSessionChanging store =
        (owner, session, key, action, expected, notes, operation) -> {
          operation.apply(before);
          throw new AssertionError("No mutation should be produced");
        };
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .pause(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 1)))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("PRECONDITION_FAILED"));
    verifyNoInteractions(clock);
  }

  @Test
  void s12_preservesTokenIdentityForTheTransactionalOwnershipCheck() {
    var session = UUID.randomUUID();
    var token = new WorkSessionRevision(UUID.randomUUID(), 1);
    var store = mock(WorkSessionChanging.class);
    var clock = mock(Clock.class);
    new ChangeWorkSession(store, clock).pause("owner", session, UUID.randomUUID(), token);
    verify(store)
        .commit(
            org.mockito.ArgumentMatchers.eq("owner"),
            org.mockito.ArgumentMatchers.eq(session),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("PAUSE"),
            org.mockito.ArgumentMatchers.same(token),
            org.mockito.ArgumentMatchers.any());
    verifyNoInteractions(clock);
  }

  @Test
  void s3_resumePreservesAccumulatedWorkAndFixedEnd() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00.123456Z"),
            25,
            Instant.parse("2026-09-07T10:25:00.123456Z"),
            "Europe/Madrid");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T10:00:01.123457Z"), 1000001, null);
    var writes = new ArrayList<WorkSessionTransition>();
    WorkSessionChanging store =
        (owner, session, key, action, expected, notes, operation) -> {
          assertThat(action).isEqualTo("RESUME");
          var write = operation.apply(before);
          writes.add(write);
          return new WorkSessionTransitionConfirmation(write.receipt(), false);
        };
    var now = Instant.parse("2026-09-07T11:00:00.000001Z");
    ChangeWorkSessionUseCase useCase =
        new ChangeWorkSession(store, Clock.fixed(now, java.time.ZoneOffset.UTC));
    var result =
        useCase.resume(
            "owner", start.id(), UUID.randomUUID(), new WorkSessionRevision(start.id(), 2));
    assertThat(result.receipt().action()).isEqualTo("RESUME");
    assertThat(result.receipt().before()).isSameAs(before);
    assertThat(result.receipt().after())
        .isEqualTo(new WorkSessionState(start, "running", 3, now, 1000001, now));
    var event = writes.getFirst().event();
    assertThat(event.action()).isEqualTo("RESUME");
    assertThat(event.fromStatus()).isEqualTo("paused");
    assertThat(event.toStatus()).isEqualTo("running");
    assertThat(event.runningSince()).isEqualTo(now);
    assertThat(event.workedMicroseconds()).isEqualTo("1000001");
  }

  @Test
  void s2_pauseRecordsExactIntervalReceiptAndEvent() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00.123456Z"),
            25,
            Instant.parse("2026-09-07T10:25:00.123456Z"),
            "Europe/Madrid");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var key = UUID.randomUUID();
    var writes = new ArrayList<WorkSessionTransition>();
    WorkSessionChanging store =
        (owner, session, requestKey, action, expected, notes, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(session).isEqualTo(start.id());
          assertThat(requestKey).isEqualTo(key);
          assertThat(action).isEqualTo("PAUSE");
          assertThat(expected).isEqualTo(new WorkSessionRevision(start.id(), 1));
          var write = operation.apply(before);
          writes.add(write);
          return new WorkSessionTransitionConfirmation(write.receipt(), false);
        };
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T10:00:01.123457999Z"));
    ChangeWorkSessionUseCase useCase = new ChangeWorkSession(store, clock);
    var result = useCase.pause("owner", start.id(), key, new WorkSessionRevision(start.id(), 1));
    var at = Instant.parse("2026-09-07T10:00:01.123457Z");
    assertThat(result.replayed()).isFalse();
    assertThat(writes).hasSize(1);
    var receipt = result.receipt();
    assertThat(receipt.id()).isNotNull();
    assertThat(receipt.sessionId()).isEqualTo(start.id());
    assertThat(receipt.action()).isEqualTo("PAUSE");
    assertThat(receipt.occurredAt()).isEqualTo(at);
    assertThat(receipt.before()).isSameAs(before);
    assertThat(receipt.after())
        .isEqualTo(new WorkSessionState(start, "paused", 2, at, 1000001, null));
    assertThat(writes.getFirst().receipt()).isSameAs(receipt);
    var event = writes.getFirst().event();
    assertThat(event.eventId()).isNotNull().isNotEqualTo(receipt.id());
    assertThat(event.aggregateId()).isEqualTo(start.id());
    assertThat(event.ownerId()).isEqualTo("owner");
    assertThat(event.occurredAt()).isEqualTo(at);
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.type()).isEqualTo("WorkSessionStateChanged.v1");
    assertThat(event.action()).isEqualTo("PAUSE");
    assertThat(event.revision()).isEqualTo("2");
    assertThat(event.fromStatus()).isEqualTo("running");
    assertThat(event.toStatus()).isEqualTo("paused");
    assertThat(event.workedMicroseconds()).isEqualTo("1000001");
    assertThat(event.runningSince()).isNull();
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}
