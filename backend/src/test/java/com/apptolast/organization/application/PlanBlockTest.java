package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class PlanBlockTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s7_rechecksServerClockAfterPreviewAndAcceptsExactEquality(boolean advanced) {
    var preference = availability();
    var context = new BlockPlanningContext("active", "pending", Optional.of(preference), List.of());
    BlockPlanning store = (owner, p, t, operation) -> operation.apply(context);
    BlockCommit commit =
        (owner, p, t, key, intent, operation) ->
            new BlockCreation(operation.apply(context).block(), false);
    var intention =
        new BlockRequest(
            "Meta",
            request().startLocal(),
            request().endLocal(),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var instant = intention.startLocal().toInstant(ZoneOffset.UTC);
    var observed = new java.util.concurrent.atomic.AtomicReference<>(instant);
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenAnswer(invocation -> observed.get());
    var service = new PlanBlock(store, commit, () -> Set.of("UTC"), clock);
    var preview = service.preview("owner", project, task, intention);
    assertThat(preview.time().startAt()).isEqualTo(instant);
    observed.set(instant.plusSeconds(advanced ? 1 : 0));
    if (advanced) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  service.create(
                      "owner",
                      project,
                      task,
                      UUID.randomUUID(),
                      preview.availabilityRevision(),
                      intention))
          .isInstanceOfSatisfying(
              ValidationException.class,
              error -> {
                assertThat(error.errors()).hasSize(1);
                assertThat(error.errors().getFirst().field()).isEqualTo("startLocal");
                assertThat(error.errors().getFirst().code()).isEqualTo("IN_PAST");
              });
    } else {
      var created =
          service.create(
              "owner", project, task, UUID.randomUUID(), preview.availabilityRevision(), intention);
      assertThat(created.block().time()).isEqualTo(preview.time());
      assertThat(created.block().createdAt()).isEqualTo(instant);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "09:00,10:00,120,false,0",
    "11:00,12:00,120,false,0",
    "09:00,10:00,0,true,7200"
  })
  void s13_s15_allowsAdjacentBlocksAtBudgetBoundaryOrWithSpecificConsent(
      String start, String end, int budget, boolean consent, long excess) {
    var current = availability();
    var limits = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) limits.put(day, budget);
    var preference = new Availability(current.id(), "owner", "UTC", limits, 2, now, now);
    var oldRequest =
        new BlockRequest(
            "Anterior",
            LocalDateTime.parse("2030-01-07T" + start),
            LocalDateTime.parse("2030-01-07T" + end),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var existing =
        new PlannedBlock(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            oldRequest,
            ResolvedBlockTime.resolve(oldRequest, Set.of("UTC"), now),
            now);
    var context =
        new BlockPlanningContext("active", "pending", Optional.of(preference), List.of(existing));
    BlockPlanning store = (owner, p, t, operation) -> operation.apply(context);
    BlockCommit commit =
        (owner, p, t, key, intent, operation) ->
            new BlockCreation(operation.apply(context).block(), false);
    var intention =
        new BlockRequest(
            "Meta",
            request().startLocal(),
            request().endLocal(),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            consent);
    var service =
        new PlanBlock(store, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC));
    assertThat(service.preview("owner", project, task, intention).days())
        .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), budget, 3600, 3600, excess));
    var created =
        service.create(
            "owner",
            project,
            task,
            UUID.randomUUID(),
            new AvailabilityRevision(preference.id(), 2),
            intention);
    assertThat(created.replayed()).isFalse();
    assertThat(created.block().request()).isEqualTo(intention);
  }

  final UUID project = UUID.randomUUID(), task = UUID.randomUUID();
  final Instant now = Instant.parse("2030-01-01T00:00:00Z");

  Availability availability() {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, 120);
    return new Availability(UUID.randomUUID(), "owner", "UTC", days, 2, now, now);
  }

  BlockRequest request() {
    return new BlockRequest(
        "Meta",
        LocalDateTime.parse("2030-01-07T10:00"),
        LocalDateTime.parse("2030-01-07T11:00"),
        "UTC",
        null,
        null,
        false);
  }

  @Test
  void s1_previewsCoherentAvailabilityAndTimeWithoutWriting() {
    var preference = availability();
    BlockPlanning store =
        (owner, projectId, taskId, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(projectId).isEqualTo(project);
          assertThat(taskId).isEqualTo(task);
          return operation.apply(
              new BlockPlanningContext("active", "pending", Optional.of(preference), List.of()));
        };
    var preview =
        new PlanBlock(store, null, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
            .preview("owner", project, task, request());
    assertThat(preview.request().objective()).isEqualTo("Meta");
    assertThat(preview.time().startAt()).isEqualTo(Instant.parse("2030-01-07T10:00:00Z"));
    assertThat(preview.time().durationMinutes()).isEqualTo(60);
    assertThat(preview.availabilityRevision())
        .isEqualTo(new AvailabilityRevision(preference.id(), 2));
    assertThat(preview.budgetZoneId()).isEqualTo("UTC");
    assertThat(preview.days())
        .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 0, 3600, 0));
  }

  @Test
  void s17_requiresAvailabilityBeforeEvaluatingCompletedResources() {
    BlockPlanning store =
        (owner, p, t, operation) ->
            operation.apply(
                new BlockPlanningContext("completed", "completed", Optional.empty(), List.of()));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(store, null, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .preview("owner", project, task, request()))
        .isInstanceOf(AvailabilityRequiredException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "completed,pending,project",
    "active,completed,task",
    "completed,completed,project"
  })
  void s17_rejectsCompletedProjectBeforeCompletedTask(
      String projectStatus, String taskStatus, String resource) {
    BlockPlanning store =
        (owner, p, t, operation) ->
            operation.apply(
                new BlockPlanningContext(
                    projectStatus, taskStatus, Optional.of(availability()), List.of()));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(store, null, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .preview("owner", project, task, request()))
        .isInstanceOf(
            resource.equals("project")
                ? ProjectCompletedException.class
                : TaskCompletedException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s9_reportsUnavailableHistoricalBudgetZoneWithoutRewritingIt(boolean create) {
    var current = availability();
    var historical =
        new Availability(
            current.id(),
            current.ownerId(),
            "Historical/Removed",
            current.dailyMinutes(),
            current.version(),
            current.createdAt(),
            current.updatedAt());
    BlockPlanning store =
        (owner, p, t, operation) ->
            operation.apply(
                new BlockPlanningContext("active", "pending", Optional.of(historical), List.of()));
    BlockCommit commit =
        (owner, p, t, key, intent, operation) -> {
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      "active", "pending", Optional.of(historical), List.of()));
          return new BlockCreation(change.block(), false);
        };
    var service =
        new PlanBlock(store, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> {
              if (create)
                service.create(
                    "owner",
                    project,
                    task,
                    UUID.randomUUID(),
                    new AvailabilityRevision(historical.id(), 2),
                    request());
              else service.preview("owner", project, task, request());
            })
        .isInstanceOf(AvailabilityZoneUnavailableException.class);
  }

  @Test
  void s2_createsBlockAndProjectEventFromOneClockAndContext() {
    var preference = availability();
    var key = UUID.randomUUID();
    var intention =
        new BlockRequest(
            " Meta ",
            request().startLocal(),
            request().endLocal(),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    BlockCommit commit =
        (owner, p, t, requestKey, submitted, operation) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(p).isEqualTo(project);
          assertThat(t).isEqualTo(task);
          assertThat(requestKey).isEqualTo(key);
          assertThat(submitted).isEqualTo(intention);
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      "active", "pending", Optional.of(preference), List.of()));
          assertThat(change.block().id()).isNotNull();
          assertThat(change.block().projectId()).isEqualTo(project);
          assertThat(change.block().taskId()).isEqualTo(task);
          assertThat(change.block().request()).isEqualTo(intention);
          assertThat(change.event().aggregateId()).isEqualTo(project);
          assertThat(change.event().taskId()).isEqualTo(task);
          assertThat(change.event().blockId()).isEqualTo(change.block().id());
          assertThat(change.event().ownerId()).isEqualTo("owner");
          assertThat(change.event().type()).isEqualTo("BlockPlanned.v1");
          assertThat(change.event().schemaVersion()).isEqualTo(1);
          assertThat(change.event().occurredAt()).isEqualTo(now);
          assertThat(change.block().createdAt()).isEqualTo(now);
          assertThat(change.event().eventId()).isNotNull();
          assertThat(change.event().startAt()).isEqualTo(change.block().time().startAt());
          assertThat(change.event().endAt()).isEqualTo(change.block().time().endAt());
          assertThat(change.event().zoneId()).isEqualTo("UTC");
          assertThat(change.event().durationMinutes()).isEqualTo(60);
          return new BlockCreation(change.block(), false);
        };
    var created =
        new PlanBlock(null, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
            .create(
                "owner",
                project,
                task,
                key,
                new AvailabilityRevision(preference.id(), 2),
                intention);
    assertThat(created.replayed()).isFalse();
    assertThat(created.block().request().objective()).isEqualTo("Meta");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"missing", "version", "identity"})
  void s17_s18_checksAvailabilityAndRevisionBeforeNewCreationEligibility(String defect) {
    var preference = availability();
    BlockCommit commit =
        (owner, p, t, key, intent, operation) -> {
          operation.apply(
              new BlockPlanningContext(
                  "completed",
                  "completed",
                  defect.equals("missing") ? Optional.empty() : Optional.of(preference),
                  List.of()));
          throw new AssertionError("Creation must fail");
        };
    var expected =
        new AvailabilityRevision(
            defect.equals("identity") ? UUID.randomUUID() : preference.id(),
            defect.equals("version") ? 1 : 2);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(null, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .create("owner", project, task, UUID.randomUUID(), expected, request()))
        .isInstanceOf(
            defect.equals("missing")
                ? AvailabilityRequiredException.class
                : AvailabilityConflictException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "completed,pending,project",
    "active,completed,task",
    "completed,completed,project"
  })
  void s17_newCreationChecksEligibility(String projectStatus, String taskStatus, String resource) {
    var preference = availability();
    BlockCommit commit =
        (owner, p, t, key, intent, operation) -> {
          operation.apply(
              new BlockPlanningContext(
                  projectStatus, taskStatus, Optional.of(preference), List.of()));
          throw new AssertionError("Creation must fail");
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(null, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .create(
                        "owner",
                        project,
                        task,
                        UUID.randomUUID(),
                        new AvailabilityRevision(preference.id(), 2),
                        request()))
        .isInstanceOf(
            resource.equals("project")
                ? ProjectCompletedException.class
                : TaskCompletedException.class);
  }

  @Test
  void s15_requiresExplicitConsentForExcessAndReturnsCurrentBudget() {
    var current = availability();
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, 0);
    var preference = new Availability(current.id(), "owner", "UTC", days, 2, now, now);
    BlockCommit commit =
        (owner, p, t, key, intent, operation) -> {
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      "active", "pending", Optional.of(preference), List.of()));
          return new BlockCreation(change.block(), false);
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(null, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .create(
                        "owner",
                        project,
                        task,
                        UUID.randomUUID(),
                        new AvailabilityRevision(preference.id(), 2),
                        request()))
        .isInstanceOfSatisfying(
            BlockBudgetExceededException.class,
            error -> {
              assertThat(error.budgetZoneId()).isEqualTo("UTC");
              assertThat(error.days())
                  .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 0, 0, 3600, 3600));
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s13_s14_rejectsOverlapUsingFirstStartThenUuid(boolean earlierStart) {
    var firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var time =
        new ResolvedBlockTime(
            Instant.parse("2030-01-07T09:30:00Z"),
            Instant.parse("2030-01-07T10:30:00Z"),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            60);
    var storedRequest =
        new BlockRequest(
            "Meta",
            LocalDateTime.parse("2030-01-07T09:30"),
            LocalDateTime.parse("2030-01-07T10:30"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var first =
        new PlannedBlock(firstId, UUID.randomUUID(), UUID.randomUUID(), storedRequest, time, now);
    var second =
        new PlannedBlock(
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"),
            project,
            task,
            storedRequest,
            time,
            now);
    var earlyRequest =
        new BlockRequest(
            "Meta",
            LocalDateTime.parse("2030-01-07T09:00"),
            LocalDateTime.parse("2030-01-07T10:30"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var early =
        new PlannedBlock(
            UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
            project,
            task,
            earlyRequest,
            ResolvedBlockTime.resolve(earlyRequest, Set.of("UTC"), now),
            now);
    var expected = earlierStart ? early : first;
    BlockPlanning store =
        (owner, p, t, operation) ->
            operation.apply(
                new BlockPlanningContext(
                    "active",
                    "pending",
                    Optional.of(availability()),
                    earlierStart ? List.of(second, first, early) : List.of(second, first)));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(store, null, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .preview("owner", project, task, request()))
        .isInstanceOfSatisfying(
            BlockOverlapException.class,
            error -> {
              assertThat(error.conflict().id()).isEqualTo(expected.id());
              assertThat(error.conflict().projectId()).isEqualTo(expected.projectId());
              assertThat(error.conflict().taskId()).isEqualTo(expected.taskId());
            });
  }

  @Test
  void s13_explicitBudgetConsentNeverPermitsOverlap() {
    var preference = availability();
    var intent =
        new BlockRequest(
            "Meta",
            request().startLocal(),
            request().endLocal(),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            true);
    var existing =
        new PlannedBlock(
            UUID.randomUUID(),
            project,
            task,
            intent,
            ResolvedBlockTime.resolve(intent, Set.of("UTC"), now),
            now);
    BlockCommit commit =
        (owner, p, t, key, submitted, operation) -> {
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      "active", "pending", Optional.of(preference), List.of(existing)));
          return new BlockCreation(change.block(), false);
        };
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new PlanBlock(null, commit, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .create(
                        "owner",
                        project,
                        task,
                        UUID.randomUUID(),
                        new AvailabilityRevision(preference.id(), 2),
                        intent))
        .isInstanceOf(BlockOverlapException.class);
  }

  @Test
  void s2_capturesCreationClockOnceBeforeValidationAndPersistence() {
    var preference = availability();
    var reads = new java.util.concurrent.atomic.AtomicInteger();
    Clock advancing =
        new Clock() {
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          public Clock withZone(ZoneId zone) {
            return this;
          }

          public Instant instant() {
            return now.plusSeconds(reads.getAndIncrement());
          }
        };
    BlockCommit commit =
        (owner, p, t, key, intent, operation) -> {
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      "active", "pending", Optional.of(preference), List.of()));
          return new BlockCreation(change.block(), false);
        };
    var result =
        new PlanBlock(null, commit, () -> Set.of("UTC"), advancing)
            .create(
                "owner",
                project,
                task,
                UUID.randomUUID(),
                new AvailabilityRevision(preference.id(), 2),
                new BlockRequest(
                    "Meta",
                    request().startLocal(),
                    request().endLocal(),
                    "UTC",
                    ZoneOffset.UTC,
                    ZoneOffset.UTC,
                    false));
    assertThat(result.block().createdAt()).isEqualTo(now);
    assertThat(reads).hasValue(1);
  }
}
