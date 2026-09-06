package com.apptolast.organization.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Availability(
    UUID id,
    String ownerId,
    String zoneId,
    Map<DayOfWeek, Integer> dailyMinutes,
    long version,
    Instant createdAt,
    Instant updatedAt) {
  public Availability {
    if (id == null
        || ownerId == null
        || ownerId.isBlank()
        || zoneId == null
        || zoneId.isBlank()
        || dailyMinutes == null
        || !dailyMinutes.keySet().equals(java.util.EnumSet.allOf(DayOfWeek.class))
        || dailyMinutes.values().stream()
            .anyMatch(value -> value == null || value < 0 || value > 1440)
        || version < 0
        || createdAt == null
        || updatedAt == null
        || updatedAt.isBefore(createdAt))
      throw new IllegalArgumentException(
          "Availability requires valid identity, seven budgets, revision and dates");
    dailyMinutes = Map.copyOf(dailyMinutes);
  }

  public int weeklyMinutes() {
    return dailyMinutes.values().stream().mapToInt(Integer::intValue).sum();
  }
}
