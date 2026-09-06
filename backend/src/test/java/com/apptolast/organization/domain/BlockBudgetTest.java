package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BlockBudgetTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "UTC,2030-01-07T23:00:00Z,2030-01-08T00:00:00Z,2030-01-07,3600,0,0",
    "Europe/Madrid,2030-01-07T22:30:00Z,2030-01-07T23:30:00Z,2030-01-07,1800,1800,0",
    "Europe/Madrid,2030-10-27T22:30:00Z,2030-10-27T23:30:00Z,2030-10-27,1800,1800,0",
    "Europe/Madrid,2030-01-07T12:00:00Z,2030-01-07T13:00:00Z,2030-01-07,3600,0,1800",
    "Europe/Madrid,2030-01-08T12:00:00Z,2030-01-08T13:00:00Z,2030-01-08,3600,0,1800"
  })
  void s11_s28_projectsMidnightAndHistoricalReservationsIntoCurrentBudgetZone(
      String zone,
      String start,
      String end,
      String firstDate,
      long first,
      long second,
      long planned) {
    var existing =
        new ResolvedBlockTime(
            Instant.parse("2030-01-07T22:30:00Z"),
            Instant.parse("2030-01-07T23:30:00Z"),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var days =
        BlockBudget.calculate(
            preference(zone, 120),
            Instant.parse(start),
            Instant.parse(end),
            planned == 0 ? List.of() : List.of(existing));
    var expected = new ArrayList<BudgetDay>();
    expected.add(new BudgetDay(LocalDate.parse(firstDate), 120, planned, first, 0));
    if (second > 0)
      expected.add(new BudgetDay(LocalDate.parse(firstDate).plusDays(1), 120, 0, second, 0));
    assertThat(days).containsExactlyElementsOf(expected);
    assertThat(days.stream().mapToLong(BudgetDay::requestedSeconds).sum())
        .isEqualTo(Duration.between(Instant.parse(start), Instant.parse(end)).getSeconds());
    assertThat(existing.startAt()).isEqualTo(Instant.parse("2030-01-07T22:30:00Z"));
    assertThat(existing.endAt()).isEqualTo(Instant.parse("2030-01-07T23:30:00Z"));
  }

  static Availability preference(String zone, int minutes) {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, minutes);
    return new Availability(
        UUID.randomUUID(), "owner", zone, days, 0, Instant.EPOCH, Instant.EPOCH);
  }

  @Test
  void s11_splitsPositiveIntersectionsAtBudgetMidnight() {
    var days =
        BlockBudget.calculate(
            preference("UTC", 120),
            Instant.parse("2030-01-07T23:30:00Z"),
            Instant.parse("2030-01-08T00:30:00Z"),
            List.of());
    assertThat(days).hasSize(2);
    assertThat(days.get(0).date()).isEqualTo(LocalDate.of(2030, 1, 7));
    assertThat(days.get(1).date()).isEqualTo(LocalDate.of(2030, 1, 8));
    for (var day : days) {
      assertThat(day.budgetMinutes()).isEqualTo(120);
      assertThat(day.plannedSeconds()).isZero();
      assertThat(day.requestedSeconds()).isEqualTo(1800);
      assertThat(day.excessSeconds()).isZero();
    }
  }

  @Test
  void s11_omitsSkippedLocalDay() {
    var days =
        BlockBudget.calculate(
            preference("Pacific/Apia", 120),
            Instant.parse("2011-12-30T09:30:00Z"),
            Instant.parse("2011-12-30T10:30:00Z"),
            List.of());
    assertThat(days)
        .extracting(BudgetDay::date)
        .containsExactly(LocalDate.of(2011, 12, 29), LocalDate.of(2011, 12, 31));
    assertThat(days).extracting(BudgetDay::requestedSeconds).containsExactly(1800L, 1800L);
  }

  @Test
  void s12_s15_countsExistingIntersectionsAndExcessInSeconds() {
    var existing =
        new ResolvedBlockTime(
            Instant.parse("2030-01-07T23:00:00Z"),
            Instant.parse("2030-01-08T01:00:00Z"),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            120);
    var outside =
        new ResolvedBlockTime(
            Instant.parse("2030-01-09T10:00:00Z"),
            Instant.parse("2030-01-09T11:00:00Z"),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var days =
        BlockBudget.calculate(
            preference("UTC", 60),
            Instant.parse("2030-01-07T23:30:00Z"),
            Instant.parse("2030-01-08T00:30:00Z"),
            List.of(existing, outside));
    assertThat(days).hasSize(2);
    for (var day : days) {
      assertThat(day.plannedSeconds()).isEqualTo(3600);
      assertThat(day.requestedSeconds()).isEqualTo(1800);
      assertThat(day.excessSeconds()).isEqualTo(1800);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "Etc/GMT+1,0001-01-01T00:00:00Z,0001-01-01T00:01:00Z,startLocal",
    "Etc/GMT-1,9999-12-31T22:59:00Z,9999-12-31T23:01:00Z,endLocal"
  })
  void s10_rejectsExposedBudgetDateOutsidePublicYears(
      String zone, String start, String end, String field) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                BlockBudget.calculate(
                    preference(zone, 120), Instant.parse(start), Instant.parse(end), List.of()))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors()).hasSize(1);
              assertThat(error.errors().getFirst().field()).isEqualTo(field);
              assertThat(error.errors().getFirst().code()).isEqualTo("OUT_OF_RANGE");
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2010-03-04T14:30:00Z,2010-03-04T15:30:00Z,3600",
    "2010-03-04T15:10:00Z,2010-03-04T15:40:00Z,1800"
  })
  void s11_handlesBackwardDateChangeUsingConsecutiveMidnightAnchors(
      String start, String end, long requested) {
    var days =
        BlockBudget.calculate(
            preference("Antarctica/Casey", 120),
            Instant.parse(start),
            Instant.parse(end),
            List.of());
    assertThat(days).containsExactly(new BudgetDay(LocalDate.of(2010, 3, 5), 120, 0, requested, 0));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "Europe/Madrid,2030-03-30T23:00:00Z,2030-03-31T22:00:00Z,2030-03-31,82800",
    "Europe/Madrid,2030-10-26T22:00:00Z,2030-10-27T22:00:00Z,2030-10-27,86400",
    "UTC,9999-12-31T23:58:00Z,9999-12-31T23:59:00Z,9999-12-31,60"
  })
  void s10_s11_usesActualDayAnchorsAndInternalNextYear(
      String zone, String start, String end, String date, long requested) {
    assertThat(
            BlockBudget.calculate(
                preference(zone, 1440), Instant.parse(start), Instant.parse(end), List.of()))
        .containsExactly(new BudgetDay(LocalDate.parse(date), 1440, 0, requested, 0));
  }

  @Test
  void s33_keepsPreviewAndPlanningSnapshotsImmutable() {
    var blocks = new ArrayList<PlannedBlock>();
    var context =
        new BlockPlanningContext("active", "pending", Optional.of(preference("UTC", 120)), blocks);
    var request =
        new BlockRequest(
            "Meta",
            LocalDateTime.parse("2030-01-07T10:00"),
            LocalDateTime.parse("2030-01-07T11:00"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var time = ResolvedBlockTime.resolve(request, Set.of("UTC"), Instant.EPOCH);
    blocks.add(
        new PlannedBlock(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), request, time, Instant.EPOCH));
    assertThat(context.blocks()).isEmpty();
    var days = new ArrayList<BudgetDay>();
    days.add(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 0, 3600, 0));
    var preview =
        new BlockPreview(
            request, time, new AvailabilityRevision(UUID.randomUUID(), 0), "UTC", days);
    days.clear();
    assertThat(preview.days()).hasSize(1);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> preview.days().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void s10_acceptsBudgetInFirstPublicYear() {
    assertThat(
            BlockBudget.calculate(
                preference("UTC", 120),
                Instant.parse("0001-01-01T10:00:00Z"),
                Instant.parse("0001-01-01T11:00:00Z"),
                List.of()))
        .containsExactly(new BudgetDay(LocalDate.of(1, 1, 1), 120, 0, 3600, 0));
  }
}
