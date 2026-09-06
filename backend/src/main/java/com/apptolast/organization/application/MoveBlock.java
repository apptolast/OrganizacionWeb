package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.UUID;

public final class MoveBlock implements MoveBlockUseCase {
  private final BlockMoving store;
  private final ZoneCatalog catalog;
  private final Clock clock;

  public MoveBlock(BlockMoving store, ZoneCatalog catalog, Clock clock) {
    this.store = store;
    this.catalog = catalog;
    this.clock = clock;
  }

  public BlockPreview preview(
      String owner, UUID project, UUID task, UUID block, long expected, BlockMoveRequest request) {
    return store.preview(
        owner,
        project,
        task,
        block,
        context -> {
          requireEditable(context.state(), expected);
          return evaluate(
              context,
              context.planning().availability().orElseThrow(AvailabilityRequiredException::new),
              request,
              clock.instant());
        });
  }

  public BlockChangeConfirmation move(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      UUID key,
      long expected,
      AvailabilityRevision availability,
      BlockMoveRequest request) {
    return store.commit(
        owner,
        project,
        task,
        block,
        key,
        request,
        context -> {
          requireEditable(context.state(), expected);
          var currentAvailability =
              context.planning().availability().orElseThrow(AvailabilityRequiredException::new);
          if (!currentAvailability.id().equals(availability.id())
              || currentAvailability.version() != availability.version())
            throw new AvailabilityConflictException();
          var observed = clock.instant();
          var prior = context.state().block();
          var preview = evaluate(context, currentAvailability, request, observed);
          if (!request.allowOverBudget()
              && preview.days().stream().anyMatch(day -> day.excessSeconds() > 0))
            throw new BlockBudgetExceededException(preview.budgetZoneId(), preview.days());
          var after =
              new PlannedBlock(
                  prior.id(),
                  prior.projectId(),
                  prior.taskId(),
                  preview.request(),
                  preview.time(),
                  prior.createdAt());
          var now = observed.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
          var next = new BlockState(after, context.state().version() + 1, "planned", now);
          var receipt =
              new BlockChangeReceipt(
                  UUID.randomUUID(), block, "RESCHEDULED", next.version(), now, prior, after);
          var event =
              new BlockChanged(
                  UUID.randomUUID(),
                  project,
                  owner,
                  now,
                  1,
                  "BlockChanged.v1",
                  receipt.id(),
                  block,
                  task,
                  "RESCHEDULED",
                  next.version(),
                  BlockChanged.Interval.from(prior),
                  BlockChanged.Interval.from(after));
          return new BlockMutation(next, receipt, event);
        });
  }

  private static void requireEditable(BlockState state, long expected) {
    if (state.version() != expected) throw new BlockStateException("BLOCK_CONFLICT");
    if (state.status().equals("cancelled")) throw new BlockStateException("BLOCK_CANCELLED");
    if (state.version() == Long.MAX_VALUE) throw new BlockStateException("BLOCK_VERSION_EXHAUSTED");
  }

  private BlockPreview evaluate(
      MoveContext context,
      Availability availability,
      BlockMoveRequest request,
      java.time.Instant now) {
    var prior = context.state().block();
    var planning = context.planning();
    var filtered =
        new BlockPlanningContext(
            planning.projectStatus(),
            planning.taskStatus(),
            planning.availability(),
            planning.blocks().stream()
                .filter(existing -> !existing.id().equals(prior.id()))
                .toList());
    return PlanBlock.evaluate(
        filtered,
        availability,
        request.withObjective(prior.request().objective()),
        now,
        catalog,
        prior);
  }
}
