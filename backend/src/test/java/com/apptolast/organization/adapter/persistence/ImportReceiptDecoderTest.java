package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.BlockChangeReceipt;
import com.apptolast.organization.domain.BlockRequest;
import com.apptolast.organization.domain.PlannedBlock;
import com.apptolast.organization.domain.ResolvedBlockTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImportReceiptDecoderTest {
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s9_boundedNumbersAcceptExactIntegerDecimalSpellings(boolean session) throws Exception {
    if (session) {
      var receipt = sessionReceipt("EXTEND");
      var exported = exportSession(receipt);
      for (var side : java.util.List.of("before", "after"))
        ((ObjectNode) exported.at("/" + side + "/session"))
            .put("plannedMinutes", new java.math.BigDecimal("60.0"));
      ((ObjectNode) exported.path("extension"))
          .put("additionalMinutes", new java.math.BigDecimal("15.0"));
      var decoded =
          new ImportReceiptDecoder(json)
              .session(
                  exported,
                  receipt.id(),
                  receipt.before().session(),
                  receipt.action(),
                  receipt.before().revision(),
                  receipt.occurredAt());
      assertThat(decoded.at("/extension/additionalMinutes").isIntegralNumber()).isTrue();
      assertThat(decoded.at("/extension/additionalMinutes").longValue()).isEqualTo(15);
    } else {
      var id = UUID.randomUUID();
      var project = UUID.randomUUID();
      var task = UUID.randomUUID();
      var receipt =
          new BlockChangeReceipt(
              UUID.randomUUID(),
              id,
              "CANCELLED",
              1,
              Instant.parse("2026-09-08T01:02:03.123456Z"),
              block(id, project, task, 10),
              null);
      var exported = exportBlock(receipt);
      ((ObjectNode) exported.at("/before/time"))
          .put("durationMinutes", new java.math.BigDecimal("60.0"));
      var decoded =
          new ImportReceiptDecoder(json)
              .block(
                  exported,
                  receipt.id(),
                  id,
                  project,
                  task,
                  receipt.kind(),
                  receipt.version(),
                  receipt.occurredAt());
      assertThat(decoded.at("/before/time/durationMinutes").isIntegralNumber()).isTrue();
      assertThat(decoded.at("/before/time/durationMinutes").longValue()).isEqualTo(60);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"root", "before", "after"})
  void s8_sessionStateShapeFailureUsesTheFileErrorBoundary(String location) throws Exception {
    var receipt = sessionReceipt("PAUSE");
    var exported = exportSession(receipt);
    com.fasterxml.jackson.databind.JsonNode input = exported;
    if (location.equals("root")) input = json.nullNode();
    else exported.putNull(location);
    var selected = input;
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportReceiptDecoder(json)
                    .session(
                        selected,
                        receipt.id(),
                        receipt.before().session(),
                        receipt.action(),
                        receipt.before().revision(),
                        receipt.occurredAt()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"context", "transition", "extra"})
  void s11_sessionReceiptMustBeClosedAndMatchItsHistoricalTransition(String defect)
      throws Exception {
    var receipt = sessionReceipt("PAUSE");
    var exported = exportSession(receipt);
    switch (defect) {
      case "context" ->
          ((ObjectNode) exported.at("/before/session"))
              .put("projectId", UUID.randomUUID().toString());
      case "transition" -> ((ObjectNode) exported.path("after")).put("workedMicroseconds", "1");
      default -> ((ObjectNode) exported.at("/after/session")).put("unknown", "private");
    }
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportReceiptDecoder(json)
                    .session(
                        exported,
                        receipt.id(),
                        receipt.before().session(),
                        receipt.action(),
                        receipt.before().revision(),
                        receipt.occurredAt()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"PAUSE", "RESUME", "CLOSE", "EXTEND"})
  void s11_allSessionActionsKeepBothSnapshotsAndExactDurableLongs(String action) throws Exception {
    var receipt = sessionReceipt(action);
    var exported = exportSession(receipt);
    var expected = exported.deepCopy();
    for (var side : java.util.List.of("before", "after")) {
      var state = (ObjectNode) expected.path(side);
      state.put("revision", Long.parseLong(state.path("revision").textValue()));
      state.put("workedMicroseconds", Long.parseLong(state.path("workedMicroseconds").textValue()));
    }
    var decoded =
        new ImportReceiptDecoder(json)
            .session(
                exported,
                receipt.id(),
                receipt.before().session(),
                action,
                receipt.before().revision(),
                receipt.occurredAt());
    assertThat(decoded).isEqualTo(expected);
    assertThat(decoded.at("/before/revision").longValue()).isEqualTo(9007199254740993L);
    assertThat(decoded.at("/after/revision").longValue()).isEqualTo(9007199254740994L);
    assertThat(exported.at("/before/revision").isTextual()).isTrue();
  }

  private com.apptolast.organization.application.WorkSessionTransitionReceipt sessionReceipt(
      String action) {
    var started = Instant.parse("2026-09-08T00:00:00Z");
    var original =
        new com.apptolast.organization.domain.SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            started,
            60,
            started.plusSeconds(3600),
            "UTC");
    var now = started.plusSeconds(600);
    boolean running = !action.equals("RESUME");
    long revision = 9007199254740993L;
    var before =
        new com.apptolast.organization.domain.WorkSessionState(
            original,
            running ? "running" : "paused",
            revision,
            started,
            0,
            running ? started : null);
    var after =
        action.equals("EXTEND")
            ? new com.apptolast.organization.domain.WorkSessionState(
                original, before.status(), revision + 1, started, 0, before.runningSince())
            : new com.apptolast.organization.domain.WorkSessionState(
                original,
                action.equals("CLOSE") ? "closed" : action.equals("PAUSE") ? "paused" : "running",
                revision + 1,
                now,
                running ? 600000000L : 0,
                action.equals("RESUME") ? now : null);
    var closure =
        action.equals("CLOSE")
            ? new com.apptolast.organization.application.WorkSessionClosure(
                "  Nota histórica  ",
                "  Siguiente paso  ",
                java.time.LocalDate.of(2026, 9, 8),
                "Legacy/Zone")
            : null;
    var extension =
        action.equals("EXTEND")
            ? new com.apptolast.organization.application.WorkSessionExtension(
                15, original.plannedEndAt(), original.plannedEndAt().plusSeconds(900))
            : null;
    return new com.apptolast.organization.application.WorkSessionTransitionReceipt(
        UUID.randomUUID(), original.id(), action, now, before, after, closure, extension);
  }

  private ObjectNode exportSession(
      com.apptolast.organization.application.WorkSessionTransitionReceipt receipt)
      throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var out = json.getFactory().createGenerator(bytes)) {
      new ExportReceiptWriter(json)
          .session(
              out,
              json.writeValueAsString(receipt),
              receipt.id(),
              receipt.before().session(),
              receipt.action(),
              receipt.before().revision(),
              receipt.occurredAt());
    }
    return (ObjectNode) json.readTree(bytes.toByteArray());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "[]", "{}"})
  void s8_malformedBlockReceiptHasASafeFileError(String raw) throws Exception {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportReceiptDecoder(json)
                    .block(
                        json.readTree(raw),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "CANCELLED",
                        1,
                        Instant.parse("2026-09-08T01:02:03.123456Z")))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"", "/before/request", "/before/time"})
  void s8_blockReceiptIsClosedAtEveryHistoricalLevel(String path) throws Exception {
    var blockId = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var receipt =
        new BlockChangeReceipt(
            UUID.randomUUID(),
            blockId,
            "CANCELLED",
            1,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            block(blockId, project, task, 10),
            null);
    var exported = exportBlock(receipt);
    ((ObjectNode) exported.at(path)).put("unknown", "must not be silently removed");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportReceiptDecoder(json)
                    .block(
                        exported,
                        receipt.id(),
                        blockId,
                        project,
                        task,
                        receipt.kind(),
                        receipt.version(),
                        receipt.occurredAt()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s11_blockReceiptCannotReplaceTheOwnedHistoricalContext() throws Exception {
    var blockId = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var receipt =
        new BlockChangeReceipt(
            UUID.randomUUID(),
            blockId,
            "CANCELLED",
            1,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            block(blockId, project, task, 10),
            null);
    var exported = exportBlock(receipt);
    ((ObjectNode) exported.path("before")).put("projectId", UUID.randomUUID().toString());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ImportReceiptDecoder(json)
                    .block(
                        exported,
                        receipt.id(),
                        blockId,
                        project,
                        task,
                        receipt.kind(),
                        receipt.version(),
                        receipt.occurredAt()))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s11_rescheduledReceiptConvertsOnlyItsExactLongAndKeepsBothSnapshots() throws Exception {
    var blockId = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var receipt =
        new BlockChangeReceipt(
            UUID.randomUUID(),
            blockId,
            "RESCHEDULED",
            9007199254740993L,
            Instant.parse("2026-09-08T01:02:03.123456Z"),
            block(blockId, project, task, 10),
            block(blockId, project, task, 12));
    var exported = exportBlock(receipt);
    var expected = exported.deepCopy();
    expected.put("version", receipt.version());

    var decoded =
        new ImportReceiptDecoder(json)
            .block(
                exported,
                receipt.id(),
                blockId,
                project,
                task,
                receipt.kind(),
                receipt.version(),
                receipt.occurredAt());

    assertThat(decoded).isEqualTo(expected);
    assertThat(decoded.path("version").isIntegralNumber()).isTrue();
    assertThat(decoded.path("version").longValue()).isEqualTo(9007199254740993L);
    assertThat(exported.path("version").textValue()).isEqualTo("9007199254740993");
  }

  private ObjectNode exportBlock(BlockChangeReceipt receipt) throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var out = json.getFactory().createGenerator(bytes)) {
      new ExportReceiptWriter(json)
          .block(
              out,
              json.writeValueAsString(receipt),
              receipt.id(),
              receipt.blockId(),
              receipt.before().projectId(),
              receipt.before().taskId(),
              receipt.kind(),
              receipt.version(),
              receipt.occurredAt());
    }
    return (ObjectNode) json.readTree(bytes.toByteArray());
  }

  private static PlannedBlock block(UUID id, UUID project, UUID task, int hour) {
    var local = LocalDateTime.of(2026, 9, 8, hour, 0);
    var offset = ZoneOffset.ofHours(2);
    return new PlannedBlock(
        id,
        project,
        task,
        new BlockRequest(
            "Objetivo histórico", local, local.plusHours(1), "Europe/Madrid", offset, offset, true),
        new ResolvedBlockTime(
            local.toInstant(offset), local.plusHours(1).toInstant(offset), offset, offset, 60),
        Instant.parse("2026-09-07T00:00:00Z"));
  }
}
