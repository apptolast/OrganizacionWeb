package com.apptolast.organization.application;

import com.apptolast.organization.domain.ProjectSnapshot;
import java.util.UUID;
import java.util.function.BiFunction;

public interface ProjectStatusEditing {
  ProjectSnapshot update(
      String ownerId, UUID id, BiFunction<ProjectSnapshot, Long, ProjectStatusChange> operation);
}
