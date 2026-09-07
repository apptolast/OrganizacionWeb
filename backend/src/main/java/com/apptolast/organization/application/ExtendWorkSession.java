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
    if (additionalMinutes < 1 || additionalMinutes > 1440)
      throw new com.apptolast.organization.domain.ValidationException(
          java.util.List.of(
              new com.apptolast.organization.domain.FieldError(
                  "additionalMinutes", "OUT_OF_RANGE", "Debe estar entre 1 y 1440 minutos.")));
    return store.extend(
        owner,
        session,
        key,
        expected,
        additionalMinutes,
        context -> {
          var before = context.state();
          before.requireClose(expected.value());
          var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
          context.requireTime(now);
          var base = now.isAfter(context.effectiveEndAt()) ? now : context.effectiveEndAt();
          var end = base.plus(additionalMinutes, ChronoUnit.MINUTES);
          before.requireTime(end);
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
