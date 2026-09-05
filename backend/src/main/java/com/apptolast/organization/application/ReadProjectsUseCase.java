package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public interface ReadProjectsUseCase {
  ProjectPage list(String ownerId, ProjectPosition after);

  Project detail(String ownerId, java.util.UUID id);
}
