package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateTaskTest {
  @Test
  void s1_createsChildAndPrivateEventTogether() {
    var project = UUID.randomUUID();
    var now = Instant.parse("2026-09-06T00:00:00.123456789Z");
    TaskCommit commit =
        (owner, id, operation) -> {
          assertThat(owner).isEqualTo("persona-a");
          assertThat(id).isEqualTo(project);
          var creation = operation.apply("idea");
          assertThat(creation.event().aggregateId()).isEqualTo(project);
          assertThat(creation.event().taskId()).isEqualTo(creation.task().id());
          assertThat(creation.event().ownerId()).isEqualTo(owner);
          assertThat(creation.event().title()).isEqualTo("Tarea");
          assertThat(creation.event().type()).isEqualTo("TaskCreated.v1");
          assertThat(creation.event().schemaVersion()).isEqualTo(1);
          assertThat(creation.event().occurredAt())
              .isEqualTo(Instant.parse("2026-09-06T00:00:00.123456Z"));
          return creation.task();
        };
    var task =
        new CreateTask(commit, Clock.fixed(now, ZoneOffset.UTC))
            .execute("persona-a", project, " Tarea ", "privado", 10);
    assertThat(task.projectId()).isEqualTo(project);
    assertThat(task.completionCriterion()).isEqualTo("privado");
  }

  @Test
  void s13_rejectsCompletedInsideCommit() {
    TaskCommit commit = (owner, id, operation) -> operation.apply("completed").task();
    assertThatThrownBy(
            () ->
                new CreateTask(commit, Clock.systemUTC())
                    .execute("a", UUID.randomUUID(), "T", null, null))
        .isInstanceOf(ProjectCompletedException.class);
  }
}
