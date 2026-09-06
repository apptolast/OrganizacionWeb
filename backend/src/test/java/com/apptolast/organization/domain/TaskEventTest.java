package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class TaskEventTest {
  @Test
  void s17_acceptsTaskEventWithItsOwnTitleLimit() {
    var id = UUID.randomUUID();
    var project = UUID.randomUUID();
    var now = Instant.EPOCH;
    var payload =
        Map.<String, Object>of(
            "eventId",
            id.toString(),
            "aggregateId",
            project.toString(),
            "ownerId",
            "a",
            "occurredAt",
            now.toString(),
            "schemaVersion",
            1,
            "type",
            "TaskCreated.v1",
            "taskId",
            UUID.randomUUID().toString(),
            "title",
            "🚀".repeat(160));
    assertThat(
            new OutboxMessage(id, project, "a", now, "TaskCreated.v1", 1, "unused", payload, 0)
                .validationCode())
        .isNull();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "taskId",
        "missing",
        "extra",
        "long",
        "blank",
        "space",
        "type",
        "version",
        "nullTitle"
      })
  void s18_blocksIncompatibleTaskEvents(String defect) {
    var id = UUID.randomUUID();
    var project = UUID.randomUUID();
    var now = Instant.EPOCH;
    var payload =
        new HashMap<String, Object>(
            Map.of(
                "eventId",
                id.toString(),
                "aggregateId",
                project.toString(),
                "ownerId",
                "a",
                "occurredAt",
                now.toString(),
                "schemaVersion",
                1,
                "type",
                "TaskCreated.v1",
                "taskId",
                UUID.randomUUID().toString(),
                "title",
                "T"));
    String type = "TaskCreated.v1";
    int version = 1;
    switch (defect) {
      case "taskId" -> payload.put("taskId", "1-1-1-1-1");
      case "missing" -> payload.remove("taskId");
      case "extra" -> payload.put("extra", true);
      case "long" -> payload.put("title", "a".repeat(161));
      case "blank" -> payload.put("title", "");
      case "space" -> payload.put("title", " T ");
      case "type" -> type = "TaskFuture.v1";
      case "version" -> version = 2;
      case "nullTitle" -> payload.put("title", null);
      default -> {}
    }
    assertThat(
            new OutboxMessage(id, project, "a", now, type, version, "unused", payload, 0)
                .validationCode())
        .isEqualTo(
            defect.equals("type") || defect.equals("version")
                ? "UNSUPPORTED_EVENT"
                : "INVALID_EVENT");
  }
}
