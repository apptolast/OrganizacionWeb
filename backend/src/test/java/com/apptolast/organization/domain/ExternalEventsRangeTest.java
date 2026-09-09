package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** @s32: el rango es obligatorio, va en UTC y no abarca más de dieciséis días. */
class ExternalEventsRangeTest {
  static final String START = "2030-01-07T00:00:00Z";

  @Test
  void s32_sixteenDaysIsTheLastAcceptedRange() {
    var range = ExternalEventsRange.of(START, "2030-01-23T00:00:00Z");
    assertEquals(Instant.parse(START), range.from());
    assertEquals(Instant.parse("2030-01-23T00:00:00Z"), range.to());
  }

  @ParameterizedTest
  @CsvSource({
    "2030-01-23T00:00:01Z",
    "2030-01-07T00:00:00Z",
    "2030-01-06T00:00:00Z",
    "2031-01-07T00:00:00Z"
  })
  void s32_anEndOutsideTheLimitsIsOutOfRange(String to) {
    assertEquals("to", only(START, to).field());
    assertEquals("OUT_OF_RANGE", only(START, to).code());
  }

  @Test
  void s32_oneSecondOfRangeIsEnough() {
    assertEquals(
        Instant.parse("2030-01-07T00:00:01Z"),
        ExternalEventsRange.of(START, "2030-01-07T00:00:01Z").to());
  }

  @ParameterizedTest
  @CsvSource({"from", "to"})
  void s32_bothEndsAreRequired(String missing) {
    var error = only("from".equals(missing) ? null : START, "to".equals(missing) ? null : START);
    assertEquals(missing, error.field());
    assertEquals("REQUIRED", error.code());
  }

  @ParameterizedTest
  @CsvSource({
    "2030-01-07",
    "2030-01-07T00:00:00+01:00",
    "2030-01-07T00:00:00.000Z",
    "2030-01-07T00:00Z",
    "no es un instante",
    "2030-01-07t00:00:00z"
  })
  void s32_theFormatIsTheUtcInstantOfTheApplication(String from) {
    var error = only(from, "2030-01-08T00:00:00Z");
    assertEquals("from", error.field());
    assertEquals("INVALID_FORMAT", error.code());
  }

  @Test
  void s32_animpossibleDateIsRejectedAsAFormatError() {
    assertEquals("INVALID_FORMAT", only("2030-02-30T00:00:00Z", "2030-01-08T00:00:00Z").code());
  }

  @Test
  void s32_aBadFormatHidesTheRangeCheck() {
    var errors =
        assertThrows(
                ValidationException.class,
                () -> ExternalEventsRange.of("2030-01-07", "2029-01-08T00:00:00Z"))
            .errors();
    assertEquals(1, errors.size());
    assertEquals("INVALID_FORMAT", errors.getFirst().code());
  }

  static FieldError only(String from, String to) {
    var errors =
        assertThrows(ValidationException.class, () -> ExternalEventsRange.of(from, to)).errors();
    assertEquals(1, errors.size(), errors.toString());
    return errors.getFirst();
  }
}
