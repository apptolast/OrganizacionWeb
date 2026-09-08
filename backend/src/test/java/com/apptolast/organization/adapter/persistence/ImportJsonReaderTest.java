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
  void s8_malformedUtf8IsAFileErrorRatherThanAStorageFailure() {
    var body =
        new ByteArrayInputStream(new byte[] {'{', '"', (byte) 0xc3, '(', '"', ':', '0', '}'});
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ImportJsonReader().read(body, (collection, row) -> fail("No malformed row")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_utf8BomIsNotPartOfTheAcceptedExportFormat() throws Exception {
    var original = new ByteArrayOutputStream();
    original.writeBytes(new byte[] {(byte) 0xef, (byte) 0xbb, (byte) 0xbf});
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(original.toByteArray()),
                        (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_eofWithinRecordArrayIsRejectedWithoutPreparingAnIncompleteRow() {
    var body =
        new ByteArrayInputStream(
            "{\"data\":{\"projects\":[{\"id\":\"partial\""
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(body, (collection, row) -> fail("No incomplete row at EOF")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_eofWithinDataObjectIsRejectedWithoutPreparingAnyRow() {
    var body =
        new ByteArrayInputStream(
            "{\"data\":{\"projects\":[]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ImportJsonReader().read(body, (collection, row) -> fail("No row at EOF")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_eofWithinRootIsRejectedWithoutPreparingAnyRow() {
    var body =
        new ByteArrayInputStream(
            "{\"owner\":\"owner-a\"".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ImportJsonReader().read(body, (collection, row) -> fail("No row at EOF")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_eofBeforeRootIsRejectedWithoutPreparingAnyRow() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(new byte[0]),
                        (collection, row) -> fail("No row at EOF")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_dataMustBeAnObjectBeforeAnyRowsArePrepared() {
    var body =
        new ByteArrayInputStream("{\"data\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(body, (collection, row) -> fail("Malformed data must not stage a row")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_declaredCountsMustEqualTheRecordsActuallyRead() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    String json =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(
                            json.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_secondDocumentAfterValidExportIsRejected() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    original.writeBytes(" {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(original.toByteArray()),
                        (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_duplicateRootPropertyIsRejectedWithoutLeakingItsValue() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    String json = original.toString(java.nio.charset.StandardCharsets.UTF_8);
    var body =
        new ByteArrayInputStream(
            (json.substring(0, json.length() - 1) + ",\"owner\":\"private-other\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ImportJsonReader().read(body, (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class)
        .hasMessage("El archivo de importación no es válido.");
  }

  @Test
  void s2_s18_nonemptyCollectionIsDeliveredOneRecordAtATimeWithExactText() throws Exception {
    var original = new ByteArrayOutputStream();
    var id = "00000000-0000-0000-0000-000000000123";
    new ExportJsonWriter()
        .prepare(
            "owner-a",
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            (collection, json) -> {
              if (!collection.equals("projects")) return 0;
              json.writeStartObject();
              json.writeStringField("id", id);
              json.writeStringField("name", "  proyecto ñ  ");
              json.writeStringField("description", "nota histórica");
              json.writeStringField("status", "idea");
              json.writeStringField("createdAt", "2026-09-07T01:02:03.123456Z");
              json.writeStringField("updatedAt", "2026-09-07T01:02:03.123456Z");
              json.writeStringField("version", "0");
              json.writeEndObject();
              return 1;
            })
        .writeTo(original);
    var records = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    var header =
        new ImportJsonReader()
            .read(
                new ByteArrayInputStream(original.toByteArray()),
                (collection, row) -> {
                  assertThat(collection).isEqualTo("projects");
                  records.add(row);
                });
    assertThat(records).hasSize(1);
    assertThat(records.getFirst().path("id").textValue()).isEqualTo(id);
    assertThat(records.getFirst().path("name").textValue()).isEqualTo("  proyecto ñ  ");
    assertThat(records.getFirst().path("version").textValue()).isEqualTo("0");
    assertThat(header.counts().projects()).isEqualTo(1);
  }

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
