package com.apptolast.organization.application;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

public final class PrepareExportData implements ExportDataUseCase {
  private final ExportDataQueries queries;
  private final Clock clock;

  public PrepareExportData(ExportDataQueries queries, Clock clock) {
    this.queries = queries;
    this.clock = clock;
  }

  public PreparedExport prepare(String owner) {
    return queries.prepare(owner, () -> clock.instant().truncatedTo(ChronoUnit.MICROS));
  }
}