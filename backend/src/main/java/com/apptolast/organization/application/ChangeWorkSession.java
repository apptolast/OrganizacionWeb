package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ChangeWorkSession implements ChangeWorkSessionUseCase {
  private final WorkSessionChanging store;
  private final Clock clock;

  public ChangeWorkSession(WorkSessionChanging store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public WorkSessionTransitionConfirmation close(
      String owner,
      UUID session,
      UUID key,
      WorkSessionRevision expected,
      com.apptolast.organization.domain.WorkSessionCloseNotes notes) {
    return store.commit(
        owner,
        session,
        key,
        "CLOSE",
        expected,
        notes,
        before -> {
          before.requireClose(expected.value());
          var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
          before.requireTime(now);
          var worked =
              before.workedMicroseconds()
                  + (before.status().equals("running")
                      ? ChronoUnit.MICROS.between(before.runningSince(), now)
                      : 0);
          var after =
              new WorkSessionState(
                  before.session(), "closed", before.revision() + 1, now, worked, null);
          java.time.ZoneId zone;
          try {
            zone = java.time.ZoneId.of(before.session().zoneId());
          } catch (java.time.DateTimeException unknownZone) {
            zone = java.time.ZoneId.of("UTC");
          }
          var day = now.atZone(zone).toLocalDate();
          if (day.getYear() < 1 || day.getYear() > 9999)
            throw new com.apptolast.organization.domain.WorkSessionTransitionException(
                "WORK_SESSION_TIME_OUT_OF_RANGE");
          var closure =
              new WorkSessionClosure(notes.progressNote(), notes.nextStep(), day, zone.getId());
          var receipt =
              new WorkSessionTransitionReceipt(
                  UUID.randomUUID(), session, "CLOSE", now, before, after, closure);
          var event =
              new WorkSessionClosed(
                  UUID.randomUUID(),
                  session,
                  owner,
                  now,
                  1,
                  "WorkSessionClosed.v1",
                  Long.toString(after.revision()),
                  before.status(),
                  Long.toString(worked),
                  day,
                  zone.getId());
          return new WorkSessionTransition(receipt, null, event);
        });
  }

  public WorkSessionTransitionConfirmation pause(
      String owner, UUID session, UUID key, WorkSessionRevision expected) {
    return transition(owner, session, key, expected, true);
  }

  public WorkSessionTransitionConfirmation resume(
      String owner, UUID session, UUID key, WorkSessionRevision expected) {
    return transition(owner, session, key, expected, false);
  }

  private WorkSessionTransitionConfirmation transition(
      String owner, UUID session, UUID key, WorkSessionRevision expected, boolean pause) {
    var action = pause ? "PAUSE" : "RESUME";
    return store.commit(
        owner,
        session,
        key,
        action,
        expected,
        before -> {
          before.requireTransition(expected.value(), pause);
          var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
          before.requireTime(now);
          var worked =
              before.workedMicroseconds()
                  + (pause ? ChronoUnit.MICROS.between(before.runningSince(), now) : 0);
          var after =
              new WorkSessionState(
                  before.session(),
                  pause ? "paused" : "running",
                  before.revision() + 1,
                  now,
                  worked,
                  pause ? null : now);
          var receipt =
              new WorkSessionTransitionReceipt(
                  UUID.randomUUID(), session, action, now, before, after);
          var event =
              new WorkSessionStateChanged(
                  UUID.randomUUID(),
                  session,
                  owner,
                  now,
                  1,
                  "WorkSessionStateChanged.v1",
                  action,
                  Long.toString(after.revision()),
                  before.status(),
                  after.status(),
                  Long.toString(after.workedMicroseconds()),
                  after.runningSince());
          return new WorkSessionTransition(receipt, event);
        });
  }
}
