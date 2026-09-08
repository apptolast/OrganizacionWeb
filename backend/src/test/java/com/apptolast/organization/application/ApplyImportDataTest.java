package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplyImportDataTest {
  @Test
  void s1_recordedAtIsReadInsideTheStoreOperationAndTruncatedBeforeReturningReceipt() {
    var key = UUID.fromString("00000000-0000-0000-0000-000000000123");
    var body = new ByteArrayInputStream(new byte[0]);
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-08T01:02:03.123456789Z"));
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    var expected =
        new ImportReceipt(
            key,
            "a".repeat(64),
            0,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            "NO_CHANGE",
            zero,
            zero);
    ImportDataCommands commands =
        (owner, requestKey, sha, input, timestamp) -> {
          assertThat(owner).isEqualTo("owner-a");
          assertThat(requestKey).isEqualTo(key);
          assertThat(sha).isEqualTo("a".repeat(64));
          assertThat(input).isSameAs(body);
          verifyNoInteractions(clock);
          assertThat(timestamp.get()).isEqualTo(expected.recordedAt());
          return expected;
        };
    ApplyImportDataUseCase useCase = new ApplyImportData(commands, clock);
    assertThat(useCase.apply("owner-a", key, "a".repeat(64), body)).isSameAs(expected);
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
  }
}
