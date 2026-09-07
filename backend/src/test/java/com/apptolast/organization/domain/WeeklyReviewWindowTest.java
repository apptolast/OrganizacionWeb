package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeeklyReviewWindowTest {
  @Test
  void s15_zeroBudgetRemainsKnownZeroRatherThanUnknown() {
    var budget = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budget.put(day, 0);
    var result =
        new WeeklyReviewWindow(
                Instant.parse("2026-09-09T12:00:00Z"),
                LocalDate.parse("2026-09-07"),
                "UTC",
                "AVAILABILITY",
                "UTC",
                budget)
            .emptyReview();
    assertThat(result.days())
        .allSatisfy(day -> assertThat(day.capacityMicroseconds()).isEqualTo(0L));
    assertThat(result.totals().capacityMicroseconds()).isEqualTo(0L);
  }

  @Test
  void s12_springTransitionCountsOneRealHourWithoutReducingCapacity() {
    var budget = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budget.put(day, 120);
    var window =
        new WeeklyReviewWindow(
            Instant.parse("2026-09-09T12:00:00Z"),
            LocalDate.parse("2026-03-29"),
            "Europe/Madrid",
            "EXPLICIT",
            "Europe/Madrid",
            budget);
    var result =
        window.summarize(
            List.of(),
            List.of(
                new WeeklyReviewWindow.Interval(
                    OffsetDateTime.parse("2026-03-29T01:30:00+01:00").toInstant(),
                    OffsetDateTime.parse("2026-03-29T03:30:00+02:00").toInstant())));
    assertThat(Duration.between(result.startAt(), result.endAt()).toHours()).isEqualTo(167);
    assertThat(result.days().getLast().workedMicroseconds()).isEqualTo(3_600_000_000L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(3_600_000_000L);
    assertThat(result.totals().capacityMicroseconds()).isEqualTo(50_400_000_000L);
  }

  @Test
  void s12_fallTransitionCountsThreeRealHoursInsideTheLongWeek() {
    var window =
        new WeeklyReviewWindow(
            Instant.parse("2026-11-01T12:00:00Z"),
            LocalDate.parse("2026-10-25"),
            "Europe/Madrid",
            "EXPLICIT",
            null,
            null);
    var result =
        window.summarize(
            List.of(),
            List.of(
                new WeeklyReviewWindow.Interval(
                    OffsetDateTime.parse("2026-10-25T01:30:00+02:00").toInstant(),
                    OffsetDateTime.parse("2026-10-25T03:30:00+01:00").toInstant())));
    assertThat(Duration.between(result.startAt(), result.endAt()).toHours()).isEqualTo(169);
    assertThat(result.days().getLast().workedMicroseconds()).isEqualTo(10_800_000_000L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(10_800_000_000L);
  }

  @Test
  void s7_skippedCivilDayHasZeroDurationsButKeepsItsChosenBudget() {
    var budget = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budget.put(day, 120);
    var window =
        new WeeklyReviewWindow(
            Instant.parse("2026-09-09T12:00:00Z"),
            LocalDate.parse("2011-12-30"),
            "Pacific/Apia",
            "EXPLICIT",
            "Pacific/Apia",
            budget);
    var result = window.emptyReview();
    var skipped = result.days().get(4);
    assertThat(skipped.date()).isEqualTo(LocalDate.parse("2011-12-30"));
    assertThat(skipped.startAt()).isEqualTo(skipped.endAt());
    assertThat(skipped.plannedMicroseconds()).isZero();
    assertThat(skipped.workedMicroseconds()).isZero();
    assertThat(skipped.capacityMicroseconds()).isEqualTo(7_200_000_000L);
    assertThat(result.totals().capacityMicroseconds()).isEqualTo(50_400_000_000L);
  }

  @Test
  void s19_durationAccumulatorRejectsOverflowInsteadOfWrapping() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                WeeklyReviewWindow.sumMicroseconds(
                    java.util.stream.LongStream.of(Long.MAX_VALUE, 1)))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void s13_workPreservesMicrosecondsAndZeroLengthIntervals() {
    var start = Instant.parse("2026-09-07T09:00:00Z");
    var window =
        new WeeklyReviewWindow(
            Instant.parse("2026-09-09T12:00:00Z"),
            LocalDate.parse("2026-09-07"),
            "UTC",
            "EXPLICIT",
            null,
            null);
    var result =
        window.summarize(
            List.of(),
            List.of(
                new WeeklyReviewWindow.Interval(start, start.plusNanos(1000)),
                new WeeklyReviewWindow.Interval(start.plusSeconds(10), start.plusSeconds(10)),
                new WeeklyReviewWindow.Interval(
                    start.plusSeconds(20), start.plusSeconds(20).plusNanos(1000))));
    assertThat(result.days())
        .extracting(WeeklyReview.Day::workedMicroseconds)
        .containsExactly(2L, 0L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(2);
    assertThat(result.totals().plannedMicroseconds()).isZero();
  }

  @Test
  void s8_plannedIntervalsAreClippedToTheirCivilDays() {
    var window =
        new WeeklyReviewWindow(
            Instant.parse("2026-09-09T12:00:00Z"),
            LocalDate.parse("2026-09-07"),
            "UTC",
            "EXPLICIT",
            null,
            null);
    var result =
        window.summarize(
            List.of(
                new WeeklyReviewWindow.Interval(
                    Instant.parse("2026-09-06T23:30:00Z"), Instant.parse("2026-09-07T00:30:00Z")),
                new WeeklyReviewWindow.Interval(
                    Instant.parse("2026-09-07T23:30:00Z"), Instant.parse("2026-09-08T00:30:00Z"))));
    assertThat(result.days())
        .extracting(WeeklyReview.Day::plannedMicroseconds)
        .containsExactly(3_600_000_000L, 1_800_000_000L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().plannedMicroseconds()).isEqualTo(5_400_000_000L);
    assertThat(result.totals().workedMicroseconds()).isZero();
  }
}
