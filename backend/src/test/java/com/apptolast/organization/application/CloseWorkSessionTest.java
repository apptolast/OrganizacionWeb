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
}
