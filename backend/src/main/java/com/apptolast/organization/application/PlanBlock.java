package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;

public final class PlanBlock implements PlanBlockUseCase {
  private final BlockPlanning store;
  private final BlockCommit commit;
  private final ZoneCatalog catalog;
  private final Clock clock;

  public PlanBlock(BlockPlanning store, BlockCommit commit, ZoneCatalog catalog, Clock clock) {
    this.store = store;
    this.commit = commit;
    this.catalog = catalog;
    this.clock = clock;
  }

  public BlockPreview preview(String owner, UUID projectId, UUID taskId, BlockRequest request) {
    return store.preview(
        owner,
        projectId,
        taskId,
        context ->
            evaluate(
                context,
                context.availability().orElseThrow(AvailabilityRequiredException::new),
                request,
                clock.instant(),
                catalog,
                null));
  }

  public BlockCreation create(
      String owner,
      UUID projectId,
      UUID taskId,
      UUID key,
      AvailabilityRevision expected,
      BlockRequest request) {
    return commit.commit(
        owner,
        projectId,
        taskId,
        key,
        request,
        context -> {
          var availability = context.availability().orElseThrow(AvailabilityRequiredException::new);
          if (!availability.id().equals(expected.id())
              || availability.version() != expected.version())
            throw new AvailabilityConflictException();
          var observed = clock.instant();
          var preview = evaluate(context, availability, request, observed, catalog, null);
          if (!request.allowOverBudget()
              && preview.days().stream().anyMatch(day -> day.excessSeconds() > 0))
            throw new BlockBudgetExceededException(availability.zoneId(), preview.days());
          var now = observed.truncatedTo(ChronoUnit.MICROS);
          var time = preview.time();
          var block = new PlannedBlock(UUID.randomUUID(), projectId, taskId, request, time, now);
          return new BlockChange(
              block,
              new BlockPlanned(
                  UUID.randomUUID(),
                  projectId,
                  owner,
                  now,
                  1,
                  "BlockPlanned.v1",
                  block.id(),
                  taskId,
                  time.startAt(),
                  time.endAt(),
                  request.zoneId(),
                  time.durationMinutes()));
        });
  }

  static BlockPreview evaluate(
      BlockPlanningContext context,
      Availability availability,
      BlockRequest request,
      Instant now,
      ZoneCatalog catalog,
      PlannedBlock previous) {
    if (context.projectStatus().equals("completed")) throw new ProjectCompletedException();
    if (context.taskStatus().equals("completed")) throw new TaskCompletedException();
    try {
      ZoneId.of(availability.zoneId());
    } catch (DateTimeException error) {
      throw new AvailabilityZoneUnavailableException();
    }
    var time = ResolvedBlockTime.resolve(request, catalog.zones(), now);
    if (previous != null
        && previous.time().startAt().equals(time.startAt())
        && previous.time().endAt().equals(time.endAt())
        && previous.request().zoneId().equals(request.zoneId()))
      throw new BlockStateException("BLOCK_UNCHANGED");
    context.blocks().stream()
        .filter(
            block ->
                block.time().startAt().isBefore(time.endAt())
                    && block.time().endAt().isAfter(time.startAt()))
        .min(
            Comparator.comparing((PlannedBlock block) -> block.time().startAt())
                .thenComparing(block -> block.id().toString()))
        .ifPresent(
            block -> {
              throw new BlockOverlapException(block);
            });
    var days =
        BlockBudget.calculate(
            availability,
            time.startAt(),
            time.endAt(),
            context.blocks().stream().map(PlannedBlock::time).toList());
    return new BlockPreview(
        request,
        time,
        new AvailabilityRevision(availability.id(), availability.version()),
        availability.zoneId(),
        days);
  }
}
