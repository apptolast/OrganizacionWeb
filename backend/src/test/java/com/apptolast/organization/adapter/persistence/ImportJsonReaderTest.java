package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ImportJsonReaderTest {
  @Test
  void s8_unknownEnvelopeFieldCannotBeUsedAsAnImport() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    String json = original.toString(java.nio.charset.StandardCharsets.UTF_8);
    var body =
        new ByteArrayInputStream(
            (json.substring(0, json.length() - 1) + ",\"secret\":\"private\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ImportJsonReader().read(body, (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class)
        .hasMessage("El archivo de importación no es válido.");
  }

  @Test
  void s16_s17_bodyLimitStopsAtFirstExcessByteWithoutMaterializingAnUnboundedDocument() {
    long[] consumed = {0};
    var body =
        new java.io.InputStream() {
          @Override
          public int read() {
            consumed[0]++;
            return ' ';
          }

          @Override
          public int read(byte[] bytes, int offset, int length) {
            java.util.Arrays.fill(bytes, offset, offset + length, (byte) ' ');
            consumed[0] += length;
            return length;
          }
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(body, (collection, row) -> fail("No record before JSON")))
        .isInstanceOf(com.apptolast.organization.application.ImportTooLargeException.class);
    assertThat(consumed[0]).isEqualTo(33_554_433L);
  }

  @Test
  void s1_s18_emptyExportIsReadIncrementallyWithItsExactOriginalHashAndFourteenCounts()
      throws Exception {
    var original = new ByteArrayOutputStream();
    var exportedAt = Instant.parse("2026-09-08T01:02:03.123456Z");
    new ExportJsonWriter().empty("dueño", exportedAt).writeTo(original);
    original.writeBytes(" \n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    byte[] bytes = original.toByteArray();
    boolean[] closed = {false};
    var body =
        new ByteArrayInputStream(bytes) {
          @Override
          public void close() {
            closed[0] = true;
          }
        };

    var result =
        new ImportJsonReader().read(body, (collection, row) -> fail("Empty export emitted a row"));

    assertThat(result.owner()).isEqualTo("dueño");
    assertThat(result.exportedAt()).isEqualTo(exportedAt);
    assertThat(result.byteLength()).isEqualTo(bytes.length);
    assertThat(result.fileSha256())
        .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
    assertThat(result.counts())
        .isEqualTo(
            new com.apptolast.organization.application.ImportCounts(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    assertThat(body.available()).isZero();
    assertThat(closed[0]).isFalse();
  }
}
