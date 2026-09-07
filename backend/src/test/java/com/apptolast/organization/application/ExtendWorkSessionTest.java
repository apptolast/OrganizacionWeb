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
}
