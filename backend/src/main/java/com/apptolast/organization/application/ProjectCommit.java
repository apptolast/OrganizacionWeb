package com.apptolast.organization.application;

import com.apptolast.organization.domain.Project;

/** Atomically persists a project and its creation event; failure leaves neither write. */
@FunctionalInterface
public interface ProjectCommit {
  void save(Project project, ProjectCreated event);
}
