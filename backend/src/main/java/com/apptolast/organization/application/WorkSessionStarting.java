package com.apptolast.organization.application;

import java.util.UUID;
import java.util.function.Function;

public interface WorkSessionStarting {
  WorkSessionConfirmation commit(
      String owner,
      UUID project,
      UUID task,
      UUID key,
      int plannedMinutes,
      Function<WorkSessionContext, WorkSessionChange> operation);
}
