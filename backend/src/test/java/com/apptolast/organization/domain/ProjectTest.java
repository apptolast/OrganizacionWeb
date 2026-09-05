package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectTest {
  @Test
  void directConstructionRequiresServerMetadata() {
    UUID id = UUID.randomUUID();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(null, "persona-a", "Idea", "", "idea", Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, " ", "Idea", "", "idea", Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, null, "Idea", "", "idea", Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, "persona-a", "Idea", "", "active", Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, "persona-a", "Idea", "", null, Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, "persona-a", "Idea", "", "idea", null, Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new Project(id, "persona-a", "Idea", "", "idea", Instant.EPOCH, null))
        .isInstanceOf(IllegalArgumentException.class);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new Project(
                    id,
                    "persona-a",
                    "Idea",
                    "",
                    "idea",
                    Instant.EPOCH,
                    Instant.EPOCH.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void directConstructionCannotBypassNameInvariant() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new Project(
                    UUID.randomUUID(), "persona-a", "", "", "idea", Instant.EPOCH, Instant.EPOCH))
        .isInstanceOf(ValidationException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "🚀"})
  void s2_s8_preservesInclusiveCodepointLimits(String character) {
    for (int count : new int[] {1, 120}) {
      Project project =
          Project.create(
              UUID.randomUUID(),
              "persona-a",
              character.repeat(count),
              character.repeat(4000),
              Instant.EPOCH);
      assertThat(project.name()).isEqualTo(character.repeat(count));
      assertThat(project.description()).isEqualTo(character.repeat(4000));
    }
  }

  @Test
  void s4_s9_preservesUnicodeCaseAndDescriptionWhitespace() {
    Project project =
        Project.create(
            UUID.randomUUID(), "persona-a", "Ae\u0301", "  Primera\nSegunda  ", Instant.EPOCH);
    assertThat(project.name()).isEqualTo("Ae\u0301");
    assertThat(project.description()).isEqualTo("  Primera\nSegunda  ");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "🚀"})
  void s10_rejects4001DescriptionCodepoints(String character) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                Project.create(
                    UUID.randomUUID(), "persona-a", "Idea", character.repeat(4001), Instant.EPOCH))
        .isInstanceOf(ValidationException.class)
        .satisfies(
            error ->
                assertThat(((ValidationException) error).errors())
                    .containsExactly(
                        new FieldError(
                            "description",
                            "TOO_LONG",
                            "La descripción admite hasta 4000 caracteres.")));
  }

  @Test
  void s7_normalizesNullDescription() {
    assertThat(
            Project.create(UUID.randomUUID(), "persona-a", "Idea", null, Instant.EPOCH)
                .description())
        .isEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "🚀"})
  void s6_rejects121CodepointsAfterTrimming(String character) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                Project.create(
                    UUID.randomUUID(),
                    "persona-a",
                    " " + character.repeat(121) + " ",
                    "",
                    Instant.EPOCH))
        .isInstanceOf(ValidationException.class)
        .satisfies(
            error ->
                assertThat(((ValidationException) error).errors())
                    .containsExactly(
                        new FieldError(
                            "name", "TOO_LONG", "El nombre admite hasta 120 caracteres.")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.NullAndEmptySource
  @org.junit.jupiter.params.provider.ValueSource(strings = {" \u00a0\u2003"})
  void s5_rejectsMissingOrEmptyNames(String name) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> Project.create(UUID.randomUUID(), "persona-a", name, "", Instant.EPOCH))
        .isInstanceOf(ValidationException.class)
        .satisfies(
            error ->
                assertThat(((ValidationException) error).errors())
                    .singleElement()
                    .satisfies(
                        fieldError -> {
                          assertThat(fieldError.field()).isEqualTo("name");
                          assertThat(fieldError.code()).isEqualTo("REQUIRED");
                          assertThat(fieldError.message()).isEqualTo("Escribe un nombre.");
                        }));
  }

  @Test
  void s3_trimsOnlyUnicodeWhitespace() {
    assertThat(
            Project.create(
                    UUID.randomUUID(),
                    "persona-a",
                    " \u00a0\u2003Mi  Proyecto\u2003\u00a0 ",
                    "",
                    Instant.EPOCH)
                .name())
        .isEqualTo("Mi  Proyecto");
  }

  @Test
  void s1_createsIdeaWithServerIdentityAndDates() {
    UUID id = UUID.randomUUID();
    Instant now = Instant.parse("2026-09-05T12:00:00Z");
    Project project = Project.create(id, "persona-a", "Zenit", "Preparar la web", now);
    assertThat(project.id()).isEqualTo(id);
    assertThat(project.ownerId()).isEqualTo("persona-a");
    assertThat(project.name()).isEqualTo("Zenit");
    assertThat(project.description()).isEqualTo("Preparar la web");
    assertThat(project.status()).isEqualTo("idea");
    assertThat(project.createdAt()).isEqualTo(now);
    assertThat(project.updatedAt()).isEqualTo(now);
  }
}
