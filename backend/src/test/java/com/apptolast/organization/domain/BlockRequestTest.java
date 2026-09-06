package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BlockRequestTest {
  @Test
  void s3_acceptsExactlyFiveHundredSupplementaryCodePoints() {
    var objective = "😀".repeat(500);
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "\u00a0" + objective + "\u2003", start, start.plusHours(1), "UTC", null, null, false);
    assertThat(request.objective()).isEqualTo(objective);
  }

  @Test
  void s3_normalizesUnicodeWhitespaceWithoutChangingInteriorOrLocalIntent() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var end = start.plusHours(1);
    var request =
        new BlockRequest(
            "\u00a0 Meta  \ud83d\ude00 \u2003",
            start,
            end,
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    assertThat(request.objective()).isEqualTo("Meta  \ud83d\ude00");
    assertThat(request.startLocal()).isEqualTo(start);
    assertThat(request.endLocal()).isEqualTo(end);
    assertThat(request.zoneId()).isEqualTo("UTC");
    assertThat(request.allowOverBudget()).isFalse();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "empty", "unicode", "long"})
  void s4_rejectsMissingOrTooLongObjective(String defect) {
    String objective =
        switch (defect) {
          case "null" -> null;
          case "empty" -> "";
          case "unicode" -> "\u00a0\u2003";
          default -> "\ud83d\ude00".repeat(501);
        };
    var start = LocalDateTime.parse("2030-01-07T10:00");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new BlockRequest(objective, start, start.plusHours(1), "UTC", null, null, false))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors()).hasSize(1);
              assertThat(error.errors().getFirst().field()).isEqualTo("objective");
              assertThat(error.errors().getFirst().code())
                  .isEqualTo(defect.equals("long") ? "TOO_LONG" : "REQUIRED");
              assertThat(error.errors().getFirst().message()).isNotBlank();
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "startNull",
        "endNull",
        "startYear",
        "endYear",
        "startSeconds",
        "endNanos",
        "zoneNull",
        "zoneBlank"
      })
  void s4_s5_reconstructionCannotBypassLocalIntentShape(String defect) {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var end = start.plusHours(1);
    var suppliedStart =
        switch (defect) {
          case "startNull" -> null;
          case "startYear" -> start.withYear(0);
          case "startSeconds" -> start.plusSeconds(1);
          default -> start;
        };
    var suppliedEnd =
        switch (defect) {
          case "endNull" -> null;
          case "endYear" -> end.withYear(10000);
          case "endNanos" -> end.plusNanos(1);
          default -> end;
        };
    String zone = defect.equals("zoneNull") ? null : defect.equals("zoneBlank") ? " " : "UTC";
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new BlockRequest("Meta", suppliedStart, suppliedEnd, zone, null, null, false))
        .isInstanceOf(ValidationException.class);
  }
}
