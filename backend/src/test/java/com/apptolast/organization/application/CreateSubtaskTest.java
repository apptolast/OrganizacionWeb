package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateSubtaskTest {
  @Test
  void s1_createsDirectChildAndOnlySubtaskEvent() {
    var project = UUID.randomUUID();
    var parent = UUID.randomUUID();
    var now = Instant.parse("2026-09-06T00:00:00.123456789Z");
    SubtaskCommit commit =
        (owner, id, parentId, operation) -> {
          assertThat(owner).isEqualTo("persona-a");
          assertThat(id).isEqualTo(project);
          assertThat(parentId).isEqualTo(parent);
          var creation = operation.apply("idea");
          assertThat(creation.event()).isInstanceOf(SubtaskCreated.class);
          var event = (SubtaskCreated) creation.event();
          assertThat(event.eventId()).isNotNull();
          assertThat(event.aggregateId()).isEqualTo(project);
          assertThat(event.ownerId()).isEqualTo(owner);
          assertThat(event.taskId()).isEqualTo(creation.task().id()).isNotEqualTo(parent);
          assertThat(event.parentTaskId()).isEqualTo(parent);
          assertThat(event.title()).isEqualTo("Paso");
          assertThat(event.type()).isEqualTo("SubtaskCreated.v1");
          assertThat(event.schemaVersion()).isEqualTo(1);
          assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-06T00:00:00.123456Z"));
          return creation.task();
        };
    var task =
        new CreateSubtask(commit, Clock.fixed(now, ZoneOffset.UTC))
            .execute("persona-a", project, parent, " Paso ", "privado", 10);
    assertThat(task.projectId()).isEqualTo(project);
    assertThat(task.completionCriterion()).isEqualTo("privado");
  }
}
