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
    ExportDataQueries source = (owner, timestamp) -> {
      assertThat(owner).isEqualTo("dueño");
      verifyNoInteractions(clock);
      assertThat(timestamp.get()).isEqualTo(Instant.parse("2026-09-08T01:02:03.123456Z"));
      return file;
    };
    assertThat(new PrepareExportData(source, clock).prepare("dueño")).isSameAs(file);
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}