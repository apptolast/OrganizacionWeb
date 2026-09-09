package com.apptolast.organization.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * The fixed slice of time a feed publishes: thirty days back and a year forward, semi-open at both
 * ends so a block belongs when it ends after the start and starts before the end.
 */
public record CalendarWindow(Instant from, Instant to) {
  private static final Duration BACK = Duration.ofDays(30);
  private static final Duration FORWARD = Duration.ofDays(365);

  public static CalendarWindow around(Instant now) {
    return new CalendarWindow(now.minus(BACK), now.plus(FORWARD));
  }

  public boolean covers(Instant startAt, Instant endAt) {
    return endAt.isAfter(from) && startAt.isBefore(to);
  }
}
