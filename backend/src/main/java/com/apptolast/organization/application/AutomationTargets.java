package com.apptolast.organization.application;

import java.util.UUID;

/** Answers whether a project exists and belongs to the owner, whatever its status. */
@FunctionalInterface
public interface AutomationTargets {
  boolean ownsProject(String owner, UUID projectId);
}
