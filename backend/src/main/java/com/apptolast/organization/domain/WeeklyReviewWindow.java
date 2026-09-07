package com.apptolast.organization.domain;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.IntStream;

public record WeeklyReviewWindow(Instant serverNow) {
  public WeeklyReview emptyReview() {
    var monday =
        serverNow
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    var days =
        IntStream.range(0, 7)
            .mapToObj(
                n -> {
                  var date = monday.plusDays(n);
                  return new WeeklyReview.Day(
                      date,
                      date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                      date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                      0,
                      0,
                      null);
                })
            .toList();
    return new WeeklyReview(
        monday,
        monday.plusDays(6),
        "UTC",
        "UNCONFIGURED",
        null,
        serverNow,
        days.getFirst().startAt(),
        days.getLast().endAt(),
        days,
        new WeeklyReview.Totals(0, 0, null),
        0);
  }
}
