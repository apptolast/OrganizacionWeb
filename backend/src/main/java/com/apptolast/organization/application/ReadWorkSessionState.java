package com.apptolast.organization.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ReadWorkSessionState implements ReadWorkSessionStateUseCase {
  private final WorkSessionStateQueries queries;
  private final Clock clock;

  public ReadWorkSessionState(WorkSessionStateQueries queries, Clock clock) {
    this.queries = queries;
    this.clock = clock;
  }

  public WorkSessionSnapshot read(String owner, UUID session) {
    return queries.read(
        owner,
        session,
        state -> {
          var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
          if (now.isBefore(java.time.Instant.parse("0001-01-01T00:00:00Z"))
              || !now.isBefore(java.time.Instant.parse("+10000-01-01T00:00:00Z")))
            throw new com.apptolast.organization.domain.WorkSessionTransitionException(
                "WORK_SESSION_TIME_OUT_OF_RANGE");
          return new WorkSessionSnapshot(
              state,
              now,
              state.workedMicroseconds()
                  + (state.status().equals("running")
                      ? Math.max(0, ChronoUnit.MICROS.between(state.runningSince(), now))
                      : 0));
        });
  }
}
