package com.apptolast.organization.domain;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public record WeeklyReviewWindow(
    Instant serverNow,
    LocalDate date,
    String zoneId,
    String zoneSource,
    String availabilityZoneId,
    Map<DayOfWeek, Integer> budgets) {
  public WeeklyReview emptyReview() {
    return summarize(List.of());
  }

  public WeeklyReview summarize(List<Interval> planned) {
    return summarize(planned, List.of());
  }

  public WeeklyReview summarize(List<Interval> planned, List<Interval> worked) {
    return summarize(planned, worked, 0);
  }

  public WeeklyReview summarize(List<Interval> planned, List<Interval> worked, long unquantified) {
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
                      sumMicroseconds(
                          planned.stream()
                              .mapToLong(
                                  interval ->
                                      interval.between(
                                          date.atStartOfDay(ZoneId.of(zoneId)).toInstant(),
                                          date.plusDays(1)
                                              .atStartOfDay(ZoneId.of(zoneId))
                                              .toInstant()))),
                      sumMicroseconds(
                          worked.stream()
                              .mapToLong(
                                  interval ->
                                      interval.between(
                                          date.atStartOfDay(ZoneId.of(zoneId)).toInstant(),
                                          date.plusDays(1)
                                              .atStartOfDay(ZoneId.of(zoneId))
                                              .toInstant()))),
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
            sumMicroseconds(days.stream().mapToLong(WeeklyReview.Day::plannedMicroseconds)),
            sumMicroseconds(days.stream().mapToLong(WeeklyReview.Day::workedMicroseconds)),
            budgets == null
                ? null
                : sumMicroseconds(days.stream().mapToLong(WeeklyReview.Day::capacityMicroseconds))),
        unquantified);
  }

  private static void requirePublicInstant(Instant instant) {
    if (instant.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
        || !instant.isBefore(Instant.parse("+10000-01-01T00:00:00Z")))
      throw new WeeklyReviewTimeOutOfRangeException();
  }

  static long sumMicroseconds(LongStream values) {
    return values.reduce(0, Math::addExact);
  }

  public record Interval(Instant startAt, Instant endAt) {
    long between(Instant start, Instant end) {
      var from = startAt.isAfter(start) ? startAt : start;
      var to = endAt.isBefore(end) ? endAt : end;
      return from.isBefore(to) ? java.time.temporal.ChronoUnit.MICROS.between(from, to) : 0;
    }
  }
}
