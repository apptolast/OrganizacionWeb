package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EditProjectTest {
  final Instant now = Instant.parse("2026-09-06T00:00:00.123456789Z");
  final Project original =
      Project.create(
          UUID.randomUUID(), "owner", "Original", "Old", Instant.parse("2026-09-05T00:00:00Z"));

  @Test
  void s1_changePreservesIdentityAndCreatesVersionedEvent() {
    var previous = new ProjectSnapshot(original, 4);
    var changes = new ArrayList<ProjectChange>();
    ProjectEditing store =
        (owner, id, operation) -> {
          assertThat(owner).isEqualTo(original.ownerId());
          assertThat(id).isEqualTo(original.id());
          var change = operation.apply(previous);
          changes.add(change);
          return change.snapshot();
        };
    var result =
        new EditProject(store, Clock.fixed(now, ZoneOffset.UTC))
            .execute(
                original.ownerId(),
                original.id(),
                new ProjectRevision(original.id(), 4),
                "  Nueva 😀  ",
                "<b>literal</b>");
    assertThat(result.project().name()).isEqualTo("Nueva 😀");
    assertThat(result.project().description()).isEqualTo("<b>literal</b>");
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.project().id()).isEqualTo(original.id());
    assertThat(result.project().ownerId()).isEqualTo(original.ownerId());
    assertThat(result.project().createdAt()).isEqualTo(original.createdAt());
    assertThat(result.project().status()).isEqualTo(original.status());
    assertThat(result.project().updatedAt())
        .isEqualTo(now.truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    var event = changes.getFirst().event();
    assertThat(event.eventId()).isNotNull();
    assertThat(event.aggregateId()).isEqualTo(original.id());
    assertThat(event.ownerId()).isEqualTo(original.ownerId());
    assertThat(event.occurredAt()).isEqualTo(result.project().updatedAt());
    assertThat(event.name()).isEqualTo("Nueva 😀");
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.type()).isEqualTo("ProjectUpdated.v1");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s2_conflictRejectsOldVersionOrDifferentProject(boolean wrongId) {
    ProjectEditing store =
        (owner, id, operation) -> operation.apply(new ProjectSnapshot(original, 4)).snapshot();
    var expected =
        new ProjectRevision(wrongId ? UUID.randomUUID() : original.id(), wrongId ? 4 : 3);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new EditProject(store, Clock.fixed(now, ZoneOffset.UTC))
                    .execute(original.ownerId(), original.id(), expected, "Original", "Old"))
        .isInstanceOf(ProjectConflictException.class);
  }

  @Test
  void s3_equivalentChangeKeepsOriginalSnapshotWithoutEvent() {
    var previous = new ProjectSnapshot(original, 4);
    var changes = new ArrayList<ProjectChange>();
    ProjectEditing store =
        (owner, id, operation) -> {
          var change = operation.apply(previous);
          changes.add(change);
          return change.snapshot();
        };
    var result =
        new EditProject(store, Clock.fixed(now, ZoneOffset.UTC))
            .execute(
                original.ownerId(),
                original.id(),
                new ProjectRevision(original.id(), 4),
                "  Original  ",
                "Old");
    assertThat(result).isSameAs(previous);
    assertThat(changes.getFirst().event()).isNull();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s1_eachEditableFieldAloneProducesChange(boolean nameOnly) {
    var changes = new ArrayList<ProjectChange>();
    ProjectEditing store =
        (owner, id, operation) -> {
          var change = operation.apply(new ProjectSnapshot(original, 4));
          changes.add(change);
          return change.snapshot();
        };
    var name = nameOnly ? "New" : "Original";
    var description = nameOnly ? "Old" : "New text";
    var result =
        new EditProject(store, Clock.fixed(now, ZoneOffset.UTC))
            .execute(
                original.ownerId(),
                original.id(),
                new ProjectRevision(original.id(), 4),
                name,
                description);
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.project().name()).isEqualTo(name);
    assertThat(result.project().description()).isEqualTo(description);
    assertThat(changes.getFirst().event()).isNotNull();
  }
}
