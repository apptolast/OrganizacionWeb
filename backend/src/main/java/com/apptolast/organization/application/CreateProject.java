package com.apptolast.organization.application;

import com.apptolast.organization.domain.Project;
import java.time.Clock;
import java.util.UUID;

public final class CreateProject implements CreateProjectUseCase {
  private final ProjectCommit commit;
  private final Clock clock;

  public CreateProject(ProjectCommit commit, Clock clock) {
    this.commit = commit;
    this.clock = clock;
  }

  public Project execute(String ownerId, String name, String description) {
    Project project =
        Project.create(
            UUID.randomUUID(),
            ownerId,
            name,
            description,
            clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    ProjectCreated event =
        new ProjectCreated(
            UUID.randomUUID(),
            project.id(),
            ownerId,
            project.createdAt(),
            1,
            project.name(),
            "ProjectCreated.v1");
    commit.save(project, event);
    return project;
  }
}
