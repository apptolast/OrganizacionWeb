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
  private final ZoneCatalog catalog;

  public StartWorkSession(WorkSessionStarting store, Clock clock, ZoneCatalog catalog) {
    this.store = store;
    this.clock = clock;
    this.catalog = catalog;
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
          WorkSessionContext.requireEligible(context.projectStatus(), context.taskStatus());
          var started = clock.instant().truncatedTo(ChronoUnit.MICROS);
          if (started.isBefore(java.time.Instant.parse("0001-01-01T00:00:00Z"))
              || !started.isBefore(java.time.Instant.parse("+10000-01-01T00:00:00Z")))
            throw new WorkSessionTimeOutOfRangeException();
          var end = started.plusSeconds(plannedMinutes * 60L);
          if (!end.isBefore(java.time.Instant.parse("+10000-01-01T00:00:00Z")))
            throw new WorkSessionTimeOutOfRangeException();
          var session =
              new SessionStart(
                  UUID.randomUUID(),
                  project,
                  task,
                  started,
                  plannedMinutes,
                  end,
                  context
                      .zoneId()
                      .filter(catalog.zones()::contains)
                      .filter(StartWorkSession::resolvable)
                      .orElse("UTC"));
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

  private static boolean resolvable(String zone) {
    try {
      java.time.ZoneId.of(zone);
      return true;
    } catch (java.time.DateTimeException invalid) {
      return false;
    }
  }
}
