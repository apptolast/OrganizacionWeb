package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskPage;
import com.apptolast.organization.domain.TaskPosition;
import java.util.UUID;

public interface ReadTasksUseCase {
  TaskPage list(String owner, UUID project, TaskPosition after);

  Task detail(String owner, UUID project, UUID id);
}
