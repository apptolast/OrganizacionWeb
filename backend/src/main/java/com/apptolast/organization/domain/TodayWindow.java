package com.apptolast.organization.domain;

import java.time.*;
import java.util.*;

public record TodayWindow(
    Instant serverNow,
    LocalDate date,
    String zoneId,
    String zoneSource,
    String availabilityZoneId,
    Instant dayStartAt,
    Instant dayEndAt,
    Integer budgetMinutes) {
  public static TodayWindow at(Instant now, Optional<Availability> preference, Set<String> zones) {
    var storedZone = preference.map(Availability::zoneId).orElse(null);
    boolean configured = storedZone != null && zones.contains(storedZone);
    var zone = ZoneId.of(configured ? storedZone : "UTC");
    var date = now.atZone(zone).toLocalDate();
    return new TodayWindow(
        now,
        date,
        zone.getId(),
        configured ? "AVAILABILITY" : storedZone == null ? "UNCONFIGURED" : "UNAVAILABLE",
        storedZone,
        date.atStartOfDay(zone).toInstant(),
        date.plusDays(1).atStartOfDay(zone).toInstant(),
        configured ? preference.orElseThrow().dailyMinutes().get(date.getDayOfWeek()) : null);
  }

  public Agenda summarize(List<TodayItem> items) {
    var included =
        items.stream()
            .filter(
                item ->
                    item.block().time().startAt().isBefore(dayEndAt)
                        && item.block().time().endAt().isAfter(dayStartAt))
            .sorted(Comparator.comparing((TodayItem item) -> item.block().time().startAt()))
            .toList();
    long planned =
        included.stream()
            .mapToLong(
                item ->
                    Duration.between(
                            item.block().time().startAt().isAfter(dayStartAt)
                                ? item.block().time().startAt()
                                : dayStartAt,
                            item.block().time().endAt().isBefore(dayEndAt)
                                ? item.block().time().endAt()
                                : dayEndAt)
                        .getSeconds())
            .sum();
    return new Agenda(
        this,
        planned,
        budgetMinutes == null ? null : Math.max(budgetMinutes * 60L - planned, 0),
        budgetMinutes == null ? null : Math.max(planned - budgetMinutes * 60L, 0),
        included.stream()
            .filter(
                item ->
                    !item.block().time().startAt().isAfter(serverNow)
                        && item.block().time().endAt().isAfter(serverNow))
            .map(item -> item.block().id())
            .findFirst()
            .orElse(null),
        included.stream()
            .filter(item -> item.block().time().startAt().isAfter(serverNow))
            .map(item -> item.block().id())
            .findFirst()
            .orElse(null),
        included.stream()
            .map(item -> item.block().time().endAt())
            .max(Comparator.naturalOrder())
            .orElse(null),
        included);
  }

  public record Agenda(
      TodayWindow window,
      long plannedSeconds,
      Long remainingSeconds,
      Long excessSeconds,
      UUID currentBlockId,
      UUID nextBlockId,
      Instant closingAt,
      List<TodayItem> items) {}
}
