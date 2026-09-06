package com.apptolast.organization.application;

import com.apptolast.organization.domain.TaskSnapshot;
import java.util.UUID;

public final class ReadTaskStatus implements ReadTaskStatusUseCase {
  private final TaskStatusQueries queries;

  public ReadTaskStatus(TaskStatusQueries queries) {
    this.queries = queries;
  }

  public TaskSnapshot status(String owner, UUID project, UUID id) {
    return queries.status(owner, project, id);
  }
}
