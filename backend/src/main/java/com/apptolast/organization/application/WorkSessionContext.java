package com.apptolast.organization.application;

import java.util.Optional;

public record WorkSessionContext(String projectStatus, String taskStatus, Optional<String> zoneId) {
  public static void requireEligible(String projectStatus, String taskStatus) {
    if ("completed".equals(projectStatus)) throw new ProjectCompletedException();
    if ("completed".equals(taskStatus)) throw new TaskCompletedException();
  }
}
