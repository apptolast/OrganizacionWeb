package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.UUID;
import java.util.function.Function;

public interface SubtaskCommit {
  Task save(
      String ownerId, UUID projectId, UUID parentId, Function<String, TaskCreation> operation);
}
