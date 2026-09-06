package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class SubtaskEventTest {
  @Test
  void s21_acceptsNineFieldsWithIndependentIdentities() {
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
            "SubtaskCreated.v1",
            "taskId",
            UUID.randomUUID().toString(),
            "parentTaskId",
            UUID.randomUUID().toString(),
            "title",
            "\uD83D\uDE80".repeat(160));
    assertThat(
            new OutboxMessage(id, project, "a", now, "SubtaskCreated.v1", 1, "unused", payload, 0)
                .validationCode())
        .isNull();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"partial", "null", "number", "equal", "equalUppercase"})
  void s22_rejectsInvalidParentIdentity(String defect) {
    var id = UUID.randomUUID();
    var project = UUID.randomUUID();
    var task = "abcdef01-2345-6789-abcd-0123456789ab";
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
                Instant.EPOCH.toString(),
                "schemaVersion",
                1,
                "type",
                "SubtaskCreated.v1",
                "taskId",
                task,
                "parentTaskId",
                UUID.randomUUID().toString(),
                "title",
                "T"));
    payload.put(
        "parentTaskId",
        switch (defect) {
          case "partial" -> "1-1-1-1-1";
          case "null" -> null;
          case "number" -> 1;
          case "equal" -> task;
          default -> task.toUpperCase(java.util.Locale.ROOT);
        });
    assertThat(
            new OutboxMessage(
                    id, project, "a", Instant.EPOCH, "SubtaskCreated.v1", 1, "unused", payload, 0)
                .validationCode())
        .isEqualTo("INVALID_EVENT");
  }
}
