package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlannedBlockTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "id",
        "project",
        "task",
        "request",
        "time",
        "created",
        "microseconds",
        "offset",
        "instant"
      })
  void s2_s21_reconstructionCannotCorruptStoredIdentityOrIntent(String defect) {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "Meta",
            start,
            start.plusHours(1),
            "Historical/Removed",
            defect.equals("offset") ? null : ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var instant = Instant.parse("2030-01-07T10:00:00Z");
    var time =
        new ResolvedBlockTime(
            instant.plusSeconds(defect.equals("instant") ? 60 : 0),
            instant.plusSeconds(defect.equals("instant") ? 3660 : 3600),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    assertThatThrownBy(
            () ->
                new PlannedBlock(
                    defect.equals("id") ? null : UUID.randomUUID(),
                    defect.equals("project") ? null : UUID.randomUUID(),
                    defect.equals("task") ? null : UUID.randomUUID(),
                    defect.equals("request") ? null : request,
                    defect.equals("time") ? null : time,
                    defect.equals("created")
                        ? null
                        : Instant.EPOCH.plusNanos(defect.equals("microseconds") ? 1 : 0)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
