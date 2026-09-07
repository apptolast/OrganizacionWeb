package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import com.apptolast.organization.domain.WorkSessionState;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadWorkSessionStateTest {
  @Test
  void s23_readRejectsYearTenThousand() {
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
    var state = new WorkSessionState(start, "running", 1, at, 0, at);
    WorkSessionStateQueries queries = (owner, id, snapshot) -> snapshot.apply(state);
    var useCase =
        new ReadWorkSessionState(
            queries,
            Clock.fixed(Instant.parse("+10000-01-01T00:00:00Z"), java.time.ZoneOffset.UTC));
    assertThatThrownBy(() -> useCase.read("owner", start.id()))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s23_readRejectsUnrepresentableYearZero() {
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
    var state = new WorkSessionState(start, "running", 1, at, 0, at);
    WorkSessionStateQueries queries = (owner, id, snapshot) -> snapshot.apply(state);
    var useCase =
        new ReadWorkSessionState(
            queries,
            Clock.fixed(Instant.parse("0000-12-31T23:59:59.999999Z"), java.time.ZoneOffset.UTC));
    assertThatThrownBy(() -> useCase.read("owner", start.id()))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("WORK_SESSION_TIME_OUT_OF_RANGE"));
  }

  @Test
  void s23_backwardReadAddsNoNegativeWork() {
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
    var state =
        new WorkSessionState(start, "running", 3, at.plusSeconds(2), 1000000, at.plusSeconds(2));
    WorkSessionStateQueries queries = (owner, id, snapshot) -> snapshot.apply(state);
    var result =
        new ReadWorkSessionState(queries, Clock.fixed(at.plusSeconds(1), java.time.ZoneOffset.UTC))
            .read("owner", start.id());
    assertThat(result.netMicroseconds()).isEqualTo(1000000);
    assertThat(result.serverNow()).isEqualTo(at.plusSeconds(1));
    assertThat(result.state()).isSameAs(state);
  }

  @Test
  void s23_pausedNetDoesNotGrow() {
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
    var state = new WorkSessionState(start, "paused", 2, at.plusSeconds(1), 1000000, null);
    WorkSessionStateQueries queries = (owner, id, snapshot) -> snapshot.apply(state);
    var result =
        new ReadWorkSessionState(
                queries, Clock.fixed(at.plusSeconds(3600), java.time.ZoneOffset.UTC))
            .read("owner", start.id());
    assertThat(result.netMicroseconds()).isEqualTo(1000000);
    assertThat(result.state()).isSameAs(state);
  }

  @Test
  void s1_readsLegacyStateBeforeCapturingOneMicrosecondClock() {
    var start =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-07T10:00:00.123456Z"),
            25,
            Instant.parse("2026-09-07T10:25:00.123456Z"),
            "UTC");
    var state = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T10:00:01.123456789Z"));
    WorkSessionStateQueries queries =
        (owner, id, snapshot) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(id).isEqualTo(start.id());
          verifyNoInteractions(clock);
          return snapshot.apply(state);
        };
    ReadWorkSessionStateUseCase useCase = new ReadWorkSessionState(queries, clock);
    var result = useCase.read("owner", start.id());
    assertThat(result.state()).isSameAs(state);
    assertThat(result.serverNow()).isEqualTo(Instant.parse("2026-09-07T10:00:01.123456Z"));
    assertThat(result.netMicroseconds()).isEqualTo(1000000L);
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}
