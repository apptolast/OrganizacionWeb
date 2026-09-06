package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class StartWorkSessionTest {
  @Test
  void s1_recordsOneRealStartAndEventWithFixedEnd() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var key = UUID.randomUUID();
    var changes = new ArrayList<WorkSessionChange>();
    var store = mock(WorkSessionStarting.class);
    when(store.commit(eq("owner"), eq(project), eq(task), eq(key), eq(25), any()))
        .thenAnswer(
            call -> {
              Function<WorkSessionContext, WorkSessionChange> operation = call.getArgument(5);
              var change =
                  operation.apply(
                      new WorkSessionContext("active", "pending", Optional.of("Europe/Madrid")));
              changes.add(change);
              return new WorkSessionConfirmation(change.session(), false);
            });
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-06T10:00:00.123456789Z"));
    StartWorkSessionUseCase start = new StartWorkSession(store, clock);
    var result = start.start("owner", project, task, key, 25);
    assertThat(result.replayed()).isFalse();
    assertThat(changes).hasSize(1);
    var session = result.session();
    assertThat(session.id()).isNotNull();
    assertThat(session)
        .isEqualTo(
            new SessionStart(
                session.id(),
                project,
                task,
                Instant.parse("2026-09-06T10:00:00.123456Z"),
                25,
                Instant.parse("2026-09-06T10:25:00.123456Z"),
                "Europe/Madrid"));
    var change = changes.getFirst();
    assertThat(change.session()).isSameAs(session);
    var event = change.event();
    assertThat(event.eventId()).isNotNull().isNotEqualTo(session.id());
    assertThat(event)
        .isEqualTo(
            new WorkSessionStarted(
                event.eventId(),
                session.id(),
                "owner",
                session.startedAt(),
                1,
                "WorkSessionStarted.v1",
                project,
                task,
                25,
                session.plannedEndAt(),
                "Europe/Madrid"));
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}
