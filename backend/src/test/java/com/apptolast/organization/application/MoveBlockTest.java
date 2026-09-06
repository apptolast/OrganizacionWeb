package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class MoveBlockTest {
  private final UUID project = UUID.randomUUID();
  private final UUID task = UUID.randomUUID();
  private final Instant now = Instant.parse("2030-01-07T09:00:00Z");

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "1,2,cancelled,BLOCK_CONFLICT,false",
    "1,2,cancelled,BLOCK_CONFLICT,true",
    "2,2,cancelled,BLOCK_CANCELLED,false",
    "2,2,cancelled,BLOCK_CANCELLED,true",
    "9223372036854775807,9223372036854775807,planned,BLOCK_VERSION_EXHAUSTED,false",
    "9223372036854775807,9223372036854775807,planned,BLOCK_VERSION_EXHAUSTED,true"
  })
  void s6_revisionStateAndExhaustionPrecedeMissingAvailability(
      long expected, long version, String status, String code, boolean commit) {
    var prior = block("10:00", "11:00", task);
    var context =
        new MoveContext(
            new BlockState(prior, version, status, now),
            new BlockPlanningContext("completed", "completed", Optional.empty(), List.of(prior)));
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(now);
    var service = new MoveBlock(store(context), () -> Set.of("UTC"), clock);
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:00"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> {
              if (commit)
                service.move(
                    "owner",
                    project,
                    task,
                    prior.id(),
                    UUID.randomUUID(),
                    expected,
                    new AvailabilityRevision(UUID.randomUUID(), 0),
                    request);
              else service.preview("owner", project, task, prior.id(), expected, request);
            })
        .isInstanceOf(BlockStateException.class)
        .hasMessage(code);
    verifyNoInteractions(clock);
  }

  private BlockMoving store(MoveContext context) {
    var store = mock(BlockMoving.class);
    when(store.preview(anyString(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Function<MoveContext, BlockPreview> operation = invocation.getArgument(4);
              return operation.apply(context);
            });
    when(store.commit(anyString(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Function<MoveContext, BlockMutation> operation = invocation.getArgument(6);
              return new BlockChangeConfirmation(operation.apply(context).receipt(), false);
            });
    return store;
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s6_availabilityIdentityAndVersionPrecedeCompletedProject(boolean wrongIdentity) {
    var prior = block("10:00", "11:00", task);
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "completed", "completed", Optional.of(availability), List.of(prior)));
    var expected =
        new AvailabilityRevision(
            wrongIdentity ? UUID.randomUUID() : availability.id(), wrongIdentity ? 2 : 1);
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:00"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new MoveBlock(store(context), () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC))
                    .move(
                        "owner",
                        project,
                        task,
                        prior.id(),
                        UUID.randomUUID(),
                        1,
                        expected,
                        request))
        .isInstanceOf(AvailabilityConflictException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s10_unchangedResolvedDestinationPrecedesOverlap(boolean commit) {
    var prior = block("10:00", "11:00", task);
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active",
                "pending",
                Optional.of(availability),
                List.of(prior, block("10:15", "10:45", UUID.randomUUID()))));
    var service =
        new MoveBlock(store(context), () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC));
    var request =
        new BlockMoveRequest(
            prior.request().startLocal(),
            prior.request().endLocal(),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            true);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> {
              if (commit)
                service.move(
                    "owner",
                    project,
                    task,
                    prior.id(),
                    UUID.randomUUID(),
                    1,
                    new AvailabilityRevision(availability.id(), 2),
                    request);
              else service.preview("owner", project, task, prior.id(), 1, request);
            })
        .isInstanceOf(BlockStateException.class)
        .hasMessage("BLOCK_UNCHANGED");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s22_commitRequiresSpecificBudgetConsentWithoutCountingPriorInterval(boolean consent) {
    var prior = block("10:00", "11:00", task);
    var original = availability();
    var limits = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) limits.put(day, 0);
    var availability = new Availability(original.id(), "owner", "UTC", limits, 2, now, now);
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active",
                "pending",
                Optional.of(availability),
                List.of(prior, block("14:00", "14:30", UUID.randomUUID()))));
    var service =
        new MoveBlock(store(context), () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC));
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:00"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            consent);
    var days = service.preview("owner", project, task, prior.id(), 1, request).days();
    assertThat(days).containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 0, 1800, 3600, 5400));
    if (consent)
      assertThat(
              service
                  .move(
                      "owner",
                      project,
                      task,
                      prior.id(),
                      UUID.randomUUID(),
                      1,
                      new AvailabilityRevision(availability.id(), 2),
                      request)
                  .receipt()
                  .after()
                  .time()
                  .durationMinutes())
          .isEqualTo(60);
    else
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () ->
                  service.move(
                      "owner",
                      project,
                      task,
                      prior.id(),
                      UUID.randomUUID(),
                      1,
                      new AvailabilityRevision(availability.id(), 2),
                      request))
          .isInstanceOfSatisfying(
              BlockBudgetExceededException.class,
              error -> {
                assertThat(error.budgetZoneId()).isEqualTo("UTC");
                assertThat(error.days()).isEqualTo(days);
              });
  }

  @Test
  void s7_previewExcludesOnlyMovedIdentityAndKeepsOtherTaskReservations() {
    var prior = block("10:00", "11:00", task);
    var other = block("14:00", "14:30", UUID.randomUUID());
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active", "pending", Optional.of(availability), List.of(prior, other)));
    var store = mock(BlockMoving.class);
    when(store.preview(eq("owner"), eq(project), eq(task), eq(prior.id()), any()))
        .thenAnswer(
            invocation -> {
              Function<MoveContext, BlockPreview> operation = invocation.getArgument(4);
              return operation.apply(context);
            });
    var service = new MoveBlock(store, () -> Set.of("UTC"), Clock.fixed(now, ZoneOffset.UTC));
    var result =
        service.preview(
            "owner",
            project,
            task,
            prior.id(),
            1,
            new BlockMoveRequest(
                LocalDateTime.parse("2030-01-07T10:30"),
                LocalDateTime.parse("2030-01-07T11:30"),
                "UTC",
                null,
                null,
                false));
    assertThat(result.request().objective()).isEqualTo("Objetivo original");
    assertThat(result.time().startAt()).isEqualTo(Instant.parse("2030-01-07T10:30:00Z"));
    assertThat(result.time().durationMinutes()).isEqualTo(60);
    assertThat(result.availabilityRevision())
        .isEqualTo(new AvailabilityRevision(availability.id(), 2));
    assertThat(result.days())
        .containsExactly(new BudgetDay(LocalDate.of(2030, 1, 7), 120, 1800, 3600, 0));
    assertThat(context.state()).isEqualTo(BlockState.initial(prior));
    verify(store).preview(eq("owner"), eq(project), eq(task), eq(prior.id()), any());
    verifyNoMoreInteractions(store);
  }

  private PlannedBlock block(String start, String end, UUID taskId) {
    var request =
        new BlockRequest(
            "Objetivo original",
            LocalDateTime.parse("2030-01-07T" + start),
            LocalDateTime.parse("2030-01-07T" + end),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    return new PlannedBlock(
        UUID.randomUUID(),
        project,
        taskId,
        request,
        ResolvedBlockTime.resolve(request, Set.of("UTC"), now),
        now.minusSeconds(86400));
  }

  private Availability availability() {
    var limits = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) limits.put(day, 120);
    return new Availability(UUID.randomUUID(), "owner", "UTC", limits, 2, now, now);
  }

  @Test
  void s11_moveCreatesOneReceiptAndEventWhilePreservingBlockIdentity() {
    var prior = block("10:00", "11:00", task);
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active", "pending", Optional.of(availability), List.of(prior)));
    var key = UUID.randomUUID();
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:30"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var mutations = new ArrayList<BlockMutation>();
    var store = mock(BlockMoving.class);
    when(store.commit(
            eq("owner"), eq(project), eq(task), eq(prior.id()), eq(key), eq(request), any()))
        .thenAnswer(
            invocation -> {
              Function<MoveContext, BlockMutation> operation = invocation.getArgument(6);
              var mutation = operation.apply(context);
              mutations.add(mutation);
              return new BlockChangeConfirmation(mutation.receipt(), false);
            });
    var clock = mock(Clock.class);
    var observed = now.plusNanos(123456789);
    when(clock.instant()).thenReturn(observed);
    var result =
        new MoveBlock(store, () -> Set.of("UTC"), clock)
            .move(
                "owner",
                project,
                task,
                prior.id(),
                key,
                1,
                new AvailabilityRevision(availability.id(), 2),
                request);
    assertThat(result.replayed()).isFalse();
    assertThat(mutations).hasSize(1);
    var mutation = mutations.getFirst();
    var after = mutation.state().block();
    assertThat(after.id()).isEqualTo(prior.id());
    assertThat(after.projectId()).isEqualTo(project);
    assertThat(after.taskId()).isEqualTo(task);
    assertThat(after.createdAt()).isEqualTo(prior.createdAt());
    assertThat(after.request().objective()).isEqualTo(prior.request().objective());
    assertThat(after.time().durationMinutes()).isEqualTo(90);
    assertThat(after.time().startAt()).isEqualTo(Instant.parse("2030-01-07T12:00:00Z"));
    assertThat(mutation.state().version()).isEqualTo(2);
    assertThat(mutation.state().status()).isEqualTo("planned");
    var instant = observed.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    assertThat(mutation.state().updatedAt()).isEqualTo(instant);
    assertThat(result.receipt())
        .isEqualTo(
            new BlockChangeReceipt(
                result.receipt().id(), prior.id(), "RESCHEDULED", 2, instant, prior, after));
    assertThat(result.receipt().id()).isNotNull();
    assertThat(mutation.event())
        .isEqualTo(
            new BlockChanged(
                mutation.event().eventId(),
                project,
                "owner",
                instant,
                1,
                "BlockChanged.v1",
                result.receipt().id(),
                prior.id(),
                task,
                "RESCHEDULED",
                2,
                BlockChanged.Interval.from(prior),
                BlockChanged.Interval.from(after)));
    assertThat(mutation.event().eventId()).isNotNull().isNotEqualTo(result.receipt().id());
    verify(clock).instant();
  }

  @Test
  void s15_confirmedReplayReturnsHistoricalReceiptWithoutClockOrCatalog() {
    var prior = block("10:00", "11:00", task);
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:00"),
            "Unknown/Historical",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var after =
        new PlannedBlock(
            prior.id(),
            project,
            task,
            request.withObjective(prior.request().objective()),
            new ResolvedBlockTime(
                Instant.parse("2030-01-07T12:00:00Z"),
                Instant.parse("2030-01-07T13:00:00Z"),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                60),
            prior.createdAt());
    var receipt =
        new BlockChangeReceipt(UUID.randomUUID(), prior.id(), "RESCHEDULED", 2, now, prior, after);
    var confirmed = new BlockChangeConfirmation(receipt, true);
    var key = UUID.randomUUID();
    var store = mock(BlockMoving.class);
    when(store.commit(
            eq("owner"), eq(project), eq(task), eq(prior.id()), eq(key), eq(request), any()))
        .thenReturn(confirmed);
    var clock = mock(Clock.class);
    var catalog = mock(ZoneCatalog.class);
    assertThat(
            new MoveBlock(store, catalog, clock)
                .move(
                    "owner",
                    project,
                    task,
                    prior.id(),
                    key,
                    1,
                    new AvailabilityRevision(UUID.randomUUID(), 1),
                    request))
        .isSameAs(confirmed);
    verifyNoInteractions(clock, catalog);
    assertThat(request.zoneId()).isEqualTo("Unknown/Historical");
  }

  @Test
  void s8_confirmationRechecksClockAfterAValidPreview() {
    var prior = block("10:00", "11:00", task);
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active", "pending", Optional.of(availability), List.of(prior)));
    var observed = new java.util.concurrent.atomic.AtomicReference<>(now);
    var clock = mock(Clock.class);
    when(clock.instant()).thenAnswer(invocation -> observed.get());
    var service = new MoveBlock(store(context), () -> Set.of("UTC"), clock);
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T12:00"),
            LocalDateTime.parse("2030-01-07T13:00"),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            false);
    var preview = service.preview("owner", project, task, prior.id(), 1, request);
    observed.set(preview.time().startAt().plusSeconds(1));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.move(
                    "owner",
                    project,
                    task,
                    prior.id(),
                    UUID.randomUUID(),
                    1,
                    preview.availabilityRevision(),
                    request))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("startLocal");
              assertThat(error.errors().getFirst().code()).isEqualTo("IN_PAST");
            });
    verify(clock, times(2)).instant();
  }

  @Test
  void s10_changingOnlyZoneIsEffectiveEvenWhenInstantsStayEqual() {
    var prior = block("10:00", "11:00", task);
    var availability = availability();
    var context =
        new MoveContext(
            BlockState.initial(prior),
            new BlockPlanningContext(
                "active", "pending", Optional.of(availability), List.of(prior)));
    var request =
        new BlockMoveRequest(
            LocalDateTime.parse("2030-01-07T11:00"),
            LocalDateTime.parse("2030-01-07T12:00"),
            "Europe/Madrid",
            ZoneOffset.ofHours(1),
            ZoneOffset.ofHours(1),
            false);
    var result =
        new MoveBlock(
                store(context),
                () -> Set.of("UTC", "Europe/Madrid"),
                Clock.fixed(now, ZoneOffset.UTC))
            .move(
                "owner",
                project,
                task,
                prior.id(),
                UUID.randomUUID(),
                1,
                new AvailabilityRevision(availability.id(), 2),
                request);
    assertThat(result.receipt().after().time().startAt()).isEqualTo(prior.time().startAt());
    assertThat(result.receipt().after().time().endAt()).isEqualTo(prior.time().endAt());
    assertThat(result.receipt().after().request().zoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.receipt().version()).isEqualTo(2);
  }
}
