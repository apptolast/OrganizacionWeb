package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadImportReceiptTest {
  @Test
  void s22_receiptIsReadWithTheAuthenticatedOwnerWithoutReapplyingTheFile() {
    var key = UUID.fromString("00000000-0000-0000-0000-000000000123");
    var zero = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    var receipt =
        new ImportReceipt(
            key,
            "a".repeat(64),
            1500,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            "NO_CHANGE",
            zero,
            zero);
    ImportReceiptQueries queries =
        (owner, requestKey) -> {
          assertThat(owner).isEqualTo("owner-a");
          assertThat(requestKey).isEqualTo(key);
          return Optional.of(receipt);
        };
    ReadImportReceiptUseCase useCase = new ReadImportReceipt(queries);
    assertThat(useCase.find("owner-a", key)).containsSame(receipt);
  }
}
