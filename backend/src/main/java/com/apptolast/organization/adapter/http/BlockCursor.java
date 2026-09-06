package com.apptolast.organization.adapter.http;

import static com.apptolast.organization.adapter.http.BlockController.identifier;
import static com.apptolast.organization.adapter.http.BlockController.invalid;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

final class BlockCursor {
  record Position(Instant time, UUID id) {}

  static Position decode(
      ObjectMapper json,
      String value,
      UUID project,
      UUID task,
      String collection,
      String timestampField) {
    try {
      if (value == null || !value.matches("[A-Za-z0-9_-]+"))
        throw invalid("cursor", "INVALID_VALUE");
      var bytes = Base64.getUrlDecoder().decode(value);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
        throw invalid("cursor", "INVALID_VALUE");
      var body =
          json.reader()
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature
                      .FAIL_ON_READING_DUP_TREE_KEY)
              .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(bytes);
      if (body == null || !body.isObject() || body.size() != 5)
        throw invalid("cursor", "INVALID_VALUE");
      for (var field : List.of("collection", "projectId", "taskId", timestampField, "id"))
        if (!body.has(field) || !body.get(field).isTextual())
          throw invalid("cursor", "INVALID_VALUE");
      if (!body.get("collection").textValue().equals(collection)
          || !body.get("projectId").textValue().equals(project.toString())
          || !body.get("taskId").textValue().equals(task.toString()))
        throw invalid("cursor", "INVALID_VALUE");
      var timestamp = body.get(timestampField).textValue();
      var time = Instant.parse(timestamp);
      var year = time.atOffset(ZoneOffset.UTC).getYear();
      if (!timestamp.endsWith("Z") || time.getNano() % 1000 != 0 || year < 1 || year > 9999)
        throw invalid("cursor", "INVALID_VALUE");
      return new Position(time, identifier(body.get("id").textValue(), "cursor"));
    } catch (Exception error) {
      throw invalid("cursor", "INVALID_VALUE");
    }
  }
}
