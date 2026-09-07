package com.apptolast.organization.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class ReadWorkSessionEnd implements ReadWorkSessionEndUseCase {
  private final WorkSessionEndQueries queries;
  private final Clock clock;

  public ReadWorkSessionEnd(WorkSessionEndQueries queries, Clock clock) {
    this.queries = queries;
    this.clock = clock;
  }

  public WorkSessionEndSnapshot read(String owner, UUID session) {
    return queries.readEnd(
        owner,
        session,
        context ->
            new WorkSessionEndSnapshot(
                context.state(),
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                context.effectiveEndAt()));
  }
}
