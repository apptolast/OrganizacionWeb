package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;

final class HistoryCursorCodec {
  private record Envelope(
      int version,
      String owner,
      HistoryFilters filters,
      HistoryPosition upper,
      HistoryPosition after) {}

  static String encode(ObjectMapper json, HistoryCursor cursor) throws JsonProcessingException {
    return cursor == null
        ? null
        : Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                json.writeValueAsBytes(
                    new Envelope(
                        1, cursor.owner(), cursor.filters(), cursor.upper(), cursor.after())));
  }

  static HistoryCursor decode(ObjectMapper json, String value) {
    try {
      if (value == null || !value.matches("[A-Za-z0-9_-]+"))
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      var bytes = Base64.getUrlDecoder().decode(value);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      var tree =
          json.reader()
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature
                      .FAIL_ON_READING_DUP_TREE_KEY)
              .readTree(bytes);
      fields(tree, "version", "owner", "filters", "upper", "after");
      if (!tree.get("version").isIntegralNumber()
          || !tree.get("version").bigIntegerValue().equals(java.math.BigInteger.ONE))
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      fields(tree.get("filters"), "category", "projectId", "taskId", "from", "to");
      fields(tree.get("upper"), "occurredAt", "type", "id");
      fields(tree.get("after"), "occurredAt", "type", "id");
      text(tree.get("owner"), false);
      for (var name : java.util.List.of("upper", "after"))
        for (var field : java.util.List.of("occurredAt", "type", "id"))
          text(tree.get(name).get(field), false);
      for (var field : java.util.List.of("category", "projectId", "taskId", "from", "to"))
        text(tree.get("filters").get(field), true);
      for (var name : java.util.List.of("upper", "after"))
        time(tree.get(name).get("occurredAt").textValue());
      for (var name : java.util.List.of("upper", "after")) {
        if (!java.util.List.of(
                "BLOCK_PLANNED",
                "BLOCK_CHANGED",
                "TASK_STATUS_CHANGED",
                "SESSION_STARTED",
                "SESSION_CHANGED")
            .contains(tree.get(name).get("type").textValue()))
          throw BlockController.invalid("cursor", "INVALID_VALUE");
      }
      for (var name : java.util.List.of("upper", "after"))
        uuid(tree.get(name).get("id").textValue());
      var category = tree.get("filters").get("category");
      if (!category.isNull()
          && !java.util.List.of("sessions", "task-status", "planning")
              .contains(category.textValue()))
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      for (var field : java.util.List.of("projectId", "taskId")) {
        var node = tree.get("filters").get(field);
        if (!node.isNull()) uuid(node.textValue());
      }
      for (var field : java.util.List.of("from", "to")) {
        var node = tree.get("filters").get(field);
        if (!node.isNull()) HistoryController.date(node.textValue(), "cursor");
      }
      var envelope = json.treeToValue(tree, Envelope.class);
      return new HistoryCursor(
          envelope.owner(), envelope.filters(), envelope.upper(), envelope.after());
    } catch (java.io.IOException | IllegalArgumentException error) {
      throw BlockController.invalid("cursor", "INVALID_VALUE");
    }
  }

  private static void fields(com.fasterxml.jackson.databind.JsonNode node, String... names) {
    if (node == null || !node.isObject() || node.size() != names.length)
      throw BlockController.invalid("cursor", "INVALID_VALUE");
    for (var name : names)
      if (!node.has(name)) throw BlockController.invalid("cursor", "INVALID_VALUE");
  }

  private static void text(com.fasterxml.jackson.databind.JsonNode node, boolean nullable) {
    if (!(node.isTextual() || nullable && node.isNull()))
      throw BlockController.invalid("cursor", "INVALID_VALUE");
  }

  private static void time(String value) {
    try {
      if (!value.matches(
          "[0-9]{4}-[0-9]{2}-[0-9]{2}T(?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9](?:\\.[0-9]{1,6})?Z"))
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      var time = java.time.Instant.parse(value);
      int year = time.atOffset(java.time.ZoneOffset.UTC).getYear();
      if (!value.endsWith("Z") || time.getNano() % 1000 != 0 || year < 1 || year > 9999)
        throw BlockController.invalid("cursor", "INVALID_VALUE");
    } catch (java.time.DateTimeException error) {
      throw BlockController.invalid("cursor", "INVALID_VALUE");
    }
  }

  private static void uuid(String value) {
    if (!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw BlockController.invalid("cursor", "INVALID_VALUE");
  }
}
