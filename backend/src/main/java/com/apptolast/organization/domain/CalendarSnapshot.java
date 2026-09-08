package com.apptolast.organization.domain;

import java.util.List;
import java.util.Optional;

public record CalendarSnapshot(Optional<String> zoneId, List<CalendarEntry> entries) {
  public CalendarSnapshot {
    entries = List.copyOf(entries);
  }
}
