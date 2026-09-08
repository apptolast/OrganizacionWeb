package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExportReceiptWriterTest {
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s5_invalidReceiptTimestampUsesTheStorageFailureBoundary(boolean session) throws Exception {
    var block = receipt();
    var change = pauseReceipt();
    var tree =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(session ? change : block);
    tree.put("occurredAt", "not-an-instant");
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> {
                var writer = new ExportReceiptWriter(json);
                if (session)
                  writer.session(
                      generator,
                      tree.toString(),
                      change.id(),
                      change.before().session(),
                      change.action(),
                      1,
                      change.occurredAt());
                else
                  writer.block(
                      generator,
                      tree.toString(),
                      block.id(),
                      block.blockId(),
                      block.before().projectId(),
                      block.before().taskId(),
                      block.kind(),
                      block.version(),
                      block.occurredAt());
              })
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  @Test
  void s5_originalPlannedMinutesCannotMatchByIntegerOverflow() throws Exception {
    var receipt = pauseReceipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/after/session"))
        .put("plannedMinutes", 4294967356L);
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .session(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          receipt.before().session(),
                          receipt.action(),
                          1,
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  @Test
  void s9_cancelledLegacyReceiptKeepsNullIntentOffsetsAndExactTextOnly() throws Exception {
    var seed = receipt();
    var receipt =
        new BlockChangeReceipt(
            seed.id(),
            seed.blockId(),
            "CANCELLED",
            seed.version(),
            seed.occurredAt(),
            seed.before(),
            null);
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    tree.put("unknown", "secret extra");
    var request = (com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/before/request");
    request.put("objective", "  Histórico 雪  ");
    request.putNull("startOffset");
    request.putNull("endOffset");
    request.putObject("unknown").put("private", "extra");
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      new ExportReceiptWriter(json)
          .block(
              generator,
              tree.toString(),
              receipt.id(),
              receipt.blockId(),
              receipt.before().projectId(),
              receipt.before().taskId(),
              receipt.kind(),
              receipt.version(),
              receipt.occurredAt());
    }
    var actual = json.readTree(output.toByteArray());
    assertThat(actual.path("after").isNull()).isTrue();
    assertThat(actual.at("/before/request/objective").textValue()).isEqualTo("  Histórico 雪  ");
    assertThat(actual.at("/before/request/startOffset").isNull()).isTrue();
    assertThat(actual.at("/before/request/endOffset").isNull()).isTrue();
    assertThat(actual.at("/before/time/startOffset").textValue()).isEqualTo("+02:00");
    assertThat(actual.at("/before/time/endOffset").textValue()).isEqualTo("+02:00");
    assertThat(actual.has("unknown")).isFalse();
    assertThat(actual.at("/before/request").has("unknown")).isFalse();
    assertThat(actual.at("/before/request").size()).isEqualTo(7);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"note", "date", "zone", "nul", "minutes", "end", "previous"})
  void s5_sessionDetailsMustBeValidForTheirDecision(String defect) throws Exception {
    var seed = pauseReceipt();
    boolean extend = java.util.Set.of("minutes", "end", "previous").contains(defect);
    var action = extend ? "EXTEND" : "CLOSE";
    var original = seed.before().session();
    var after =
        extend
            ? new WorkSessionState(
                original, "running", 2, seed.before().changedAt(), 0, seed.before().runningSince())
            : new WorkSessionState(original, "closed", 2, seed.occurredAt(), 600000000, null);
    var closure =
        extend
            ? null
            : new com.apptolast.organization.application.WorkSessionClosure(
                "", "", LocalDate.of(2026, 9, 8), "UTC");
    var extension =
        extend
            ? new com.apptolast.organization.application.WorkSessionExtension(
                15, original.plannedEndAt(), original.plannedEndAt().plusSeconds(900))
            : null;
    var receipt =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            seed.id(),
            original.id(),
            action,
            seed.occurredAt(),
            seed.before(),
            after,
            closure,
            extension);
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    var detail =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            tree.path(extend ? "extension" : "closure");
    switch (defect) {
      case "note" -> detail.putNull("progressNote");
      case "date" -> detail.put("workDate", "+10000-01-01");
      case "zone" -> detail.put("closeZoneId", " ");
      case "nul" -> detail.put("nextStep", "bad\u0000text");
      case "minutes" -> detail.put("additionalMinutes", 0);
      case "end" -> detail.put("effectiveEndAt", original.plannedEndAt().toString());
      case "previous" -> detail.put("previousEndAt", original.startedAt().toString());
    }
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .session(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          original,
                          action,
                          1,
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "revision",
        "worked",
        "status",
        "running",
        "closure",
        "negative",
        "submicro",
        "fractional"
      })
  void s5_sessionRejectsParseableButIncoherentTransition(String defect) throws Exception {
    var receipt = pauseReceipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    var before = (com.fasterxml.jackson.databind.node.ObjectNode) tree.path("before");
    var after = (com.fasterxml.jackson.databind.node.ObjectNode) tree.path("after");
    switch (defect) {
      case "revision" -> after.put("revision", 3);
      case "worked" -> after.put("workedMicroseconds", 599999999);
      case "status" -> after.put("status", "running");
      case "running" -> after.put("runningSince", receipt.occurredAt().toString());
      case "closure" -> tree.putObject("closure").put("progressNote", "unexpected");
      case "negative" -> before.put("workedMicroseconds", -1);
      case "submicro" -> before.put("changedAt", "2026-09-08T00:00:00.000000001Z");
      case "fractional" -> after.put("revision", 2.0);
    }
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .session(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          receipt.before().session(),
                          receipt.action(),
                          1,
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"kind", "after", "offset", "local", "duration", "submicro", "range"})
  void s5_blockRejectsIncoherentDiscriminatorsAndHistoricalTimes(String defect) throws Exception {
    var receipt = receipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    var before = (com.fasterxml.jackson.databind.node.ObjectNode) tree.path("before");
    var request = (com.fasterxml.jackson.databind.node.ObjectNode) before.path("request");
    var time = (com.fasterxml.jackson.databind.node.ObjectNode) before.path("time");
    String kind = defect.equals("kind") ? "MOVED" : receipt.kind();
    switch (defect) {
      case "kind" -> tree.put("kind", kind);
      case "after" -> tree.putNull("after");
      case "offset" -> request.put("startOffset", "+01:00");
      case "local" -> request.put("startLocal", "2026-09-08T10:01:00");
      case "duration" -> time.put("durationMinutes", 61);
      case "submicro" -> before.put("createdAt", "2026-09-07T00:00:00.123456789Z");
      case "range" -> before.put("createdAt", "+10000-01-01T00:00:00Z");
    }
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .block(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          receipt.blockId(),
                          receipt.before().projectId(),
                          receipt.before().taskId(),
                          kind,
                          receipt.version(),
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"null", "trailing", "duplicate", "boolean", "objective", "fractional"})
  void s5_blockRejectsMalformedStructureWithoutCoercingValues(String defect) throws Exception {
    var receipt = receipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    var request = (com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/before/request");
    if (defect.equals("boolean")) request.put("allowOverBudget", "true");
    if (defect.equals("objective")) request.putObject("objective").put("private", "unknown");
    if (defect.equals("fractional"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/before/time"))
          .put("durationMinutes", 60.5);
    var raw =
        switch (defect) {
          case "null" -> "null";
          case "trailing" -> tree + " {}";
          case "duplicate" -> tree.toString().replaceFirst("\\{", "{\"kind\":\"CANCELLED\",");
          default -> tree.toString();
        };
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .block(
                          generator,
                          raw,
                          receipt.id(),
                          receipt.blockId(),
                          receipt.before().projectId(),
                          receipt.before().taskId(),
                          receipt.kind(),
                          receipt.version(),
                          receipt.occurredAt()))
          .isInstanceOfAny(IllegalArgumentException.class, java.io.IOException.class);
    }
    assertThat(output.size()).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "id",
        "sessionId",
        "action",
        "occurredAt",
        "revision",
        "projectId",
        "taskId",
        "startedAt",
        "plannedMinutes",
        "plannedEndAt",
        "zoneId"
      })
  void s5_sessionReceiptMatchesRowAndEveryOriginalSessionField(String field) throws Exception {
    var receipt = pauseReceipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    var original = receipt.before().session();
    switch (field) {
      case "id", "sessionId" -> tree.put(field, UUID.randomUUID().toString());
      case "action" -> tree.put(field, "CLOSE");
      case "occurredAt" -> tree.put(field, receipt.occurredAt().plusSeconds(1).toString());
      case "revision" ->
          ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("before")).put(field, 9);
      default -> {
        var session = (com.fasterxml.jackson.databind.node.ObjectNode) tree.at("/after/session");
        switch (field) {
          case "plannedMinutes" -> session.put(field, 61);
          case "startedAt" -> session.put(field, original.startedAt().plusSeconds(1).toString());
          case "plannedEndAt" ->
              session.put(field, original.plannedEndAt().plusSeconds(1).toString());
          case "zoneId" -> session.put(field, "Europe/Madrid");
          default -> session.put(field, UUID.randomUUID().toString());
        }
      }
    }
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .session(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          original,
                          receipt.action(),
                          receipt.before().revision(),
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  private static com.apptolast.organization.application.WorkSessionTransitionReceipt
      pauseReceipt() {
    var start = Instant.parse("2026-09-08T00:00:00Z");
    var original =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            start,
            60,
            start.plusSeconds(3600),
            "UTC");
    var before = new WorkSessionState(original, "running", 1, start, 0, start);
    var now = start.plusSeconds(600);
    var after = new WorkSessionState(original, "paused", 2, now, 600000000, null);
    return new com.apptolast.organization.application.WorkSessionTransitionReceipt(
        UUID.randomUUID(), original.id(), "PAUSE", now, before, after);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"PAUSE", "RESUME", "EXTEND"})
  void s10_nonClosingReceiptsDoNotInventClosure(String action) throws Exception {
    var started = Instant.parse("2026-09-08T00:00:00Z");
    var original =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            started,
            60,
            started.plusSeconds(3600),
            "UTC");
    var now = started.plusSeconds(600);
    boolean running = !action.equals("RESUME");
    var before =
        new WorkSessionState(
            original, running ? "running" : "paused", 1, started, 0, running ? started : null);
    var after =
        action.equals("EXTEND")
            ? new WorkSessionState(original, before.status(), 2, started, 0, before.runningSince())
            : new WorkSessionState(
                original,
                running ? "paused" : "running",
                2,
                now,
                running ? 600000000 : 0,
                running ? null : now);
    var extension =
        action.equals("EXTEND")
            ? new com.apptolast.organization.application.WorkSessionExtension(
                15, original.plannedEndAt(), original.plannedEndAt().plusSeconds(900))
            : null;
    var receipt =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            UUID.randomUUID(), original.id(), action, now, before, after, null, extension);
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    tree.remove("closure");
    if (extension == null) tree.remove("extension");
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      new ExportReceiptWriter(json)
          .session(generator, tree.toString(), receipt.id(), original, action, 1, now);
    }
    var actual = json.readTree(output.toByteArray());
    assertThat(actual.has("closure")).isFalse();
    assertThat(actual.size()).isEqualTo(extension == null ? 6 : 7);
    assertThat(actual.at("/after/status").textValue()).isEqualTo(after.status());
    assertThat(actual.at("/after/workedMicroseconds").textValue())
        .isEqualTo(Long.toString(after.workedMicroseconds()));
    if (extension != null) {
      assertThat(actual.at("/extension/additionalMinutes").intValue()).isEqualTo(15);
      assertThat(actual.at("/extension/previousEndAt").textValue())
          .isEqualTo("2026-09-08T01:00:00.000000Z");
      assertThat(actual.at("/extension/effectiveEndAt").textValue())
          .isEqualTo("2026-09-08T01:15:00.000000Z");
      assertThat(actual.path("extension").size()).isEqualTo(3);
    }
  }

  @Test
  void s10_closedSessionKeepsExactNotesAndHistoricalCivilDate() throws Exception {
    var started = Instant.parse("2026-09-07T22:00:00Z");
    var original =
        new SessionStart(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            started,
            60,
            started.plusSeconds(3600),
            "America/Los_Angeles");
    var occurred = Instant.parse("2026-09-08T01:02:03.123456Z");
    var before =
        new WorkSessionState(
            original, "paused", 9007199254740993L, started.plusSeconds(3600), 1800000000L, null);
    var after =
        new WorkSessionState(
            original, "closed", before.revision() + 1, occurred, before.workedMicroseconds(), null);
    var closure =
        new com.apptolast.organization.application.WorkSessionClosure(
            "  Avance 雪\n  ", "  Próximo paso  ", LocalDate.of(2026, 9, 7), "America/Los_Angeles");
    var receipt =
        new com.apptolast.organization.application.WorkSessionTransitionReceipt(
            UUID.randomUUID(), original.id(), "CLOSE", occurred, before, after, closure);
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    tree.remove("extension");
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      new ExportReceiptWriter(json)
          .session(
              generator,
              tree.toString(),
              receipt.id(),
              original,
              "CLOSE",
              before.revision(),
              occurred);
    }
    var actual = json.readTree(output.toByteArray());
    assertThat(actual.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "sessionId", "action", "occurredAt", "before", "after", "closure");
    assertThat(actual.path("closure")).isEqualTo(json.valueToTree(closure));
    assertThat(actual.at("/before/revision").textValue()).isEqualTo("9007199254740993");
    assertThat(actual.at("/after/revision").textValue()).isEqualTo("9007199254740994");
    assertThat(actual.at("/after/workedMicroseconds").textValue()).isEqualTo("1800000000");
    assertThat(actual.at("/after/changedAt").textValue()).isEqualTo("2026-09-08T01:02:03.123456Z");
    assertThat(actual.at("/before/session/startedAt").textValue())
        .isEqualTo("2026-09-07T22:00:00.000000Z");
    assertThat(actual.at("/after/runningSince").isNull()).isTrue();
    assertThat(actual.path("after").size()).isEqualTo(6);
    assertThat(actual.at("/after/session").size()).isEqualTo(7);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"id", "blockId", "projectId", "taskId", "kind", "version", "occurredAt"})
  void s5_blockReceiptMustAgreeWithItsOwnedDurableRow(String field) throws Exception {
    var receipt = receipt();
    var tree = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(receipt);
    switch (field) {
      case "projectId", "taskId" ->
          ((com.fasterxml.jackson.databind.node.ObjectNode) tree.path("after"))
              .put(field, UUID.randomUUID().toString());
      case "kind" -> tree.put(field, "CANCELLED");
      case "version" -> tree.put(field, receipt.version() + 1);
      case "occurredAt" -> tree.put(field, receipt.occurredAt().plusSeconds(1).toString());
      default -> tree.put(field, UUID.randomUUID().toString());
    }
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  new ExportReceiptWriter(json)
                      .block(
                          generator,
                          tree.toString(),
                          receipt.id(),
                          receipt.blockId(),
                          receipt.before().projectId(),
                          receipt.before().taskId(),
                          receipt.kind(),
                          receipt.version(),
                          receipt.occurredAt()))
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThat(output.size()).isZero();
  }

  private static BlockChangeReceipt receipt() {
    var block = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    return new BlockChangeReceipt(
        UUID.randomUUID(),
        block,
        "RESCHEDULED",
        1,
        Instant.parse("2026-09-08T01:02:03.123456Z"),
        block(block, project, task, 10),
        block(block, project, task, 12));
  }

  @Test
  void s9_rescheduledReceiptKeepsBothCompleteHistoricalSnapshots() throws Exception {
    var blockId = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var id = UUID.randomUUID();
    var occurred = Instant.parse("2026-09-08T01:02:03.123456Z");
    var before = block(blockId, project, task, 10);
    var after = block(blockId, project, task, 12);
    var receipt =
        new BlockChangeReceipt(
            id, blockId, "RESCHEDULED", 9007199254740993L, occurred, before, after);
    var output = new ByteArrayOutputStream();
    try (var generator = json.getFactory().createGenerator(output)) {
      new ExportReceiptWriter(json)
          .block(
              generator,
              json.writeValueAsString(receipt),
              id,
              blockId,
              project,
              task,
              "RESCHEDULED",
              receipt.version(),
              occurred);
    }
    var actual = json.readTree(output.toByteArray());
    assertThat(actual.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("id", "blockId", "kind", "version", "occurredAt", "before", "after");
    assertThat(actual.path("version").isTextual()).isTrue();
    assertThat(actual.path("version").textValue()).isEqualTo("9007199254740993");
    assertThat(actual.path("occurredAt").textValue()).isEqualTo("2026-09-08T01:02:03.123456Z");
    for (var name : java.util.List.of("before", "after")) {
      var expected = json.valueToTree(name.equals("before") ? before : after);
      ((com.fasterxml.jackson.databind.node.ObjectNode) expected)
          .put("createdAt", "2026-09-07T00:00:00.000000Z");
      var time = (com.fasterxml.jackson.databind.node.ObjectNode) expected.path("time");
      var snapshot = name.equals("before") ? before : after;
      time.put("startAt", snapshot.time().startAt().toString().replace("Z", ".000000Z"));
      time.put("endAt", snapshot.time().endAt().toString().replace("Z", ".000000Z"));
      var request = (com.fasterxml.jackson.databind.node.ObjectNode) expected.path("request");
      request.put("startLocal", snapshot.request().startLocal().toString() + ":00");
      request.put("endLocal", snapshot.request().endLocal().toString() + ":00");
      assertThat(actual.path(name)).isEqualTo(expected);
    }
  }

  private static PlannedBlock block(UUID id, UUID project, UUID task, int hour) {
    var start = LocalDateTime.of(2026, 9, 8, hour, 0);
    var offset = ZoneOffset.ofHours(2);
    return new PlannedBlock(
        id,
        project,
        task,
        new BlockRequest(
            "Objetivo histórico", start, start.plusHours(1), "Europe/Madrid", offset, offset, true),
        new ResolvedBlockTime(
            start.toInstant(offset), start.plusHours(1).toInstant(offset), offset, offset, 60),
        Instant.parse("2026-09-07T00:00:00Z"));
  }
}
