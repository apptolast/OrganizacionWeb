package com.apptolast.organization.application;

import java.util.UUID;
import java.util.function.Function;

public interface WorkSessionExtending {
  WorkSessionTransitionConfirmation extend(
      String owner,
      UUID session,
      UUID key,
      WorkSessionRevision expected,
      int additionalMinutes,
      Function<WorkSessionEnd, WorkSessionExtensionTransition> operation);
}
