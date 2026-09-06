package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolvedBlockTimeTest {
  @Test
  void s1_s8_resolvesUnambiguousLocalEndpoints() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request = new BlockRequest("Meta", start, start.plusHours(1), "UTC", null, null, false);
    var result =
        ResolvedBlockTime.resolve(request, Set.of("UTC"), Instant.parse("2030-01-01T00:00:00Z"));
    assertThat(result.startAt()).isEqualTo(Instant.parse("2030-01-07T10:00:00Z"));
    assertThat(result.endAt()).isEqualTo(Instant.parse("2030-01-07T11:00:00Z"));
    assertThat(result.startOffset()).isEqualTo(ZoneOffset.UTC);
    assertThat(result.endOffset()).isEqualTo(ZoneOffset.UTC);
    assertThat(result.durationMinutes()).isEqualTo(60);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "Europe/Madrid,2030-03-31T02:15,2030-03-31T03:30,startLocal",
    "Europe/Madrid,2030-03-31T01:30,2030-03-31T02:15,endLocal",
    "Australia/Lord_Howe,2030-10-06T02:15,2030-10-06T03:00,startLocal"
  })
  void s8_rejectsGapsWithoutShifting(String zone, String start, String end, String field) {
    var request =
        new BlockRequest(
            "Meta", LocalDateTime.parse(start), LocalDateTime.parse(end), zone, null, null, false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of(zone), Instant.parse("2030-01-01T00:00:00Z")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors()).hasSize(1);
              assertThat(error.errors().getFirst().field()).isEqualTo(field);
              assertThat(error.errors().getFirst().code()).isEqualTo("NONEXISTENT_LOCAL_TIME");
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2030-10-27T02:15,2030-10-27T02:45,startOffset",
    "2030-10-27T01:30,2030-10-27T02:45,endOffset"
  })
  void s8_requiresExplicitOccurrenceWithOrderedOffsets(String start, String end, String field) {
    var request =
        new BlockRequest(
            "Meta",
            LocalDateTime.parse(start),
            LocalDateTime.parse(end),
            "Europe/Madrid",
            null,
            null,
            false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of("Europe/Madrid"), Instant.parse("2030-01-01T00:00:00Z")))
        .isInstanceOfSatisfying(
            BlockOffsetException.class,
            error -> {
              assertThat(error.error().field()).isEqualTo(field);
              assertThat(error.error().code()).isEqualTo("AMBIGUOUS_OFFSET");
              assertThat(error.validOffsets())
                  .containsExactly(ZoneOffset.of("+02:00"), ZoneOffset.of("+01:00"));
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"start,+02:00", "end,+02:00"})
  void s8_rejectsOffsetOutsideValidSet(String endpoint, String invalid) {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        new BlockRequest(
            "Meta",
            start,
            start.plusHours(1),
            "Europe/Madrid",
            ZoneOffset.of(endpoint.equals("start") ? invalid : "+01:00"),
            ZoneOffset.of(endpoint.equals("end") ? invalid : "+01:00"),
            false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of("Europe/Madrid"), Instant.parse("2030-01-01T00:00:00Z")))
        .isInstanceOfSatisfying(
            BlockOffsetException.class,
            error -> {
              assertThat(error.error().field()).isEqualTo(endpoint + "Offset");
              assertThat(error.error().code()).isEqualTo("INVALID_OFFSET");
              assertThat(error.validOffsets()).containsExactly(ZoneOffset.of("+01:00"));
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"zero", "negative", "long", "fraction"})
  void s6_rejectsNonPositiveLongAndFractionalRealDuration(String defect) {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request =
        defect.equals("fraction")
            ? new BlockRequest(
                "Meta",
                LocalDateTime.parse("1972-01-06T23:30"),
                LocalDateTime.parse("1972-01-07T01:15"),
                "Africa/Monrovia",
                ZoneOffset.of("-00:44:30"),
                ZoneOffset.UTC,
                false)
            : new BlockRequest(
                "Meta",
                start,
                start.plusMinutes(
                    defect.equals("zero") ? 0 : defect.equals("negative") ? -1 : 1441),
                "UTC",
                null,
                null,
                false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of(request.zoneId()), Instant.parse("1900-01-01T00:00:00Z")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("endLocal");
              assertThat(error.errors().getFirst().code()).isEqualTo("OUT_OF_RANGE");
            });
  }

  @Test
  void s7_rejectsPastStartUsingServerClock() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request = new BlockRequest("Meta", start, start.plusHours(1), "UTC", null, null, false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of("UTC"), Instant.parse("2030-01-07T10:00:00.000001Z")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("startLocal");
              assertThat(error.errors().getFirst().code()).isEqualTo("IN_PAST");
            });
  }

  @Test
  void s9_doesNotResolveZoneOutsideCurrentCatalog() {
    var start = LocalDateTime.parse("2030-01-07T10:00");
    var request = new BlockRequest("Meta", start, start.plusHours(1), "UTC", null, null, false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                ResolvedBlockTime.resolve(
                    request, Set.of("Europe/Madrid"), Instant.parse("2030-01-01T00:00:00Z")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("zoneId");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "Etc/GMT-1,0001-01-01T00:00,0001-01-01T00:01,startLocal",
    "Etc/GMT+1,9999-12-31T22:59,9999-12-31T23:01,endLocal"
  })
  void s10_rejectsUtcYearsOutsidePublicRange(String zone, String start, String end, String field) {
    var request =
        new BlockRequest(
            "Meta", LocalDateTime.parse(start), LocalDateTime.parse(end), zone, null, null, false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ResolvedBlockTime.resolve(request, Set.of(zone), Instant.MIN))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo(field);
              assertThat(error.errors().getFirst().code()).isEqualTo("OUT_OF_RANGE");
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "startNull",
        "endNull",
        "startOffsetNull",
        "endOffsetNull",
        "durationMismatch",
        "fractional",
        "year"
      })
  void s6_s10_reconstructedTimeRetainsCoherentInstants(String defect) {
    var start = Instant.parse("2030-01-07T10:00:00Z");
    var suppliedStart =
        defect.equals("startNull")
            ? null
            : defect.equals("year") ? Instant.parse("0000-01-07T10:00:00Z") : start;
    var suppliedEnd =
        defect.equals("endNull")
            ? null
            : defect.equals("fractional")
                ? start.plusSeconds(3600).plusNanos(1)
                : start.plusSeconds(3600);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ResolvedBlockTime(
                    suppliedStart,
                    suppliedEnd,
                    defect.equals("startOffsetNull") ? null : ZoneOffset.UTC,
                    defect.equals("endOffsetNull") ? null : ZoneOffset.UTC,
                    defect.equals("durationMismatch") ? 61 : 60))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "UTC,2030-01-07T10:00,2030-01-07T10:01,Z,Z,1,2030-01-07T10:00:00Z",
    "UTC,2030-01-07T10:00,2030-01-08T10:00,Z,Z,1440,2030-01-07T10:00:00Z",
    "Australia/Lord_Howe,2030-04-07T01:45,2030-04-07T01:45,+11:00,+10:30,30,2030-04-06T00:00:00Z",
    "Europe/Madrid,2030-10-27T02:45,2030-10-27T02:15,+02:00,+01:00,30,2030-10-26T00:00:00Z",
    "Europe/Paris,1900-01-01T10:00,1900-01-01T10:01,+00:09:21,+00:09:21,1,1899-12-31T00:00:00Z",
    "UTC,0001-01-01T00:00,0001-01-01T00:01,Z,Z,1,0001-01-01T00:00:00Z",
    "UTC,9999-12-31T23:58,9999-12-31T23:59,Z,Z,1,9999-12-31T23:58:00Z"
  })
  void s6_s7_s8_s10_acceptsExactBoundariesAndHistoricalOffsets(
      String zone,
      String start,
      String end,
      String first,
      String last,
      int minutes,
      String observed) {
    var request =
        new BlockRequest(
            "Meta",
            LocalDateTime.parse(start),
            LocalDateTime.parse(end),
            zone,
            ZoneOffset.of(first),
            ZoneOffset.of(last),
            false);
    var time = ResolvedBlockTime.resolve(request, java.util.Set.of(zone), Instant.parse(observed));
    assertThat(time.durationMinutes()).isEqualTo(minutes);
    assertThat(time.startAt()).isEqualTo(request.startLocal().toInstant(request.startOffset()));
    assertThat(time.endAt()).isEqualTo(request.endLocal().toInstant(request.endOffset()));
    assertThat(time.startOffset()).isEqualTo(request.startOffset());
    assertThat(time.endOffset()).isEqualTo(request.endOffset());
  }
}
