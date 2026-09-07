package com.apptolast.organization.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReview(
    LocalDate weekStart,
    LocalDate weekEnd,
    String zoneId,
    String zoneSource,
    String availabilityZoneId,
    Instant serverNow,
    Instant startAt,
    Instant endAt,
    List<Day> days,
    Totals totals,
    long unquantifiedSessionCount) {
  public record Day(
      LocalDate date,
      Instant startAt,
      Instant endAt,
      long plannedMicroseconds,
      long workedMicroseconds,
      Long capacityMicroseconds) {}

  public record Totals(
      long plannedMicroseconds, long workedMicroseconds, Long capacityMicroseconds) {}
}
