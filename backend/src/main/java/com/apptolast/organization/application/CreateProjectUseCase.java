package com.apptolast.organization.application;

import com.apptolast.organization.domain.Project;

@FunctionalInterface
public interface CreateProjectUseCase {
  Project execute(String ownerId, String name, String description);
}
