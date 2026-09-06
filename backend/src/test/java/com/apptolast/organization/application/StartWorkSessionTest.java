package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import com.apptolast.organization.domain.ValidationException;
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

  @Test
  void s6_rejectsZeroMinutesBeforeStorage() {
    var store = mock(WorkSessionStarting.class);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new StartWorkSession(store, clock)
                    .start("owner", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting("field", "code")
                    .containsExactly(tuple("plannedMinutes", "OUT_OF_RANGE")));
    verifyNoInteractions(store, clock);
  }

  @Test
  void s6_rejectsMinutesAboveMaximumBeforeStorage() {
    var store = mock(WorkSessionStarting.class);
    var clock = mock(Clock.class);
    assertThatThrownBy(
            () ->
                new StartWorkSession(store, clock)
                    .start("owner", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1441))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting("field", "code")
                    .containsExactly(tuple("plannedMinutes", "OUT_OF_RANGE")));
    verifyNoInteractions(store, clock);
  }

  @Test
  void s2_acceptsOneMinute() {
    var result =
        startInContext(
            "active",
            "pending",
            1,
            Clock.fixed(Instant.parse("2026-09-06T10:00:00.123456Z"), java.time.ZoneOffset.UTC));
    assertThat(result.session().plannedMinutes()).isEqualTo(1);
    assertThat(result.session().plannedEndAt())
        .isEqualTo(Instant.parse("2026-09-06T10:01:00.123456Z"));
  }

  private WorkSessionConfirmation startInContext(
      String projectStatus, String taskStatus, int minutes, Clock clock) {
    WorkSessionStarting store =
        (owner, project, task, key, duration, operation) -> {
          var change =
              operation.apply(
                  new WorkSessionContext(projectStatus, taskStatus, Optional.of("UTC")));
          return new WorkSessionConfirmation(change.session(), false);
        };
    return new StartWorkSession(store, clock)
        .start("owner", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), minutes);
  }

  @Test
  void s2_acceptsMaximumMinutes() {
    var result =
        startInContext(
            "active",
            "pending",
            1440,
            Clock.fixed(Instant.parse("2026-09-06T10:00:00.123456Z"), java.time.ZoneOffset.UTC));
    assertThat(result.session().plannedMinutes()).isEqualTo(1440);
    assertThat(result.session().plannedEndAt())
        .isEqualTo(Instant.parse("2026-09-07T10:00:00.123456Z"));
  }

  @Test
  void s11_rejectsCompletedProjectBeforeClock() {
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-06T10:00:00Z"));
    assertThatThrownBy(() -> startInContext("completed", "pending", 25, clock))
        .isInstanceOf(ProjectCompletedException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s11_rejectsCompletedTaskBeforeClock() {
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-06T10:00:00Z"));
    assertThatThrownBy(() -> startInContext("active", "completed", 25, clock))
        .isInstanceOf(TaskCompletedException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s11_completedProjectTakesPrecedenceOverCompletedTask() {
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-06T10:00:00Z"));
    assertThatThrownBy(() -> startInContext("completed", "completed", 25, clock))
        .isInstanceOf(ProjectCompletedException.class);
    verifyNoInteractions(clock);
  }
}
