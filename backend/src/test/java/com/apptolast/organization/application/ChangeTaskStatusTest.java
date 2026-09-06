package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeTaskStatusTest {
  @Test
  void s2_completesWithOneVersionAndPrivateEvent() {
    var task =
        Task.create(
            UUID.randomUUID(), UUID.randomUUID(), "T", "Private criterion", 10, Instant.EPOCH);
    var now = Instant.parse("2026-09-06T00:00:00.123456789Z");
    TaskStatusEditing store =
        (owner, project, id, operation) -> {
          assertThat(owner).isEqualTo("a");
          assertThat(project).isEqualTo(task.projectId());
          assertThat(id).isEqualTo(task.id());
          var change = operation.apply(new TaskSnapshot(task, 0, null));
          var next = change.snapshot();
          assertThat(next.version()).isEqualTo(1);
          assertThat(next.task().status()).isEqualTo("completed");
          assertThat(next.completedAt())
              .isEqualTo(Instant.parse("2026-09-06T00:00:00.123456Z"))
              .isEqualTo(next.task().updatedAt());
          assertThat(next.task().createdAt()).isEqualTo(task.createdAt());
          assertThat(next.task().completionCriterion()).isEqualTo(task.completionCriterion());
          var event = change.event();
          assertThat(event.eventId()).isNotNull();
          assertThat(event.aggregateId()).isEqualTo(task.projectId());
          assertThat(event.taskId()).isEqualTo(task.id());
          assertThat(event.ownerId()).isEqualTo(owner);
          assertThat(event.type()).isEqualTo("TaskStatusChanged.v1");
          assertThat(event.schemaVersion()).isEqualTo(1);
          assertThat(event.fromStatus()).isEqualTo("pending");
          assertThat(event.toStatus()).isEqualTo("completed");
          assertThat(event.occurredAt()).isEqualTo(next.completedAt());
          return next;
        };
    var result =
        new ChangeTaskStatus(store, Clock.fixed(now, ZoneOffset.UTC))
            .execute("a", task.projectId(), task.id(), new TaskRevision(task.id(), 0), "completed");
    assertThat(result.task().id()).isEqualTo(task.id());
  }

  @Test
  void s3_reopensWithoutCurrentCompletionDate() {
    var task =
        new Task(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T",
            "Private",
            10,
            "completed",
            Instant.EPOCH,
            Instant.EPOCH.plusSeconds(1));
    TaskStatusEditing store =
        (owner, project, id, operation) -> {
          var change = operation.apply(new TaskSnapshot(task, 1, task.updatedAt()));
          assertThat(change.snapshot().completedAt()).isNull();
          assertThat(change.snapshot().version()).isEqualTo(2);
          assertThat(change.snapshot().task().status()).isEqualTo("pending");
          assertThat(change.event().fromStatus()).isEqualTo("completed");
          assertThat(change.event().toStatus()).isEqualTo("pending");
          return change.snapshot();
        };
    new ChangeTaskStatus(store, Clock.fixed(Instant.EPOCH.plusSeconds(2), ZoneOffset.UTC))
        .execute("a", task.projectId(), task.id(), new TaskRevision(task.id(), 1), "pending");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"pending", "completed"})
  void s4_keepsSameSnapshotWithoutEventForCurrentNoOp(String status) {
    var task =
        new Task(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T",
            "",
            null,
            status,
            Instant.EPOCH,
            Instant.EPOCH);
    var previous = new TaskSnapshot(task, 2, status.equals("completed") ? Instant.EPOCH : null);
    TaskStatusEditing store =
        (owner, project, id, operation) -> {
          var change = operation.apply(previous);
          assertThat(change.snapshot()).isSameAs(previous);
          assertThat(change.event()).isNull();
          return change.snapshot();
        };
    assertThat(
            new ChangeTaskStatus(store, Clock.systemUTC())
                .execute("a", task.projectId(), task.id(), new TaskRevision(task.id(), 2), status))
        .isSameAs(previous);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"version", "identity"})
  void s5_checksRevisionBeforeSatisfiedIntent(String defect) {
    var task = Task.create(UUID.randomUUID(), UUID.randomUUID(), "T", "", null, Instant.EPOCH);
    TaskStatusEditing store =
        (owner, project, id, operation) ->
            operation.apply(new TaskSnapshot(task, 2, null)).snapshot();
    var expected =
        new TaskRevision(
            defect.equals("identity") ? UUID.randomUUID() : task.id(),
            defect.equals("version") ? 0 : 2);
    assertThatThrownBy(
            () ->
                new ChangeTaskStatus(store, Clock.systemUTC())
                    .execute("a", task.projectId(), task.id(), expected, "pending"))
        .isInstanceOf(TaskConflictException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "completed,false",
    "pending,false",
    "completed,true",
    "pending,true"
  })
  void s33_usesNondecreasingMicrosecondInstant(String target, boolean backwards) {
    var previousTime = Instant.parse("2026-09-06T00:00:00.123456Z");
    var clockTime = backwards ? previousTime.minusSeconds(30) : previousTime.plusNanos(789);
    String old = target.equals("completed") ? "pending" : "completed";
    var task =
        new Task(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T",
            "",
            null,
            old,
            previousTime.minusSeconds(60),
            previousTime);
    TaskStatusEditing store =
        (owner, project, id, operation) -> {
          var change =
              operation.apply(
                  new TaskSnapshot(task, 5, old.equals("completed") ? previousTime : null));
          assertThat(change.snapshot().version()).isEqualTo(6);
          assertThat(change.snapshot().task().updatedAt()).isEqualTo(previousTime);
          assertThat(change.event().occurredAt()).isEqualTo(previousTime);
          assertThat(change.snapshot().completedAt())
              .isEqualTo(target.equals("completed") ? previousTime : null);
          return change.snapshot();
        };
    new ChangeTaskStatus(store, Clock.fixed(clockTime, ZoneOffset.UTC))
        .execute("a", task.projectId(), task.id(), new TaskRevision(task.id(), 5), target);
  }
}
