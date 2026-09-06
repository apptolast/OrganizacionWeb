package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.*;

class TaskStatusEventTest {
  static Map<String, Object> payload() {
    return new HashMap<>(
        Map.of(
            "eventId",
            UUID.randomUUID().toString(),
            "aggregateId",
            UUID.randomUUID().toString(),
            "ownerId",
            "a",
            "occurredAt",
            Instant.EPOCH.toString(),
            "schemaVersion",
            1,
            "type",
            "TaskStatusChanged.v1",
            "taskId",
            UUID.randomUUID().toString(),
            "fromStatus",
            "pending",
            "toStatus",
            "completed"));
  }

  static OutboxMessage message(Map<String, Object> payload) {
    return new OutboxMessage(
        UUID.fromString((String) payload.get("eventId")),
        UUID.fromString((String) payload.get("aggregateId")),
        "a",
        Instant.EPOCH,
        "TaskStatusChanged.v1",
        1,
        "unused",
        payload,
        0);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s20_acceptsBothRealTransitions(boolean complete) {
    var body = payload();
    if (!complete) {
      body.put("fromStatus", "completed");
      body.put("toStatus", "pending");
    }
    assertThat(message(body).validationCode()).isNull();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "active,completed",
    "pending,active",
    "pending,pending",
    "completed,completed",
    "NULL,completed",
    "pending,NULL",
    "1,completed",
    "pending,1"
  })
  void s32_rejectsUnknownOrUnchangedTransition(String from, String to) {
    var body = payload();
    body.put("fromStatus", from.equals("NULL") ? null : from.equals("1") ? 1 : from);
    body.put("toStatus", to.equals("NULL") ? null : to.equals("1") ? 1 : to);
    assertThat(message(body).validationCode()).isEqualTo("INVALID_EVENT");
  }
}
