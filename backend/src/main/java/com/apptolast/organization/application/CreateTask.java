package com.apptolast.organization.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.apptolast.organization.domain.Task;
import java.time.Clock;
import java.util.UUID;

public final class CreateTask implements CreateTaskUseCase {
  private final TaskCommit commit;
  private final Clock clock;

  public CreateTask(TaskCommit commit, Clock clock) {
    this.commit = commit;
    this.clock = clock;
  }

  public Task execute(
      String ownerId, UUID projectId, String title, String criterion, Integer minutes) {
    return commit.save(
        ownerId,
        projectId,
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
              new TaskCreated(
                  UUID.randomUUID(),
                  projectId,
                  ownerId,
                  task.createdAt(),
                  1,
                  "TaskCreated.v1",
                  task.id(),
                  task.title()));
        });
  }
}
