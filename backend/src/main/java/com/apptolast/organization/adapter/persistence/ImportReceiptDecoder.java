package com.apptolast.organization.adapter.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.UUID;

public final class ImportReceiptDecoder {
  private final ObjectMapper json;

  public ImportReceiptDecoder(ObjectMapper json) {
    this.json = json;
  }

  public JsonNode session(
      JsonNode receipt,
      UUID changeId,
      com.apptolast.organization.domain.SessionStart original,
      String action,
      long expectedRevision,
      Instant occurredAt) {
    try (var out = new com.fasterxml.jackson.databind.util.TokenBuffer(json, false)) {
      ObjectNode durable = object(integers(object(receipt).deepCopy()));
      for (var side : java.util.List.of("before", "after")) {
        var state = object(durable.path(side));
        for (var field : java.util.List.of("revision", "workedMicroseconds")) {
          state.set(
              field,
              json.getNodeFactory().numberNode(Long.parseLong(state.path(field).textValue())));
        }
      }
      new ExportReceiptWriter(json)
          .session(
              out, durable.toString(), changeId, original, action, expectedRevision, occurredAt);
      if (!integers(json.readTree(out.asParser())).equals(integers(receipt.deepCopy())))
        throw new com.apptolast.organization.application.ImportInvalidFileException();
      return durable;
    } catch (java.io.IOException | IllegalArgumentException | ArithmeticException invalid) {
      throw new com.apptolast.organization.application.ImportInvalidFileException();
    }
  }

  public JsonNode block(
      JsonNode receipt,
      UUID changeId,
      UUID blockId,
      UUID projectId,
      UUID taskId,
      String kind,
      long version,
      Instant occurredAt) {
    try (var out = new com.fasterxml.jackson.databind.util.TokenBuffer(json, false)) {
      if (!receipt.isObject())
        throw new com.apptolast.organization.application.ImportInvalidFileException();
      ObjectNode durable = object(integers(object(receipt).deepCopy()));
      durable.set(
          "version",
          json.getNodeFactory().numberNode(Long.parseLong(receipt.path("version").textValue())));
      new ExportReceiptWriter(json)
          .block(
              out,
              durable.toString(),
              changeId,
              blockId,
              projectId,
              taskId,
              kind,
              version,
              occurredAt);
      if (!integers(json.readTree(out.asParser())).equals(integers(receipt.deepCopy())))
        throw new com.apptolast.organization.application.ImportInvalidFileException();
      return durable;
    } catch (java.io.IOException | IllegalArgumentException | ArithmeticException invalid) {
      throw new com.apptolast.organization.application.ImportInvalidFileException();
    }
  }

  private static ObjectNode object(JsonNode value) {
    if (!value.isObject())
      throw new com.apptolast.organization.application.ImportInvalidFileException();
    return (ObjectNode) value;
  }

  private JsonNode integers(JsonNode value) {
    if (value.isNumber()) {
      long exact = value.decimalValue().longValueExact();
      return exact >= Integer.MIN_VALUE && exact <= Integer.MAX_VALUE
          ? json.getNodeFactory().numberNode((int) exact)
          : json.getNodeFactory().numberNode(exact);
    }
    if (value.isObject()) {
      var object = (ObjectNode) value;
      for (var field : object.properties()) object.set(field.getKey(), integers(field.getValue()));
    } else if (value.isArray()) {
      var array = (com.fasterxml.jackson.databind.node.ArrayNode) value;
      for (int i = 0; i < array.size(); i++) array.set(i, integers(array.get(i)));
    }
    return value;
  }
}
