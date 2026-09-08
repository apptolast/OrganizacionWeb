package com.apptolast.organization.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

final class CustomizationTime {
  private CustomizationTime() {}

  static Instant capture(Clock clock) {
    try {
      var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
      int year = now.atOffset(ZoneOffset.UTC).getYear();
      if (year < 1 || year > 9999)
        throw new IllegalStateException("Customization time outside public range");
      return now;
    } catch (RuntimeException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
