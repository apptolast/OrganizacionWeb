package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RetryScheduleTest {
  private static final Instant T = Instant.parse("2026-09-08T12:00:00Z");

  @ParameterizedTest
  @CsvSource({"1,PT1M", "2,PT5M", "3,PT30M", "4,PT2H", "5,PT24H"})
  void s24_failedAttemptsOneToFiveScheduleTheFixedTable(int attempt, String delay) {
    assertEquals(
        Optional.of(T.plus(Duration.parse(delay))), RetrySchedule.nextAttemptAt(attempt, T));
  }

  @Test
  void s24_sixthFailureExhaustsTheDelivery() {
    assertEquals(Optional.empty(), RetrySchedule.nextAttemptAt(6, T));
    assertEquals(6, RetrySchedule.MAX_ATTEMPTS);
  }
}
