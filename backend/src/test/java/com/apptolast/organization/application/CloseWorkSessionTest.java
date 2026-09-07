package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CloseWorkSessionTest {
  @Test
  void s1_closesRunningWithExactWorkNotesAndIndependentEvent() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:59:00Z"),
            25,
            Instant.parse("2026-09-07T10:24:00Z"),
            "UTC");
    var since = Instant.parse("2026-09-07T10:00:00.123456Z");
    var before = new WorkSessionState(start, "running", 3, since, 59999999, since);
    var notes = new WorkSessionCloseNotes("Avance parcial", "Continuar la revisión");
    var key = UUID.randomUUID();
    var writes = new ArrayList<WorkSessionTransition>();
    WorkSessionChanging store =
        (owner, session, requestKey, action, revision, input, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(session).isEqualTo(start.id());
          assertThat(requestKey).isEqualTo(key);
          assertThat(action).isEqualTo("CLOSE");
          assertThat(revision).isEqualTo(new WorkSessionRevision(start.id(), 3));
          assertThat(input).isEqualTo(notes);
          var change = operation.apply(before);
          writes.add(change);
          return new WorkSessionTransitionConfirmation(change.receipt(), false);
        };
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T10:00:01.123457999Z"));
    ChangeWorkSessionUseCase useCase = new ChangeWorkSession(store, clock);
    var result =
        useCase.close("owner", start.id(), key, new WorkSessionRevision(start.id(), 3), notes);
    var at = Instant.parse("2026-09-07T10:00:01.123457Z");
    assertThat(result.replayed()).isFalse();
    var receipt = result.receipt();
    assertThat(receipt.id()).isNotNull();
    assertThat(receipt.sessionId()).isEqualTo(start.id());
    assertThat(receipt.action()).isEqualTo("CLOSE");
    assertThat(receipt.before()).isSameAs(before);
    assertThat(receipt.occurredAt()).isEqualTo(at);
    assertThat(receipt.after())
        .isEqualTo(new WorkSessionState(start, "closed", 4, at, 61000000, null));
    assertThat(receipt.closure())
        .isEqualTo(
            new WorkSessionClosure(
                "Avance parcial", "Continuar la revisión", LocalDate.of(2026, 9, 7), "UTC"));
    assertThat(writes).hasSize(1);
    assertThat(writes.getFirst().event()).isNull();
    var event = writes.getFirst().closedEvent();
    assertThat(event.eventId()).isNotNull().isNotEqualTo(receipt.id());
    assertThat(event.aggregateId()).isEqualTo(start.id());
    assertThat(event.ownerId()).isEqualTo("owner");
    assertThat(event.occurredAt()).isEqualTo(at);
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.type()).isEqualTo("WorkSessionClosed.v1");
    assertThat(event.revision()).isEqualTo("4");
    assertThat(event.fromStatus()).isEqualTo("running");
    assertThat(event.workedMicroseconds()).isEqualTo("61000000");
    assertThat(event.workDate()).isEqualTo(LocalDate.of(2026, 9, 7));
    assertThat(event.closeZoneId()).isEqualTo("UTC");
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }

  @Test
  void s2_closesPausedWithoutCountingTheRest() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var at = Instant.parse("2026-09-08T10:00:00Z");
    var result =
        new ChangeWorkSession(store, Clock.fixed(at, ZoneOffset.UTC))
            .close(
                "owner",
                start.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(start.id(), 2),
                new WorkSessionCloseNotes("", ""));
    assertThat(result.receipt().after())
        .isEqualTo(new WorkSessionState(start, "closed", 3, at, 60000000, null));
    assertThat(result.receipt().closure().workDate()).isEqualTo(LocalDate.of(2026, 9, 8));
  }

  @Test
  void s13_staleRevisionPrecedesClosedStateAndClock() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "closed", 3, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 2),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("PRECONDITION_FAILED"));
    verifyNoInteractions(clock);
  }

  @Test
  void s13_closedSessionRejectsNewCloseBeforeClock() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "closed", 3, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 3),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_STATE_CONFLICT"));
    verifyNoInteractions(clock);
  }

  @Test
  void s14_exhaustedRevisionPrecedesClock() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "paused", Long.MAX_VALUE, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), Long.MAX_VALUE),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_REVISION_EXHAUSTED"));
    verifyNoInteractions(clock);
  }

  @Test
  void s7_rejectsClockBeforeLastChange() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T09:00:59.999999Z"));
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 2),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
    verify(clock).instant();
  }

  @Test
  void s7_rejectsClockOutsideFourDigitUtc() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "UTC");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("+10000-01-01T00:00:00Z"));
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 2),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
    verify(clock).instant();
  }

  @Test
  void s9_usesUtcWhenHistoricalZoneCannotResolve() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "Removed/Zone");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var at = Instant.parse("2026-09-08T10:00:00Z");
    var result =
        new ChangeWorkSession(store, Clock.fixed(at, ZoneOffset.UTC))
            .close(
                "owner",
                start.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(start.id(), 2),
                new WorkSessionCloseNotes("", ""));
    assertThat(result.receipt().after())
        .isEqualTo(new WorkSessionState(start, "closed", 3, at, 60000000, null));
    assertThat(result.receipt().closure().closeZoneId()).isEqualTo("UTC");
    assertThat(result.receipt().after().session().zoneId()).isEqualTo("Removed/Zone");
    assertThat(result.receipt().closure().workDate()).isEqualTo(LocalDate.of(2026, 9, 8));
  }

  @Test
  void s10_rejectsLocalYearOverflowWithoutUtcFallback() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T09:00:00Z"),
            25,
            Instant.parse("2026-09-07T09:25:00Z"),
            "Etc/GMT-14");
    var before =
        new WorkSessionState(
            start, "paused", 2, Instant.parse("2026-09-07T09:01:00Z"), 60000000, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("9999-12-31T23:59:59.999999Z"));
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 2),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
    verify(clock).instant();
  }

  @Test
  void s10_rejectsLocalYearZeroWithoutUtcFallback() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("0001-01-01T00:00:00Z"),
            25,
            Instant.parse("0001-01-01T00:25:00Z"),
            "Etc/GMT+12");
    var before =
        new WorkSessionState(start, "paused", 2, Instant.parse("0001-01-01T00:00:00Z"), 0, null);
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("0001-01-01T00:01:00Z"));
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        "owner",
                        start.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(start.id(), 2),
                        new WorkSessionCloseNotes("", "")))
        .isInstanceOfSatisfying(
            WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
    verify(clock).instant();
  }

  @Test
  void s4_nullNotesNormalizeToEmptyIntent() {
    assertThat(new WorkSessionCloseNotes(null, null)).isEqualTo(new WorkSessionCloseNotes("", ""));
  }

  @Test
  void s5_rejectsProgressBeyondTwoThousandCodePoints() {
    assertThatThrownBy(() -> new WorkSessionCloseNotes("a".repeat(2001), ""))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("progressNote", "INVALID_VALUE")));
  }

  @Test
  void s5_rejectsNextStepBeyondTwoThousandEmoji() {
    assertThatThrownBy(() -> new WorkSessionCloseNotes("", "😀".repeat(2001)))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("nextStep", "INVALID_VALUE")));
  }

  @Test
  void s5_rejectsDecodedNullCharacter() {
    assertThatThrownBy(() -> new WorkSessionCloseNotes("a" + (char) 0 + "b", ""))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("progressNote", "INVALID_VALUE")));
  }

  @Test
  void s5_rejectsIsolatedHighSurrogate() {
    assertThatThrownBy(() -> new WorkSessionCloseNotes("", "a" + (char) 0xD800 + "b"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("nextStep", "INVALID_VALUE")));
  }

  @Test
  void s5_rejectsIsolatedLowSurrogate() {
    assertThatThrownBy(() -> new WorkSessionCloseNotes("a" + (char) 0xDFFF + "b", ""))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(tuple("progressNote", "INVALID_VALUE")));
  }

  @Test
  void s4_preservesTwoThousandValidEmoji() {
    var text = "😀".repeat(2000);
    var notes = new WorkSessionCloseNotes(text, text);
    assertThat(notes.progressNote()).isEqualTo(text);
    assertThat(notes.nextStep()).isEqualTo(text);
  }

  @Test
  void s4_preservesWhitespaceAndLineBreaks() {
    var text = "  revisión\n siguiente paso  ";
    var notes = new WorkSessionCloseNotes(text, text);
    assertThat(notes.progressNote()).isEqualTo(text);
    assertThat(notes.nextStep()).isEqualTo(text);
  }

  @Test
  void s8_preservesExactMicrosecondsAcrossTheWholeUtcRange() {
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
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var clock = Clock.fixed(Instant.parse("9999-12-31T23:59:59.999999Z"), ZoneOffset.UTC);
    var receipt =
        new ChangeWorkSession(store, clock)
            .close(
                "owner",
                start.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(start.id(), 1),
                new WorkSessionCloseNotes("", ""))
            .receipt();
    assertThat(receipt.after().workedMicroseconds()).isEqualTo(315537897599999999L);
    assertThat(receipt.closure().workDate()).isEqualTo(LocalDate.of(9999, 12, 31));
  }

  @Test
  void s9_attributesTheLocalClosingDayInTheHistoricalZone() {
    var at = Instant.parse("2026-09-07T22:00:00Z");
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
    WorkSessionChanging store =
        (owner, session, key, action, revision, notes, operation) ->
            new WorkSessionTransitionConfirmation(operation.apply(before).receipt(), false);
    var receipt =
        new ChangeWorkSession(
                store, Clock.fixed(Instant.parse("2026-09-07T22:30:00Z"), ZoneOffset.UTC))
            .close(
                "owner",
                start.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(start.id(), 1),
                new WorkSessionCloseNotes("", ""))
            .receipt();
    assertThat(receipt.closure().workDate()).isEqualTo(LocalDate.of(2026, 9, 8));
    assertThat(receipt.closure().closeZoneId()).isEqualTo("Europe/Madrid");
    assertThat(receipt.after().session()).isSameAs(start);
  }
}
