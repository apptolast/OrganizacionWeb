package com.apptolast.organization.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/** Ventana almacenada: intersección semiabierta con [syncAt - 24 h, syncAt + 336 h), máximo 500. */
public record ExternalCalendarSnapshot(List<ExternalEvent> events, boolean truncated) {
  public static final Duration BEFORE = Duration.ofHours(24);
  public static final Duration AFTER = Duration.ofHours(336);
  public static final int LIMIT = 500;

  public static ExternalCalendarSnapshot select(List<ExternalEvent> candidates, Instant syncAt) {
    var from = syncAt.minus(BEFORE);
    var to = syncAt.plus(AFTER);
    var selected =
        candidates.stream()
            .filter(event -> event.startAt().isBefore(to) && event.endAt().isAfter(from))
            .sorted(Comparator.comparing(ExternalEvent::startAt).thenComparing(ExternalEvent::uid))
            .toList();
    boolean truncated = selected.size() > LIMIT;
    return new ExternalCalendarSnapshot(
        truncated ? selected.subList(0, LIMIT) : selected, truncated);
  }
}
