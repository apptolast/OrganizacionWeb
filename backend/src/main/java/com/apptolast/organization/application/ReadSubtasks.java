package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.Optional;
import java.util.UUID;

public final class ReadSubtasks implements ReadSubtasksUseCase {
  private final SubtaskQueries queries;

  public ReadSubtasks(SubtaskQueries queries) {
    this.queries = queries;
  }

  public Optional<Task> parent(String owner, UUID project, UUID id) {
    return queries.parent(owner, project, id);
  }

  public com.apptolast.organization.domain.TaskPage list(
      String owner,
      UUID project,
      UUID parent,
      com.apptolast.organization.domain.TaskPosition after) {
    return ReadTasks.page(queries.list(owner, project, parent, after));
  }
}
