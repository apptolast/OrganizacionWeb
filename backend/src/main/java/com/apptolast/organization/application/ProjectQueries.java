package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.List;

public interface ProjectQueries {
  List<ProjectSummary> list(String ownerId, ProjectPosition after, int limit);

  java.util.Optional<Project> find(String ownerId, java.util.UUID id);
}
