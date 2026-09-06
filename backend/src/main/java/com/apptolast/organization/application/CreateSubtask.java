package com.apptolast.organization.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.apptolast.organization.domain.Task;
import java.time.Clock;
import java.util.UUID;

public final class CreateSubtask implements CreateSubtaskUseCase {
  private final SubtaskCommit commit;
  private final Clock clock;

  public CreateSubtask(SubtaskCommit commit, Clock clock) {
    this.commit = commit;
    this.clock = clock;
  }

  public Task execute(
      String ownerId,
      UUID projectId,
      UUID parentId,
      String title,
      String criterion,
      Integer minutes) {
    return commit.save(
        ownerId,
        projectId,
        parentId,
        status -> {
          if ("completed".equals(status)) throw new ProjectCompletedException();
          var task =
              Task.create(
                  UUID.randomUUID(),
                  projectId,
                  title,
                  criterion,
                  minutes,
                  clock.instant().truncatedTo(MICROS));
          return new TaskCreation(
              task,
              new SubtaskCreated(
                  UUID.randomUUID(),
                  projectId,
                  ownerId,
                  task.createdAt(),
                  1,
                  "SubtaskCreated.v1",
                  task.id(),
                  parentId,
                  task.title()));
        });
  }
}
