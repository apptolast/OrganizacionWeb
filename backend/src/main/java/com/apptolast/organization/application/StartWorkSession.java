package com.apptolast.organization.application;

import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.SessionStart;
import com.apptolast.organization.domain.ValidationException;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public final class StartWorkSession implements StartWorkSessionUseCase {
  private final WorkSessionStarting store;
  private final Clock clock;

  public StartWorkSession(WorkSessionStarting store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public WorkSessionConfirmation start(
      String owner, UUID project, UUID task, UUID key, int plannedMinutes) {
    if (plannedMinutes < 1 || plannedMinutes > 1440) {
      throw new ValidationException(
          List.of(
              new FieldError(
                  "plannedMinutes", "OUT_OF_RANGE", "Debe estar entre 1 y 1440 minutos.")));
    }
    return store.commit(
        owner,
        project,
        task,
        key,
        plannedMinutes,
        context -> {
          if ("completed".equals(context.projectStatus())) throw new ProjectCompletedException();
          if ("completed".equals(context.taskStatus())) throw new TaskCompletedException();
          var started = clock.instant().truncatedTo(ChronoUnit.MICROS);
          var session =
              new SessionStart(
                  UUID.randomUUID(),
                  project,
                  task,
                  started,
                  plannedMinutes,
                  started.plusSeconds(plannedMinutes * 60L),
                  context.zoneId().orElseThrow());
          var event =
              new WorkSessionStarted(
                  UUID.randomUUID(),
                  session.id(),
                  owner,
                  started,
                  1,
                  "WorkSessionStarted.v1",
                  project,
                  task,
                  plannedMinutes,
                  session.plannedEndAt(),
                  session.zoneId());
          return new WorkSessionChange(session, event);
        });
  }
}
