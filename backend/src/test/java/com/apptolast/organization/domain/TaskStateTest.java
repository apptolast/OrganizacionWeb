package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskStateTest {
  @Test
  void s9_reconstructsCompletedWithoutChangingContent() {
    var original =
        Task.create(UUID.randomUUID(), UUID.randomUUID(), "T", "Criterion", 10, Instant.EPOCH);
    var completed =
        new Task(
            original.id(),
            original.projectId(),
            original.title(),
            original.completionCriterion(),
            original.estimatedMinutes(),
            "completed",
            original.createdAt(),
            Instant.EPOCH.plusSeconds(1));
    assertThat(completed.status()).isEqualTo("completed");
    assertThat(completed.title()).isEqualTo(original.title());
    assertThat(completed.completionCriterion()).isEqualTo(original.completionCriterion());
    assertThat(completed.estimatedMinutes()).isEqualTo(10);
    assertThat(original.status()).isEqualTo("pending");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "nullTask",
        "negativeVersion",
        "pendingWithDate",
        "completedWithoutDate",
        "completedWrongDate"
      })
  void s1_s2_rejectsContradictorySnapshot(String defect) {
    boolean complete = defect.startsWith("completed");
    var task =
        new Task(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "T",
            "",
            null,
            complete ? "completed" : "pending",
            Instant.EPOCH,
            Instant.EPOCH);
    var date =
        defect.equals("pendingWithDate")
            ? Instant.EPOCH
            : defect.equals("completedWrongDate") ? Instant.EPOCH.plusSeconds(1) : null;
    assertThatThrownBy(
            () ->
                new TaskSnapshot(
                    defect.equals("nullTask") ? null : task,
                    defect.equals("negativeVersion") ? -1 : 0,
                    date))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
