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
  void s12_preservesTokenIdentityForTheTransactionalOwnershipCheck() {
    var session = UUID.randomUUID();
    var token = new WorkSessionRevision(UUID.randomUUID(), 1);
    var store = mock(WorkSessionChanging.class);
    var clock = mock(Clock.class);
    new ChangeWorkSession(store, clock).pause("owner", session, UUID.randomUUID(), token);
    verify(store).commit(org.mockito.ArgumentMatchers.eq("owner"), org.mockito.ArgumentMatchers.eq(session),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("PAUSE"),
        org.mockito.ArgumentMatchers.same(token), org.mockito.ArgumentMatchers.any());
    verifyNoInteractions(clock);
  }

  @Test
  void s3_resumePreservesAccumulatedWorkAndFixedEnd() {
    var start = new SessionStart(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        Instant.parse("2026-09-07T10:00:00.123456Z"), 25,
        Instant.parse("2026-09-07T10:25:00.123456Z"), "Europe/Madrid");
    var before = new WorkSessionState(start, "paused", 2,
        Instant.parse("2026-09-07T10:00:01.123457Z"), 1000001, null);
    var writes = new ArrayList<WorkSessionTransition>();
    WorkSessionChanging store = (owner, session, key, action, expected, operation) -> {
      assertThat(action).isEqualTo("RESUME");
      var write = operation.apply(before);
      writes.add(write);
      return new WorkSessionTransitionConfirmation(write.receipt(), false);
    };
    var now = Instant.parse("2026-09-07T11:00:00.000001Z");
    ChangeWorkSessionUseCase useCase = new ChangeWorkSession(store, Clock.fixed(now, java.time.ZoneOffset.UTC));
    var result = useCase.resume("owner", start.id(), UUID.randomUUID(), new WorkSessionRevision(start.id(), 2));
    assertThat(result.receipt().action()).isEqualTo("RESUME");
    assertThat(result.receipt().before()).isSameAs(before);
    assertThat(result.receipt().after()).isEqualTo(new WorkSessionState(start, "running", 3, now, 1000001, now));
    var event = writes.getFirst().event();
    assertThat(event.action()).isEqualTo("RESUME");
    assertThat(event.fromStatus()).isEqualTo("paused");
    assertThat(event.toStatus()).isEqualTo("running");
    assertThat(event.runningSince()).isEqualTo(now);
    assertThat(event.workedMicroseconds()).isEqualTo("1000001");
  }

  @Test
  void s2_pauseRecordsExactIntervalReceiptAndEvent() {
    var start = new SessionStart(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        Instant.parse("2026-09-07T10:00:00.123456Z"), 25,
        Instant.parse("2026-09-07T10:25:00.123456Z"), "Europe/Madrid");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var key = UUID.randomUUID();
    var writes = new ArrayList<WorkSessionTransition>();
    WorkSessionChanging store = (owner, session, requestKey, action, expected, operation) -> {
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
    assertThat(receipt.after()).isEqualTo(new WorkSessionState(start, "paused", 2, at, 1000001, null));
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
