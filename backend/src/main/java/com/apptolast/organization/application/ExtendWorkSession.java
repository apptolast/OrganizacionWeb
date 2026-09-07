package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ExtendWorkSession implements ExtendWorkSessionUseCase {
  private final WorkSessionExtending store;
  private final Clock clock;

  public ExtendWorkSession(WorkSessionExtending store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public WorkSessionTransitionConfirmation extend(
      String owner, UUID session, UUID key, WorkSessionRevision expected, int additionalMinutes) {
    return store.extend(
        owner,
        session,
        key,
        expected,
        additionalMinutes,
        context -> {
          var before = context.state();
          var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
          var end = context.effectiveEndAt().plus(additionalMinutes, ChronoUnit.MINUTES);
          var after =
              new WorkSessionState(
                  before.session(),
                  before.status(),
                  before.revision() + 1,
                  before.changedAt(),
                  before.workedMicroseconds(),
                  before.runningSince());
          var extension =
              new WorkSessionExtension(additionalMinutes, context.effectiveEndAt(), end);
          var receipt =
              new WorkSessionTransitionReceipt(
                  UUID.randomUUID(), session, "EXTEND", now, before, after, null, extension);
          var event =
              new WorkSessionExtended(
                  UUID.randomUUID(),
                  session,
                  owner,
                  now,
                  1,
                  "WorkSessionExtended.v1",
                  Long.toString(after.revision()),
                  additionalMinutes,
                  context.effectiveEndAt(),
                  end,
                  before.status());
          return new WorkSessionExtensionTransition(receipt, event);
        });
  }
}
