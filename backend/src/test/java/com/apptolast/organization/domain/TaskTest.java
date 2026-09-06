package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskTest {
  @Test
  void s1_s3_normalizesOnlyOuterWhitespaceAndDefaults() {
    var now = Instant.parse("2026-09-06T00:00:00Z");
    var task =
        Task.create(
            UUID.randomUUID(), UUID.randomUUID(), " \u00a0Mi  Aé tarea\u2003", null, null, now);
    assertThat(task.title()).isEqualTo("Mi  Aé tarea");
    assertThat(task.completionCriterion()).isEmpty();
    assertThat(task.estimatedMinutes()).isNull();
    assertThat(task.status()).isEqualTo("pending");
    assertThat(task.createdAt()).isEqualTo(now);
    assertThat(task.updatedAt()).isEqualTo(now);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.NullAndEmptySource
  @org.junit.jupiter.params.provider.ValueSource(strings = {" \u00a0\u2003"})
  void s4_requiresTitleEvenInDirectConstruction(String title) {
    assertThatThrownBy(
            () ->
                new Task(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    title,
                    "",
                    null,
                    "pending",
                    Instant.EPOCH,
                    Instant.EPOCH))
        .isInstanceOfSatisfying(
            ValidationException.class,
            e ->
                assertThat(e.errors())
                    .containsExactly(
                        new FieldError("title", "REQUIRED", "Escribe un título para la tarea.")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"title,160", "completionCriterion,2000"})
  void s2_s5_s6_enforcesCodePointMaximum(String field, int maximum) {
    String text = "🚀".repeat(maximum);
    var accepted =
        Task.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            field.equals("title") ? text : "T",
            field.equals("title") ? null : text,
            null,
            Instant.EPOCH);
    assertThat(field.equals("title") ? accepted.title() : accepted.completionCriterion())
        .isEqualTo(text);
    assertThatThrownBy(
            () ->
                Task.create(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    field.equals("title") ? text + "a" : "T",
                    field.equals("title") ? null : text + "a",
                    null,
                    Instant.EPOCH))
        .isInstanceOfSatisfying(
            ValidationException.class,
            e -> {
              assertThat(e.errors().getFirst().field()).isEqualTo(field);
              assertThat(e.errors().getFirst().code()).isEqualTo("TOO_LONG");
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"0,false", "1,true", "1440,true", "1441,false"})
  void s6_s7_estimateBounds(int minutes, boolean accepted) {
    if (accepted)
      assertThat(
              Task.create(UUID.randomUUID(), UUID.randomUUID(), "T", null, minutes, Instant.EPOCH)
                  .estimatedMinutes())
          .isEqualTo(minutes);
    else
      assertThatThrownBy(
              () ->
                  Task.create(
                      UUID.randomUUID(), UUID.randomUUID(), "T", null, minutes, Instant.EPOCH))
          .isInstanceOfSatisfying(
              ValidationException.class,
              e ->
                  assertThat(e.errors())
                      .containsExactly(
                          new FieldError(
                              "estimatedMinutes",
                              "OUT_OF_RANGE",
                              "La estimación debe estar entre 1 y 1440 minutos.")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"id", "projectId", "createdAt", "updatedAt", "status", "nullStatus", "backward"})
  void s1_rejectsInvalidIdentityStateOrDates(String defect) {
    assertThatThrownBy(
            () ->
                new Task(
                    defect.equals("id") ? null : UUID.randomUUID(),
                    defect.equals("projectId") ? null : UUID.randomUUID(),
                    "T",
                    "",
                    null,
                    defect.equals("status")
                        ? "completed"
                        : defect.equals("nullStatus") ? null : "pending",
                    defect.equals("createdAt") ? null : Instant.EPOCH,
                    defect.equals("updatedAt")
                        ? null
                        : defect.equals("backward")
                            ? Instant.EPOCH.minusSeconds(1)
                            : Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
