package com.apptolast.organization.domain;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.IntStream;

public record WeeklyReviewWindow(
    Instant serverNow,
    LocalDate date,
    String zoneId,
    String zoneSource,
    String availabilityZoneId,
    java.util.Map<DayOfWeek, Integer> budgets) {
  public WeeklyReview emptyReview() {
    requirePublicInstant(serverNow);
    var localToday = serverNow.atZone(ZoneId.of(zoneId)).toLocalDate();
    if (localToday.getYear() < 1 || localToday.getYear() > 9999)
      throw new WeeklyReviewTimeOutOfRangeException();
    var monday =
        (date == null ? localToday : date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    if (monday.getYear() < 1 || monday.plusDays(6).getYear() > 9999)
      throw new WeeklyReviewTimeOutOfRangeException();
    var days =
        IntStream.range(0, 7)
            .mapToObj(
                n -> {
                  var date = monday.plusDays(n);
                  return new WeeklyReview.Day(
                      date,
                      date.atStartOfDay(ZoneId.of(zoneId)).toInstant(),
                      date.plusDays(1).atStartOfDay(ZoneId.of(zoneId)).toInstant(),
                      0,
                      0,
                      budgets == null ? null : budgets.get(date.getDayOfWeek()) * 60_000_000L);
                })
            .toList();
    requirePublicInstant(days.getFirst().startAt());
    return new WeeklyReview(
        monday,
        monday.plusDays(6),
        zoneId,
        zoneSource,
        availabilityZoneId,
        serverNow,
        days.getFirst().startAt(),
        days.getLast().endAt(),
        days,
        new WeeklyReview.Totals(
            0,
            0,
            budgets == null
                ? null
                : days.stream().mapToLong(WeeklyReview.Day::capacityMicroseconds).sum()),
        0);
  }

  private static void requirePublicInstant(Instant instant) {
    if (instant.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
        || !instant.isBefore(Instant.parse("+10000-01-01T00:00:00Z")))
      throw new WeeklyReviewTimeOutOfRangeException();
  }
}
