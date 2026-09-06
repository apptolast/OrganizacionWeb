package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import java.util.UUID;
import java.util.function.Function;

public interface TaskCommit {
  Task save(String ownerId, UUID projectId, Function<String, TaskCreation> operation);
}
