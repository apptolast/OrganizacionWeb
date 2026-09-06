package com.apptolast.organization.application;

import com.apptolast.organization.domain.TodayWindow;
import java.time.Clock;

public final class ReadToday implements ReadTodayUseCase {
  private final TodayQueries queries;
  private final Clock clock;
  private final ZoneCatalog catalog;

  public ReadToday(TodayQueries queries, Clock clock, ZoneCatalog catalog) {
    this.queries = queries;
    this.clock = clock;
    this.catalog = catalog;
  }

  public TodayWindow.Agenda get(String owner) {
    var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var zones = catalog.zones();
    return queries.read(owner, preference -> TodayWindow.at(now, preference, zones));
  }
}
