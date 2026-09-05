package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ChangeProjectStatusTest {
  final Instant now = Instant.parse("2026-09-06T01:00:00.123456789Z");

  @ParameterizedTest
  @CsvSource({
    "idea,active",
    "idea,completed",
    "active,paused",
    "active,completed",
    "paused,active",
    "paused,completed",
    "completed,paused"
  })
  void s1_allowedTransitionChangesOnlyStatusAndEmitsExactEvent(String from, String to) {
    var original =
        new Project(UUID.randomUUID(), "owner", "Name", "Text", from, Instant.EPOCH, Instant.EPOCH);
    var previous = new ProjectSnapshot(original, 4);
    var changes = new ArrayList<ProjectStatusChange>();
    ProjectStatusEditing store =
        (owner, id, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(id).isEqualTo(original.id());
          var change = operation.apply(previous, 0L);
          changes.add(change);
          return change.snapshot();
        };
    var result =
        new ChangeProjectStatus(store, Clock.fixed(now, ZoneOffset.UTC), 3)
            .execute("owner", original.id(), new ProjectRevision(original.id(), 4), to);
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.project())
        .isEqualTo(
            new Project(
                original.id(),
                "owner",
                "Name",
                "Text",
                to,
                Instant.EPOCH,
                now.truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
    var event = changes.getFirst().event();
    assertThat(event.eventId()).isNotNull();
    assertThat(event.aggregateId()).isEqualTo(original.id());
    assertThat(event.ownerId()).isEqualTo("owner");
    assertThat(event.occurredAt()).isEqualTo(result.project().updatedAt());
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.type()).isEqualTo("ProjectStatusChanged.v1");
    assertThat(event.fromStatus()).isEqualTo(from);
    assertThat(event.toStatus()).isEqualTo(to);
  }

  @ParameterizedTest
  @CsvSource({"idea,paused", "active,idea", "paused,idea", "completed,idea", "completed,active"})
  void s2_invalidTransitionNeverProducesChange(String from, String to) {
    var original =
        new Project(UUID.randomUUID(), "owner", "Name", "Text", from, Instant.EPOCH, Instant.EPOCH);
    ProjectStatusEditing store =
        (owner, id, operation) -> operation.apply(new ProjectSnapshot(original, 0), 0L).snapshot();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ChangeProjectStatus(store, Clock.fixed(now, ZoneOffset.UTC), 3)
                    .execute("owner", original.id(), new ProjectRevision(original.id(), 0), to))
        .isInstanceOf(InvalidProjectTransitionException.class);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"idea", "active", "paused", "completed"})
  void s3_currentStateIsNoopEvenAboveCapacity(String state) {
    var original =
        new Project(
            UUID.randomUUID(), "owner", "Name", "Text", state, Instant.EPOCH, Instant.EPOCH);
    var previous = new ProjectSnapshot(original, 4);
    ProjectStatusEditing store =
        (owner, id, operation) -> {
          var change = operation.apply(previous, 10L);
          assertThat(change.event()).isNull();
          return change.snapshot();
        };
    assertThat(
            new ChangeProjectStatus(store, Clock.fixed(now, ZoneOffset.UTC), 3)
                .execute("owner", original.id(), new ProjectRevision(original.id(), 4), state))
        .isSameAs(previous);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s9_staleOrForeignRevisionFailsBeforeNoop(boolean wrongId) {
    var original = Project.create(UUID.randomUUID(), "owner", "Name", "", Instant.EPOCH);
    ProjectStatusEditing store =
        (owner, id, operation) -> operation.apply(new ProjectSnapshot(original, 4), 0L).snapshot();
    var revision =
        new ProjectRevision(wrongId ? UUID.randomUUID() : original.id(), wrongId ? 4 : 3);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ChangeProjectStatus(store, Clock.fixed(now, ZoneOffset.UTC), 3)
                    .execute("owner", original.id(), revision, "idea"))
        .isInstanceOf(ProjectConflictException.class);
  }

  @ParameterizedTest
  @CsvSource({"3,3", "3,2", "10,10"})
  void s4_s8_capacityCountsCurrentOwnerAndRejectsAtOrAboveLimit(long count, int limit) {
    var original = Project.create(UUID.randomUUID(), "owner", "Name", "", Instant.EPOCH);
    ProjectStatusEditing store =
        (owner, id, operation) ->
            operation.apply(new ProjectSnapshot(original, 0), count).snapshot();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ChangeProjectStatus(store, Clock.fixed(now, ZoneOffset.UTC), limit)
                    .execute(
                        "owner", original.id(), new ProjectRevision(original.id(), 0), "active"))
        .isInstanceOf(ActiveProjectLimitException.class)
        .satisfies(
            error -> {
              var failure = (ActiveProjectLimitException) error;
              assertThat(failure.activeCount()).isEqualTo(count);
              assertThat(failure.limit()).isEqualTo(limit);
            });
  }
}
