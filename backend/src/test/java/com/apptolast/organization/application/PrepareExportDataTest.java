package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PrepareExportDataTest {
  @Test
  void s1_s14_clockIsReadOnceAfterSnapshotAndTruncatedToMicroseconds() {
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-08T01:02:03.123456789Z"));
    var file = mock(PreparedExport.class);
    ExportDataQueries source =
        (owner, timestamp) -> {
          assertThat(owner).isEqualTo("dueño");
          verifyNoInteractions(clock);
          assertThat(timestamp.get()).isEqualTo(Instant.parse("2026-09-08T01:02:03.123456Z"));
          return file;
        };
    assertThat(new PrepareExportData(source, clock).prepare("dueño")).isSameAs(file);
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }

  @Test
  void s14_clockBeforeCivilYearOneCannotPrepareAnExport() {
    ExportDataQueries source =
        (owner, timestamp) -> {
          timestamp.get();
          return mock(PreparedExport.class);
        };
    var clock = Clock.fixed(Instant.parse("0000-12-31T23:59:59.999999Z"), java.time.ZoneOffset.UTC);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new PrepareExportData(source, clock).prepare("owner-a"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s14_clockAfterCivilYear9999CannotPrepareAnExport() {
    ExportDataQueries source =
        (owner, timestamp) -> {
          timestamp.get();
          return mock(PreparedExport.class);
        };
    var clock = Clock.fixed(Instant.parse("+10000-01-01T00:00:00Z"), java.time.ZoneOffset.UTC);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new PrepareExportData(source, clock).prepare("owner-a"))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
