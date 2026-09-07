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
}
