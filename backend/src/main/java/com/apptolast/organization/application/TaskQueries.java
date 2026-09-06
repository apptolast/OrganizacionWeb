package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskPosition;
import java.util.List;
import java.util.UUID;

public interface TaskQueries {
  List<Task> list(String owner, UUID project, TaskPosition after);

  Task detail(String owner, UUID project, UUID id);
}
