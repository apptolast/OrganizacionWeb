package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class TodayWindowTest {
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");

  static Availability availability(String zone, int budget) {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, budget);
    return new Availability(UUID.randomUUID(), "persona-a", zone, days, 0, NOW, NOW);
  }

  @Test
  void s1_emptyDayKeepsKnownCapacityAndNoCandidates() {
    var today =
        TodayWindow.at(NOW, Optional.of(availability("UTC", 120)), Set.of("UTC"))
            .summarize(List.of());
    assertThat(today.window().serverNow()).isEqualTo(NOW);
    assertThat(today.window().date()).isEqualTo(LocalDate.of(2030, 1, 7));
    assertThat(today.window().zoneId()).isEqualTo("UTC");
    assertThat(today.window().zoneSource()).isEqualTo("AVAILABILITY");
    assertThat(today.window().availabilityZoneId()).isEqualTo("UTC");
    assertThat(today.window().dayStartAt()).isEqualTo(Instant.parse("2030-01-07T00:00:00Z"));
    assertThat(today.window().dayEndAt()).isEqualTo(Instant.parse("2030-01-08T00:00:00Z"));
    assertThat(today.window().budgetMinutes()).isEqualTo(120);
    assertThat(today.plannedSeconds()).isZero();
    assertThat(today.remainingSeconds()).isEqualTo(7200L);
    assertThat(today.excessSeconds()).isZero();
    assertThat(today.currentBlockId()).isNull();
    assertThat(today.nextBlockId()).isNull();
    assertThat(today.closingAt()).isNull();
    assertThat(today.items()).isEmpty();
  }

  static TodayItem block(String start, String end) {
    var from = Instant.parse(start);
    var to = Instant.parse(end);
    var request =
        new BlockRequest(
            "Meta",
            LocalDateTime.ofInstant(from, ZoneOffset.UTC),
            LocalDateTime.ofInstant(to, ZoneOffset.UTC),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            true);
    return new TodayItem(
        new PlannedBlock(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            request,
            new ResolvedBlockTime(
                from,
                to,
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                (int) Duration.between(from, to).toMinutes()),
            NOW),
        "Proyecto",
        "Tarea");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "120,60,3600,0",
    "120,120,0,0",
    "120,150,0,1800",
    "0,60,0,3600"
  })
  void s2_capacityUsesReservedSeconds(int budget, int duration, long remaining, long excess) {
    var item =
        block(
            "2030-01-07T08:00:00Z",
            Instant.parse("2030-01-07T08:00:00Z").plusSeconds(duration * 60L).toString());
    var result =
        TodayWindow.at(NOW, Optional.of(availability("UTC", budget)), Set.of("UTC"))
            .summarize(List.of(item));
    assertThat(result.plannedSeconds()).isEqualTo(duration * 60L);
    assertThat(result.remainingSeconds()).isEqualTo(remaining);
    assertThat(result.excessSeconds()).isEqualTo(excess);
    assertThat(result.items()).containsExactly(item);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"UNCONFIGURED", "UNAVAILABLE"})
  void s3_fallbackRetainsBlocksWithoutInventingBudget(String source) {
    var preference =
        source.equals("UNCONFIGURED")
            ? Optional.<Availability>empty()
            : Optional.of(availability("Legacy/Retired", 120));
    var item = block("2030-01-07T10:00:00Z", "2030-01-07T11:00:00Z");
    var result = TodayWindow.at(NOW, preference, Set.of("UTC")).summarize(List.of(item));
    assertThat(result.window().zoneId()).isEqualTo("UTC");
    assertThat(result.window().zoneSource()).isEqualTo(source);
    assertThat(result.window().availabilityZoneId())
        .isEqualTo(preference.map(Availability::zoneId).orElse(null));
    assertThat(result.window().date()).isEqualTo(LocalDate.of(2030, 1, 7));
    assertThat(result.window().budgetMinutes()).isNull();
    assertThat(result.remainingSeconds()).isNull();
    assertThat(result.excessSeconds()).isNull();
    assertThat(result.plannedSeconds()).isEqualTo(3600);
    assertThat(result.items()).containsExactly(item);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2030-01-06T23:00:00Z,2030-01-07T00:00:00Z,0,0",
    "2030-01-06T23:30:00Z,2030-01-07T00:30:00Z,1,1800",
    "2030-01-07T10:00:00Z,2030-01-07T11:00:00Z,1,3600",
    "2030-01-07T23:30:00Z,2030-01-08T00:30:00Z,1,1800",
    "2030-01-08T00:00:00Z,2030-01-08T01:00:00Z,0,0"
  })
  void s4_countsOnlyPositiveIntersectionsWithoutClippingStoredBlock(
      String start, String end, int count, long seconds) {
    var item = block(start, end);
    var result =
        TodayWindow.at(NOW, Optional.of(availability("UTC", 120)), Set.of("UTC"))
            .summarize(List.of(item));
    assertThat(result.items()).hasSize(count);
    assertThat(result.plannedSeconds()).isEqualTo(seconds);
    if (count == 1) assertThat(result.items()).containsExactly(item);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2026-03-29T12:00:00Z,2026-03-29,2026-03-28T23:00:00Z,2026-03-29T22:00:00Z,23",
    "2026-10-25T12:00:00Z,2026-10-25,2026-10-24T22:00:00Z,2026-10-25T23:00:00Z,25",
    "2026-10-26T12:00:00Z,2026-10-26,2026-10-25T23:00:00Z,2026-10-26T23:00:00Z,24"
  })
  void s5_usesRealLocalMidnightsAndWeekdayBudget(
      String now, String date, String start, String end, long hours) {
    var base = availability("Europe/Madrid", 120);
    var budgets = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    budgets.putAll(base.dailyMinutes());
    budgets.put(DayOfWeek.SUNDAY, 45);
    var availability =
        new Availability(base.id(), base.ownerId(), base.zoneId(), budgets, 0, NOW, NOW);
    var result =
        TodayWindow.at(Instant.parse(now), Optional.of(availability), Set.of("Europe/Madrid"));
    assertThat(result.date()).isEqualTo(LocalDate.parse(date));
    assertThat(result.dayStartAt()).isEqualTo(Instant.parse(start));
    assertThat(result.dayEndAt()).isEqualTo(Instant.parse(end));
    assertThat(Duration.between(result.dayStartAt(), result.dayEndAt()).toHours()).isEqualTo(hours);
    assertThat(result.budgetMinutes())
        .isEqualTo(result.date().getDayOfWeek() == DayOfWeek.SUNDAY ? 45 : 120);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"2030-01-07T23:45:00Z", "2030-01-08T00:15:00Z"})
  void s6_midnightBlockKeepsItsActualClosingTime(String now) {
    var item = block("2030-01-07T23:30:00Z", "2030-01-08T00:30:00Z");
    var result =
        TodayWindow.at(Instant.parse(now), Optional.of(availability("UTC", 120)), Set.of("UTC"))
            .summarize(List.of(item));
    assertThat(result.items()).containsExactly(item);
    assertThat(result.plannedSeconds()).isEqualTo(1800);
    assertThat(result.closingAt()).isEqualTo(item.block().time().endAt());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "08:59:59,-1,0",
    "09:00:00,0,1",
    "10:00:00,1,2",
    "11:00:00,-1,2",
    "13:00:00,-1,-1"
  })
  void s7_currentAndNextUseSemiOpenBoundaries(String time, int current, int next) {
    var a = block("2030-01-07T09:00:00Z", "2030-01-07T10:00:00Z");
    var b = block("2030-01-07T10:00:00Z", "2030-01-07T11:00:00Z");
    var c = block("2030-01-07T12:00:00Z", "2030-01-07T13:00:00Z");
    var ordered = List.of(a, b, c);
    var result =
        TodayWindow.at(
                Instant.parse("2030-01-07T" + time + "Z"),
                Optional.of(availability("UTC", 120)),
                Set.of("UTC"))
            .summarize(List.of(c, a, b));
    assertThat(result.currentBlockId())
        .isEqualTo(current < 0 ? null : ordered.get(current).block().id());
    assertThat(result.nextBlockId()).isEqualTo(next < 0 ? null : ordered.get(next).block().id());
    assertThat(result.closingAt()).isEqualTo(c.block().time().endAt());
    assertThat(result.items()).containsExactlyElementsOf(ordered);
  }
}
