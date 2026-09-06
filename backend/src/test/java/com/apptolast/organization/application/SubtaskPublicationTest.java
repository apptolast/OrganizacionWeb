package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;

class SubtaskPublicationTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "type",
        "version",
        "extra",
        "missingTask",
        "missingParent",
        "taskId",
        "parentId",
        "nullTask",
        "nullParent",
        "numericTask",
        "numericParent",
        "equal",
        "equalCase",
        "empty",
        "long",
        "space",
        "nullTitle"
      })
  void s22_blocksEveryIncompatibleDivisionWithoutSending(String defect) {
    UUID id = UUID.randomUUID(), project = UUID.randomUUID();
    String task = "abcdef01-2345-6789-abcd-0123456789ab";
    String type = "SubtaskCreated.v1";
    int version = 1;
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
                type,
                "taskId",
                task,
                "parentTaskId",
                UUID.randomUUID().toString(),
                "title",
                "T"));
    switch (defect) {
      case "type" -> type = "Future.v1";
      case "version" -> version = 2;
      case "extra" -> payload.put("extra", 1);
      case "missingTask" -> payload.remove("taskId");
      case "missingParent" -> payload.remove("parentTaskId");
      case "taskId" -> payload.put("taskId", "1-1-1-1-1");
      case "parentId" -> payload.put("parentTaskId", "1-1-1-1-1");
      case "nullTask" -> payload.put("taskId", null);
      case "nullParent" -> payload.put("parentTaskId", null);
      case "numericTask" -> payload.put("taskId", 1);
      case "numericParent" -> payload.put("parentTaskId", 1);
      case "equal" -> payload.put("parentTaskId", task);
      case "equalCase" -> payload.put("parentTaskId", task.toUpperCase(Locale.ROOT));
      case "empty" -> payload.put("title", "");
      case "long" -> payload.put("title", "a".repeat(161));
      case "space" -> payload.put("title", " T ");
      case "nullTitle" -> payload.put("title", null);
    }
    var event =
        new OutboxMessage(id, project, "a", Instant.EPOCH, type, version, "unchanged", payload, 0);
    var work = new PublishOutboxTest.Work(event);
    var logs = new ArrayList<PublicationAttempt>();
    PublicationAudit audit =
        new PublicationAudit() {
          public void event(PublicationAttempt attempt) {
            logs.add(attempt);
          }

          public void workerError(String code) {
            throw new AssertionError(code);
          }
        };
    new PublishOutbox(
            work,
            message -> {
              throw new AssertionError("Invalid event must not reach broker");
            },
            audit,
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        .runCycle();
    assertThat(work.persisted)
        .containsExactly(
            new PublicationAttempt(
                id,
                "blocked",
                0,
                Instant.EPOCH,
                null,
                defect.equals("type") || defect.equals("version")
                    ? "UNSUPPORTED_EVENT"
                    : "INVALID_EVENT"));
    assertThat(logs).isEqualTo(work.persisted);
  }
}
