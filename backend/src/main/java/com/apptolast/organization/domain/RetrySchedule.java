package com.apptolast.organization.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Fixed back-off after each failed attempt; the sixth failure exhausts the delivery. */
public final class RetrySchedule {
  public static final int MAX_ATTEMPTS = 6;
  private static final List<Duration> DELAYS =
      List.of(
          Duration.ofMinutes(1),
          Duration.ofMinutes(5),
          Duration.ofMinutes(30),
          Duration.ofHours(2),
          Duration.ofHours(24));

  private RetrySchedule() {}

  public static Optional<Instant> nextAttemptAt(int failedAttempts, Instant now) {
    if (failedAttempts >= MAX_ATTEMPTS) return Optional.empty();
    return Optional.of(now.plus(DELAYS.get(failedAttempts - 1)));
  }
}
