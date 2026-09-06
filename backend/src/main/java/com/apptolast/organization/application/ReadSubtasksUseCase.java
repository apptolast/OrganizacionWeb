package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.Optional;
import java.util.UUID;

public interface ReadSubtasksUseCase {
  com.apptolast.organization.domain.TaskPage list(
      String owner,
      UUID project,
      UUID parent,
      com.apptolast.organization.domain.TaskPosition after);

  Optional<Task> parent(String owner, UUID project, UUID id);
}
