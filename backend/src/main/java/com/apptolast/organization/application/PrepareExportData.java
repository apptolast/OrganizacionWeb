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
    return queries.prepare(
        owner,
        () -> {
          var instant = clock.instant().truncatedTo(ChronoUnit.MICROS);
          if (instant.isBefore(java.time.Instant.parse("0001-01-01T00:00:00Z"))
              || instant.isAfter(java.time.Instant.parse("9999-12-31T23:59:59.999999Z")))
            throw new StorageUnavailableException(
                new IllegalArgumentException("Invalid export time"));
          return instant;
        });
  }
}
