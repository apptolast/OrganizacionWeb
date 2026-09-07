package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ReadWeeklyReviewTest {
  @Test
  void s6_defaultWeekWhoseSundayExceedsYearRangeIsATemporalConflict() {
    WeeklyReviewQueries queries = (owner, window) -> window.apply(Optional.empty()).emptyReview();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(
                        queries,
                        Clock.fixed(Instant.parse("9999-12-31T12:00:00Z"), ZoneOffset.UTC),
                        mock(ZoneCatalog.class))
                    .get("owner", null, null))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s6_localClockBeyondYearRangeFailsEvenWithExplicitDate() {
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("Etc/GMT-14"));
    WeeklyReviewQueries queries = (owner, window) -> window.apply(Optional.empty()).emptyReview();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(
                        queries,
                        Clock.fixed(Instant.parse("9999-12-31T23:00:00Z"), ZoneOffset.UTC),
                        catalog)
                    .get("owner", LocalDate.parse("2026-09-07"), "Etc/GMT-14"))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s6_unrepresentableClockFailsEvenForAnExplicitSafeWeek() {
    WeeklyReviewQueries queries = (owner, window) -> window.apply(Optional.empty()).emptyReview();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(
                        queries,
                        Clock.fixed(Instant.parse("+10000-01-01T00:00:00Z"), ZoneOffset.UTC),
                        mock(ZoneCatalog.class))
                    .get("owner", LocalDate.parse("2026-09-07"), null))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s6_utcBoundaryBeforeYearOneIsATemporalConflict() {
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("Etc/GMT-14"));
    WeeklyReviewQueries queries = (owner, window) -> window.apply(Optional.empty()).emptyReview();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(
                        queries,
                        Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC),
                        catalog)
                    .get("owner", LocalDate.parse("0001-01-01"), "Etc/GMT-14"))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s6_explicitWeekBeyondCivilRangeFailsBeforeSnapshot() {
    var queries = mock(WeeklyReviewQueries.class);
    var clock = mock(Clock.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(queries, clock, mock(ZoneCatalog.class))
                    .get("owner", LocalDate.parse("9999-12-31"), null))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.ValidationException.class,
            failure ->
                assertThat(failure.errors())
                    .singleElement()
                    .satisfies(
                        error -> {
                          assertThat(error.field()).isEqualTo("date");
                          assertThat(error.code()).isEqualTo("INVALID_VALUE");
                        }));
    verifyNoInteractions(queries, clock);
  }

  @Test
  void s4_unknownExplicitZoneFailsBeforeQueriesOrClock() {
    var queries = mock(WeeklyReviewQueries.class);
    var clock = mock(Clock.class);
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC"));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(queries, clock, catalog).get("owner", null, " Europe/Madrid "))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.ValidationException.class,
            failure ->
                assertThat(failure.errors())
                    .singleElement()
                    .satisfies(
                        error -> {
                          assertThat(error.field()).isEqualTo("zoneId");
                          assertThat(error.code()).isEqualTo("INVALID_VALUE");
                        }));
    verifyNoInteractions(queries, clock);
  }

  @Test
  void s15_explicitDifferentZoneKeepsCapacityUnknown() {
    var now = Instant.parse("2026-09-09T12:00:00Z");
    var budgets = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budgets.put(day, 120);
    var preference =
        new com.apptolast.organization.domain.Availability(
            java.util.UUID.randomUUID(), "owner", "Europe/Madrid", budgets, 1, now, now);
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC", "Europe/Madrid"));
    WeeklyReviewQueries queries =
        (owner, window) -> window.apply(Optional.of(preference)).emptyReview();
    var result =
        new ReadWeeklyReview(queries, Clock.fixed(now, ZoneOffset.UTC), catalog)
            .get("owner", null, "UTC");
    assertThat(result.zoneId()).isEqualTo("UTC");
    assertThat(result.zoneSource()).isEqualTo("EXPLICIT");
    assertThat(result.availabilityZoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.days()).allSatisfy(day -> assertThat(day.capacityMicroseconds()).isNull());
    assertThat(result.totals().capacityMicroseconds()).isNull();
  }

  @Test
  void s2_unavailableStoredZoneFallsBackWithoutMovingItsBudget() {
    var now = Instant.parse("2026-09-09T12:00:00Z");
    var budgets = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budgets.put(day, 120);
    var preference =
        new com.apptolast.organization.domain.Availability(
            java.util.UUID.randomUUID(), "owner", "Unavailable/Zone", budgets, 1, now, now);
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC"));
    WeeklyReviewQueries queries =
        (owner, window) -> window.apply(Optional.of(preference)).emptyReview();
    var result =
        new ReadWeeklyReview(queries, Clock.fixed(now, ZoneOffset.UTC), catalog)
            .get("owner", null, null);
    assertThat(result.zoneId()).isEqualTo("UTC");
    assertThat(result.zoneSource()).isEqualTo("UNAVAILABLE");
    assertThat(result.availabilityZoneId()).isEqualTo("Unavailable/Zone");
    assertThat(result.days()).allSatisfy(day -> assertThat(day.capacityMicroseconds()).isNull());
    assertThat(result.totals().capacityMicroseconds()).isNull();
  }

  @Test
  void s2_storedZoneUsesItsLocalWeekAndCurrentCapacity() {
    var now = Instant.parse("2026-09-06T23:30:00Z");
    var budgets = new java.util.EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) budgets.put(day, 120);
    var preference =
        new com.apptolast.organization.domain.Availability(
            java.util.UUID.randomUUID(), "owner", "Europe/Madrid", budgets, 1, now, now);
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC", "Europe/Madrid"));
    WeeklyReviewQueries queries =
        (owner, window) -> window.apply(Optional.of(preference)).emptyReview();
    var result =
        new ReadWeeklyReview(queries, Clock.fixed(now, ZoneOffset.UTC), catalog)
            .get("owner", null, null);
    assertThat(result.weekStart()).isEqualTo(LocalDate.parse("2026-09-07"));
    assertThat(result.zoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.zoneSource()).isEqualTo("AVAILABILITY");
    assertThat(result.availabilityZoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.days())
        .allSatisfy(day -> assertThat(day.capacityMicroseconds()).isEqualTo(7_200_000_000L));
    assertThat(result.totals().capacityMicroseconds()).isEqualTo(50_400_000_000L);
  }

  @Test
  void s1_emptyWeekUsesOneClockOnlyAfterTheSnapshotCallback() {
    var snapshotEstablished = new AtomicBoolean();
    var clock = mock(Clock.class);
    when(clock.instant())
        .thenAnswer(
            call -> {
              assertThat(snapshotEstablished).isTrue();
              return Instant.parse("2026-09-09T12:00:00.123456789Z");
            });
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC"));
    WeeklyReviewQueries queries =
        (owner, window) -> {
          assertThat(owner).isEqualTo("owner");
          snapshotEstablished.set(true);
          return window.apply(Optional.empty()).emptyReview();
        };

    ReadWeeklyReviewUseCase useCase = new ReadWeeklyReview(queries, clock, catalog);
    var result = useCase.get("owner", null, null);

    assertThat(result.weekStart()).isEqualTo(LocalDate.parse("2026-09-07"));
    assertThat(result.weekEnd()).isEqualTo(LocalDate.parse("2026-09-13"));
    assertThat(result.serverNow()).isEqualTo(Instant.parse("2026-09-09T12:00:00.123456Z"));
    assertThat(result.zoneId()).isEqualTo("UTC");
    assertThat(result.zoneSource()).isEqualTo("UNCONFIGURED");
    assertThat(result.availabilityZoneId()).isNull();
    assertThat(result.startAt()).isEqualTo(Instant.parse("2026-09-07T00:00:00Z"));
    assertThat(result.endAt()).isEqualTo(Instant.parse("2026-09-14T00:00:00Z"));
    assertThat(result.days()).hasSize(7);
    for (int day = 0; day < 7; day++) {
      var value = result.days().get(day);
      var date = LocalDate.parse("2026-09-07").plusDays(day);
      assertThat(value.date()).isEqualTo(date);
      assertThat(value.startAt()).isEqualTo(date.atStartOfDay(ZoneOffset.UTC).toInstant());
      assertThat(value.endAt())
          .isEqualTo(date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
      assertThat(value.plannedMicroseconds()).isZero();
      assertThat(value.workedMicroseconds()).isZero();
      assertThat(value.capacityMicroseconds()).isNull();
    }
    assertThat(result.totals().plannedMicroseconds()).isZero();
    assertThat(result.totals().workedMicroseconds()).isZero();
    assertThat(result.totals().capacityMicroseconds()).isNull();
    assertThat(result.unquantifiedSessionCount()).isZero();
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }

  @Test
  void s2_explicitDateAndZoneSelectTheirCivilWeek() {
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC", "Europe/Madrid"));
    WeeklyReviewQueries queries = (owner, window) -> window.apply(Optional.empty()).emptyReview();
    var result =
        new ReadWeeklyReview(
                queries,
                Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC),
                catalog)
            .get("owner", LocalDate.parse("2026-03-29"), "Europe/Madrid");
    assertThat(result.weekStart()).isEqualTo(LocalDate.parse("2026-03-23"));
    assertThat(result.weekEnd()).isEqualTo(LocalDate.parse("2026-03-29"));
    assertThat(result.zoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.zoneSource()).isEqualTo("EXPLICIT");
    assertThat(result.availabilityZoneId()).isNull();
    assertThat(result.startAt()).isEqualTo(Instant.parse("2026-03-22T23:00:00Z"));
    assertThat(result.endAt()).isEqualTo(Instant.parse("2026-03-29T22:00:00Z"));
    assertThat(Duration.between(result.startAt(), result.endAt()).toHours()).isEqualTo(167);
  }
}
