package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.UUID;

public final class ChangeProjectStatus implements ChangeProjectStatusUseCase {
  private final ProjectStatusEditing store;
  private final Clock clock;
  private final int limit;

  public ChangeProjectStatus(ProjectStatusEditing store, Clock clock, int limit) {
    this.store = store;
    this.clock = clock;
    this.limit = limit;
  }

  public ProjectSnapshot execute(String ownerId, UUID id, ProjectRevision expected, String target) {
    return store.update(
        ownerId,
        id,
        (previous, activeCount) -> {
          var current = previous.project();
          if (!current.id().equals(expected.id()) || previous.version() != expected.version())
            throw new ProjectConflictException();
          if (current.status().equals(target)) return new ProjectStatusChange(previous, null);
          if (!ProjectStates.allows(current.status(), target))
            throw new InvalidProjectTransitionException();
          if (target.equals("active") && activeCount >= limit)
            throw new ActiveProjectLimitException(activeCount, limit);
          var changed =
              new Project(
                  current.id(),
                  current.ownerId(),
                  current.name(),
                  current.description(),
                  target,
                  current.createdAt(),
                  clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
          return new ProjectStatusChange(
              new ProjectSnapshot(changed, previous.version() + 1),
              new ProjectStatusChanged(
                  UUID.randomUUID(),
                  current.id(),
                  current.ownerId(),
                  changed.updatedAt(),
                  1,
                  "ProjectStatusChanged.v1",
                  current.status(),
                  target));
        });
  }
}
