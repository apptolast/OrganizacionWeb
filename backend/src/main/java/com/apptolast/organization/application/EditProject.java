package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.UUID;

public final class EditProject implements EditProjectUseCase {
  private final ProjectEditing store;
  private final Clock clock;

  public EditProject(ProjectEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public ProjectSnapshot execute(
      String ownerId, UUID id, ProjectRevision expected, String name, String description) {
    return store.update(
        ownerId,
        id,
        previous -> {
          Project current = previous.project();
          if (!current.id().equals(expected.id()) || previous.version() != expected.version())
            throw new ProjectConflictException();
          Project changed =
              new Project(
                  current.id(),
                  current.ownerId(),
                  name,
                  description,
                  current.status(),
                  current.createdAt(),
                  clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
          if (current.name().equals(changed.name())
              && current.description().equals(changed.description()))
            return new ProjectChange(previous, null);
          return new ProjectChange(
              new ProjectSnapshot(changed, previous.version() + 1),
              new ProjectUpdated(
                  UUID.randomUUID(),
                  changed.id(),
                  changed.ownerId(),
                  changed.updatedAt(),
                  1,
                  changed.name(),
                  "ProjectUpdated.v1"));
        });
  }
}
