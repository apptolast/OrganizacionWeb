package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BlockMoveRequestTest {
  @ParameterizedTest
  @ValueSource(
      strings = {"startNull", "endNull", "startSeconds", "endYear", "zoneNull", "zoneBlank"})
  void s19_rejectsMalformedDestinationBeforeAnyBusinessOrReplay(String defect) {
    var start = LocalDateTime.parse("2030-01-07T12:00");
    var end = start.plusHours(1);
    var suppliedStart =
        defect.equals("startNull")
            ? null
            : defect.equals("startSeconds") ? start.plusSeconds(1) : start;
    var suppliedEnd =
        defect.equals("endNull") ? null : defect.equals("endYear") ? end.withYear(10000) : end;
    var zone =
        defect.equals("zoneNull") ? null : defect.equals("zoneBlank") ? " " : "Unknown/Historical";
    assertThatThrownBy(
            () -> new BlockMoveRequest(suppliedStart, suppliedEnd, zone, null, null, false))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors()).hasSize(1);
              var field =
                  defect.startsWith("start")
                      ? "startLocal"
                      : defect.startsWith("end") ? "endLocal" : "zoneId";
              assertThat(error.errors().getFirst().field()).isEqualTo(field);
              assertThat(error.errors().getFirst().code())
                  .isEqualTo(field.equals("zoneId") ? "INVALID_VALUE" : "INVALID_FORMAT");
            });
  }
}
