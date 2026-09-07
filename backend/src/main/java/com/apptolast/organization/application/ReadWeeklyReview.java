package com.apptolast.organization.application;

import com.apptolast.organization.domain.WeeklyReview;
import com.apptolast.organization.domain.WeeklyReviewWindow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ReadWeeklyReview implements ReadWeeklyReviewUseCase {
  private final WeeklyReviewQueries queries;
  private final Clock clock;

  public ReadWeeklyReview(WeeklyReviewQueries queries, Clock clock, ZoneCatalog catalog) {
    this.queries = queries;
    this.clock = clock;
  }

  public WeeklyReview get(String owner, LocalDate date, String zoneId) {
    return queries.read(
        owner,
        preference -> new WeeklyReviewWindow(clock.instant().truncatedTo(ChronoUnit.MICROS)));
  }
}
