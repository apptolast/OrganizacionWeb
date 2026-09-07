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

  public WorkSessionTransitionConfirmation pause(String owner, UUID session, UUID key, WorkSessionRevision expected) {
    return transition(owner, session, key, expected, true);
  }

  public WorkSessionTransitionConfirmation resume(String owner, UUID session, UUID key, WorkSessionRevision expected) {
    return transition(owner, session, key, expected, false);
  }

  private WorkSessionTransitionConfirmation transition(String owner, UUID session, UUID key, WorkSessionRevision expected, boolean pause) {
    var action = pause ? "PAUSE" : "RESUME";
    return store.commit(owner, session, key, action, expected, before -> {
      var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
      var worked = before.workedMicroseconds() + (pause ? ChronoUnit.MICROS.between(before.runningSince(), now) : 0);
      var after = new WorkSessionState(before.session(), pause ? "paused" : "running", before.revision() + 1, now,
          worked, pause ? null : now);
      var receipt = new WorkSessionTransitionReceipt(UUID.randomUUID(), session, action, now, before, after);
      var event = new WorkSessionStateChanged(UUID.randomUUID(), session, owner, now, 1,
          "WorkSessionStateChanged.v1", action, Long.toString(after.revision()), before.status(),
          after.status(), Long.toString(after.workedMicroseconds()), after.runningSince());
      return new WorkSessionTransition(receipt, event);
    });
  }
}
