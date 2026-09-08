package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreviewImportDataTest {
  @Test
  void s1_s18_emptyPreviewPreservesThePreparedSnapshotWithoutOwningTheRequestStream() {
    var body = new ByteArrayInputStream("archivo UTF-8 propio".getBytes(StandardCharsets.UTF_8));
    var counts = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    var expected =
        new ImportPreview(
            "a".repeat(64),
            20,
            "owner-a",
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            counts,
            counts,
            counts,
            List.of());
    int[] comparisons = {0};
    ImportDataQueries queries =
        (owner, input) -> {
          assertThat(owner).isEqualTo("owner-a");
          assertThat(input).isSameAs(body);
          comparisons[0]++;
          return expected;
        };

    ImportDataUseCase useCase = new PreviewImportData(queries);
    assertThat(useCase.preview("owner-a", body)).isSameAs(expected);
    assertThat(comparisons[0]).isEqualTo(1);
    assertThat(body.available()).isEqualTo(20);
  }
}
