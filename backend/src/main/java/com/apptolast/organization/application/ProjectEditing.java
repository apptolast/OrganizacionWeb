package com.apptolast.organization.application;

import com.apptolast.organization.domain.ProjectSnapshot;
import java.util.UUID;
import java.util.function.Function;

public interface ProjectEditing {
  ProjectSnapshot update(
      String ownerId, UUID id, Function<ProjectSnapshot, ProjectChange> operation);
}
