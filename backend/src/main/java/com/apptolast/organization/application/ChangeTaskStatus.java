package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskRevision;
import com.apptolast.organization.domain.TaskSnapshot;
import java.time.Clock;
import java.util.UUID;

public final class ChangeTaskStatus implements ChangeTaskStatusUseCase {
  private final TaskStatusEditing store;
  private final Clock clock;

  public ChangeTaskStatus(TaskStatusEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public TaskSnapshot execute(
      String owner, UUID project, UUID id, TaskRevision expected, String target) {
    return store.update(
        owner,
        project,
        id,
        previous -> {
          var current = previous.task();
          if (!current.id().equals(expected.id()) || previous.version() != expected.version())
            throw new TaskConflictException();
          if (current.status().equals(target)) return new TaskStatusChange(previous, null);
          var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
          if (now.isBefore(current.updatedAt())) now = current.updatedAt();
          var changed =
              new Task(
                  current.id(),
                  current.projectId(),
                  current.title(),
                  current.completionCriterion(),
                  current.estimatedMinutes(),
                  target,
                  current.createdAt(),
                  now);
          return new TaskStatusChange(
              new TaskSnapshot(
                  changed, previous.version() + 1, target.equals("completed") ? now : null),
              new TaskStatusChanged(
                  UUID.randomUUID(),
                  project,
                  owner,
                  now,
                  1,
                  "TaskStatusChanged.v1",
                  id,
                  current.status(),
                  target));
        });
  }
}
