package com.apptolast.organization.domain;

import java.time.Instant;

public record TaskSnapshot(Task task, long version, Instant completedAt) {
  public TaskSnapshot {
    if (task == null
        || version < 0
        || (task.status().equals("pending")
            ? completedAt != null
            : completedAt == null || !completedAt.equals(task.updatedAt())))
      throw new IllegalArgumentException(
          "Task snapshot requires consistent state, revision and completion date");
  }
}
