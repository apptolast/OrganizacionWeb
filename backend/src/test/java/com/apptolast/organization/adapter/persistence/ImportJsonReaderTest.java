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
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"trailing", "cancel", "leading", "zero"})
  void s17_longEquivalentIntegersAreReducedBeforeNumericMaterialization(String kind)
      throws Exception {
    var number =
        switch (kind) {
          case "trailing" -> "-1." + "0".repeat(1100);
          case "cancel" -> "1" + "0".repeat(1100) + "e-1100";
          case "leading" -> "0." + "0".repeat(1100) + "1e1101";
          default -> "-0." + "0".repeat(1100) + "e" + "9".repeat(1100);
        };
    var expected = kind.equals("zero") ? "0" : kind.equals("trailing") ? "-1" : "1";
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace("\"projects\":[]", "\"projects\":[{\"number\":" + number + "}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var staged = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    new ImportJsonReader()
        .read(new ByteArrayInputStream(bytes), (collection, row) -> staged.add(row));
    assertThat(staged).hasSize(1);
    var value = staged.getFirst().get("number").decimalValue();
    assertThat(value).isEqualByComparingTo(expected);
    assertThat(value.precision()).isLessThanOrEqualTo(19);
  }

  @Test
  void s17_longFractionCannotBeRoundedOrPreparedAsAnUnboundedDecimal() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace("\"projects\":[]", "\"projects\":[{\"number\":1." + "0".repeat(1100) + "1}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var prepared = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> prepared.incrementAndGet()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(prepared.get()).isZero();
  }

  @Test
  void s17_oversizedIntegerTokenIsNotPreparedAsABigInteger() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace("\"projects\":[]", "\"projects\":[{\"number\":" + "1".repeat(1101) + "}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var prepared = new java.util.concurrent.atomic.AtomicInteger();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> prepared.incrementAndGet()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
    assertThat(prepared.get()).isZero();
  }

  @Test
  void s21_wrongHashPrecedesAValidJsonWithWrongEnvelopeType() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(
                            "[]".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        (collection, row) -> fail("No data envelope"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"1.0", "1e0"})
  void s8_integerEnvelopeNumbersUseTheirMathematicalValue(String one) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"schemaVersion\":1", "\"schemaVersion\":" + one)
            .replace("\"projects\":0", "\"projects\":" + one)
            .replace("\"tasks\":0", "\"tasks\":0e2147483649")
            .replace("\"projects\":[]", "\"projects\":[{}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var prepared = new java.util.concurrent.atomic.AtomicInteger();
    var result =
        new ImportJsonReader()
            .read(new ByteArrayInputStream(bytes), (collection, row) -> prepared.incrementAndGet());
    assertThat(result.counts().projects()).isEqualTo(1);
    assertThat(result.counts().tasks()).isZero();
    assertThat(prepared.get()).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "0e2147483649,true",
    "-0e-2147483649,true",
    "0e2147483649,false"
  })
  void s9_zeroWithExtremeExponentRemainsExactForTheRowValidator(String number, boolean matchingHash)
      throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projectCustomFieldValues\":0", "\"projectCustomFieldValues\":1")
            .replace(
                "\"projectCustomFieldValues\":[]",
                "\"projectCustomFieldValues\":[{\"value\":" + number + "}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var staged = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    var hash =
        matchingHash
            ? HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
            : "0".repeat(64);
    if (matchingHash)
      new ImportJsonReader()
          .read(new ByteArrayInputStream(bytes), (collection, row) -> staged.add(row), hash);
    else
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ImportJsonReader()
                      .read(
                          new ByteArrayInputStream(bytes),
                          (collection, row) -> staged.add(row),
                          hash))
          .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
    assertThat(staged).hasSize(1);
    assertThat(staged.getFirst().get("value").decimalValue())
        .isEqualByComparingTo(java.math.BigDecimal.ZERO);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"0001-01-01T00:00:00.000000Z", "9999-12-31T23:59:59.999999Z"})
  void s8_exportTimestampEndpointsRemainValid(String timestamp) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter().empty("owner-a", Instant.parse(timestamp)).writeTo(original);
    var header =
        new ImportJsonReader()
            .read(
                new ByteArrayInputStream(original.toByteArray()),
                (collection, row) -> fail("Empty file"));
    assertThat(header.exportedAt()).isEqualTo(Instant.parse(timestamp));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "2026-09-08T01:02:03.1234567Z",
        "2026-09-08T01:02:03.123456+00:00",
        "0000-01-01T00:00:00.000000Z",
        "+10000-01-01T00:00:00.000000Z",
        "2016-12-31T23:59:60.000000Z"
      })
  void s8_exportedAtHasTheExactExportTimestampRepresentation(String timestamp) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("2026-09-08T01:02:03.123456Z", timestamp)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(new ByteArrayInputStream(bytes), (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {100000, 100001})
  void s16_recordLimitIsInclusiveAndSharedAcrossCollections(int rows) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var projects = "[" + "{},".repeat(49999) + "{}]";
    var tasks = "[" + "{},".repeat(rows - 50001) + "{}]";
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":50000")
            .replace("\"tasks\":0", "\"tasks\":" + (rows - 50000))
            .replace("\"projects\":[]", "\"projects\":" + projects)
            .replace("\"tasks\":[]", "\"tasks\":" + tasks)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var prepared = new java.util.concurrent.atomic.AtomicInteger();
    if (rows == 100000) {
      var result =
          new ImportJsonReader()
              .read(
                  new ByteArrayInputStream(bytes), (collection, row) -> prepared.incrementAndGet());
      assertThat(result.counts().projects() + result.counts().tasks()).isEqualTo(rows);
    } else {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ImportJsonReader()
                      .read(
                          new ByteArrayInputStream(bytes),
                          (collection, row) -> prepared.incrementAndGet(),
                          "0".repeat(64)))
          .isInstanceOf(com.apptolast.organization.application.ImportTooLargeException.class);
    }
    assertThat(prepared.get()).isEqualTo(100000);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"number", "name", "string"})
  void s21_jacksonTokenDefaultsDoNotPreemptTheFileHash(String kind) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var json = original.toString(java.nio.charset.StandardCharsets.UTF_8);
    var changed =
        switch (kind) {
          case "number" -> "{\"unknown\":" + "1".repeat(1001) + "," + json.substring(1);
          case "name" -> "{\"" + "n".repeat(50001) + "\":0," + json.substring(1);
          default -> json.replace("owner-a", "a".repeat(20000001));
        };
    var bytes = changed.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    assertThat(bytes.length).isLessThan(33_554_432);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Empty collections"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {16, 17})
  void s8_depthLimitStillAppliesWhileRecoveringANumericRow(int depth) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var nested = "[".repeat(depth - 4) + "0" + "]".repeat(depth - 4);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace(
                "\"projects\":[]",
                "\"projects\":[{\"number\":1e2147483649,\"nested\":" + nested + "}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Unrepresentable row"),
                        "0".repeat(64)))
        .isInstanceOf(
            depth == 16
                ? com.apptolast.organization.application.ImportFileChangedException.class
                : com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"oldHash", "matchingHash", "malformed", "duplicate"})
  void s21_numericRecoveryPreservesSyntaxAndFollowingRecords(String mode) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var tail =
        switch (mode) {
          case "malformed" -> ",\"after\":]}";
          case "duplicate" -> ",\"number\":2}";
          default -> ",\"after\":[{},2]}";
        };
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":2")
            .replace(
                "\"projects\":[]",
                "\"projects\":[{\"number\":1e-2147483649" + tail + ",{\"id\":\"following\"}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var staged = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    var hash =
        mode.equals("matchingHash")
            ? HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
            : "0".repeat(64);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> staged.add(row),
                        hash))
        .isInstanceOf(
            mode.equals("oldHash")
                ? com.apptolast.organization.application.ImportFileChangedException.class
                : com.apptolast.organization.application.ImportInvalidFileException.class);
    if (mode.equals("oldHash") || mode.equals("matchingHash")) {
      assertThat(staged).hasSize(1);
      assertThat(staged.getFirst().get("id").textValue()).isEqualTo("following");
    } else assertThat(staged).isEmpty();
  }

  @Test
  void s21_wrongHashPrecedesUnrepresentableDecimalExponent() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace("\"projects\":[]", "\"projects\":[{\"number\":1e2147483649,\"after\":[{},2]}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Unrepresentable row must not stage"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @Test
  void s9_decimalIsExactWhenPreparedForLaterValidation() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("\"projects\":0", "\"projects\":1")
            .replace("\"projects\":[]", "\"projects\":[{\"number\":1.0000000000000000000001}]")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var staged = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
    new ImportJsonReader()
        .read(new ByteArrayInputStream(bytes), (collection, row) -> staged.add(row));
    assertThat(staged).hasSize(1);
    assertThat(staged.getFirst().get("number").decimalValue())
        .isEqualByComparingTo(new java.math.BigDecimal("1.0000000000000000000001"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s8_allFourteenCollectionsAreRequiredEvenWhenCountsAgree(boolean unknown) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var root =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original.toByteArray());
    var counts = (com.fasterxml.jackson.databind.node.ObjectNode) root.get("counts");
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) root.get("data");
    counts.remove("projects");
    data.remove("projects");
    if (unknown) {
      counts.put("privateUnknown", 1);
      data.putArray("privateUnknown").addObject().put("secret", "not staged");
    }
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(mapper.writeValueAsBytes(root)),
                        (collection, row) -> fail("Unknown collection must not stage")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "[]", "{\"projects\":{}}"})
  void s21_wrongHashPrecedesInvalidDataShape(String data) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var root =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original.toByteArray());
    root.set("data", mapper.readTree(data));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(mapper.writeValueAsBytes(root)),
                        (collection, row) -> fail("Invalid shape must not stage"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "owner,123",
    "owner,{}",
    "owner,null",
    "exportedAt,null"
  })
  void s8_metadataHasRequiredStringTypes(String field, String replacement) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var root =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original.toByteArray());
    root.set(field, mapper.readTree(replacement));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(mapper.writeValueAsBytes(root)),
                        (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s8_invalidTimestampIsAFileErrorAfterMatchingHash() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("2026-09-08T01:02:03.123456Z", "not-an-instant")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    var hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Empty file"),
                        hash))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"format", "schemaVersion"})
  void s8_formatAndVersionAreValidatedRatherThanIgnored(String field) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var root =
        (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(original.toByteArray());
    root.put(field, "invalid");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(mapper.writeValueAsBytes(root)),
                        (collection, row) -> fail("Empty file")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"[]", "{\"projects\":{\"invalid\":[]}}", "null"})
  void s21_wrongHashPrecedesSemanticallyInvalidCountsJson(String counts) throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replaceFirst("\"counts\":\\{[^}]*}", "\"counts\":" + counts)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Empty file"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @Test
  void s21_wrongHashPrecedesInvalidExportedAtText() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replace("2026-09-08T01:02:03.123456Z", "not-an-instant")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Empty file"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @Test
  void s21_wrongHashPrecedesUnknownEnvelopeFieldWithoutPreparingIt() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var json = original.toString(java.nio.charset.StandardCharsets.UTF_8);
    var bytes =
        (json.substring(0, json.length() - 1) + ",\"unknown\":{\"private\":[1,2,3]}}")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Unknown envelope must not stage rows"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

  @Test
  void s21_wrongHashPrecedesMismatchedDeclaredCountsAfterCompleteJson() throws Exception {
    var original = new ByteArrayOutputStream();
    new ExportJsonWriter()
        .empty("owner-a", Instant.parse("2026-09-08T01:02:03.123456Z"))
        .writeTo(original);
    var bytes =
        original
            .toString(java.nio.charset.StandardCharsets.UTF_8)
            .replaceFirst("\"projects\":0", "\"projects\":1")
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportJsonReader()
                    .read(
                        new ByteArrayInputStream(bytes),
                        (collection, row) -> fail("Empty file"),
                        "0".repeat(64)))
        .isInstanceOf(com.apptolast.organization.application.ImportFileChangedException.class);
  }

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
