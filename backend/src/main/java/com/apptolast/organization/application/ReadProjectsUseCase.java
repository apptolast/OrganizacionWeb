package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public interface ReadProjectsUseCase {
  ProjectPage list(String ownerId, ProjectPosition after);

  ProjectSnapshot detail(String ownerId, java.util.UUID id);
}
