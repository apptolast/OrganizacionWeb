package com.apptolast.organization.application;

import com.apptolast.organization.domain.WeeklyReview;
import com.apptolast.organization.domain.WeeklyReviewWindow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class ReadWeeklyReview implements ReadWeeklyReviewUseCase {
  private final WeeklyReviewQueries queries;
  private final Clock clock;
  private final ZoneCatalog catalog;

  public ReadWeeklyReview(WeeklyReviewQueries queries, Clock clock, ZoneCatalog catalog) {
    this.queries = queries;
    this.clock = clock;
    this.catalog = catalog;
  }

  public WeeklyReview get(String owner, LocalDate date, String zoneId) {
    if (date != null
        && (date.getYear() < 1
            || date.getYear() > 9999
            || date.with(
                        java.time.temporal.TemporalAdjusters.previousOrSame(
                            java.time.DayOfWeek.MONDAY))
                    .plusDays(6)
                    .getYear()
                > 9999))
      throw new com.apptolast.organization.domain.ValidationException(
          java.util.List.of(
              new com.apptolast.organization.domain.FieldError(
                  "date", "INVALID_VALUE", "Selecciona una semana completa entre 0001 y 9999.")));
    if (zoneId != null && !catalog.zones().contains(zoneId))
      throw new com.apptolast.organization.domain.ValidationException(
          java.util.List.of(
              new com.apptolast.organization.domain.FieldError(
                  "zoneId", "INVALID_VALUE", "Selecciona una zona disponible.")));
    return queries.read(
        owner,
        preference -> {
          var stored =
              preference.map(com.apptolast.organization.domain.Availability::zoneId).orElse(null);
          boolean available = stored != null && catalog.zones().contains(stored);
          var selected = zoneId != null ? zoneId : available ? stored : "UTC";
          return new WeeklyReviewWindow(
              clock.instant().truncatedTo(ChronoUnit.MICROS),
              date,
              selected,
              zoneId != null
                  ? "EXPLICIT"
                  : stored == null ? "UNCONFIGURED" : available ? "AVAILABILITY" : "UNAVAILABLE",
              stored,
              available && selected.equals(stored)
                  ? preference.orElseThrow().dailyMinutes()
                  : null);
        });
  }
}
