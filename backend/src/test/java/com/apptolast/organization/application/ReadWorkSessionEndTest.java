package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReadWorkSessionEndTest {
  @Test
  void s13_readsTheEndWithOneClockCaptureAfterTheStoredSnapshot() {
    var id = UUID.randomUUID();
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var start =
        new SessionStart(
            id, UUID.randomUUID(), UUID.randomUUID(), at, 25, at.plusSeconds(1500), "UTC");
    var state = new WorkSessionState(start, "paused", 2, at, 0, null);
    var context = new WorkSessionEnd(state, start.plannedEndAt(), at);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T10:10:00.123456999Z"));
    WorkSessionEndQueries queries =
        (owner, session, snapshot) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(session).isEqualTo(id);
          verifyNoInteractions(clock);
          return snapshot.apply(context);
        };
    ReadWorkSessionEndUseCase useCase = new ReadWorkSessionEnd(queries, clock);
    var result = useCase.read("owner", id);
    assertThat(result.state()).isSameAs(state);
    assertThat(result.effectiveEndAt()).isEqualTo(start.plannedEndAt());
    assertThat(result.serverNow()).isEqualTo(Instant.parse("2026-09-07T10:10:00.123456Z"));
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}
