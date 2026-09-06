package com.apptolast.organization.application;

import com.apptolast.organization.domain.TaskSnapshot;
import java.util.UUID;
import java.util.function.Function;

public interface TaskStatusEditing {
  TaskSnapshot update(
      String owner, UUID project, UUID id, Function<TaskSnapshot, TaskStatusChange> operation);
}
