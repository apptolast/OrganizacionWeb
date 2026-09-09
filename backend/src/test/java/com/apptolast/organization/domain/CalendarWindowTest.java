package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s18: the window is semi-open at both ends, measured from the injected clock.
 */
class CalendarWindowTest {
  private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

  @Test
  void s18_theWindowStartsThirtyDaysBackAndEndsThreeHundredSixtyFiveDaysForward() {
    var window = CalendarWindow.around(NOW);
    assertEquals(Instant.parse("2026-08-09T12:00:00Z"), window.from());
    assertEquals(Instant.parse("2027-09-08T12:00:00Z"), window.to());
  }

  @ParameterizedTest
  @CsvSource({
    "2026-08-09T11:00:00Z,2026-08-09T12:00:00Z,false",
    "2026-08-09T11:00:01Z,2026-08-09T12:00:01Z,true",
    "2027-09-08T12:00:00Z,2027-09-08T13:00:00Z,false",
    "2027-09-08T11:59:59Z,2027-09-08T12:59:59Z,true",
    "2026-08-01T00:00:00Z,2027-10-01T00:00:00Z,true",
    "2026-09-08T11:00:00Z,2026-09-08T12:00:00Z,true"
  })
  void s18_aBlockBelongsWhenItEndsAfterTheStartAndStartsBeforeTheEnd(
      String startAt, String endAt, boolean present) {
    assertEquals(
        present, CalendarWindow.around(NOW).covers(Instant.parse(startAt), Instant.parse(endAt)));
  }
}
