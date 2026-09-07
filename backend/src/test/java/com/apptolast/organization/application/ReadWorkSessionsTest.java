package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReadWorkSessionsTest {
  @Test
  void s23_returnsActiveSessionFromOwnerQueries() {
    var session =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-06T10:00:00Z"),
            25,
            Instant.parse("2026-09-06T10:25:00Z"),
            "UTC");
    var queries = mock(WorkSessionQueries.class);
    when(queries.active("owner")).thenReturn(Optional.of(session));
    ReadWorkSessionsUseCase read = new ReadWorkSessions(queries);
    assertThat(read.active("owner")).containsSame(session);
    verify(queries).active("owner");
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s21_readsOriginalByIdentity() {
    var session =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-06T10:00:00Z"),
            25,
            Instant.parse("2026-09-06T10:25:00Z"),
            "UTC");
    var queries = mock(WorkSessionQueries.class);
    when(queries.detail("owner", session.id())).thenReturn(Optional.of(session));
    ReadWorkSessionsUseCase read = new ReadWorkSessions(queries);
    assertThat(read.detail("owner", session.id())).isSameAs(session);
    verify(queries).detail("owner", session.id());
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s22_absentIdentityIsNotFound() {
    var queries = mock(WorkSessionQueries.class);
    var id = UUID.randomUUID();
    when(queries.detail("owner", id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> new ReadWorkSessions(queries).detail("owner", id))
        .isInstanceOf(WorkSessionNotFoundException.class);
  }

  @Test
  void s21_readsOriginalByRequestKey() {
    var session =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.parse("2026-09-06T10:00:00Z"),
            25,
            Instant.parse("2026-09-06T10:25:00Z"),
            "UTC");
    var key = UUID.randomUUID();
    var queries = mock(WorkSessionQueries.class);
    when(queries.byRequest("owner", key)).thenReturn(Optional.of(session));
    ReadWorkSessionsUseCase read = new ReadWorkSessions(queries);
    assertThat(read.byRequest("owner", key)).isSameAs(session);
    verify(queries).byRequest("owner", key);
    verifyNoMoreInteractions(queries);
  }
}
