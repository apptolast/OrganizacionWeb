package com.apptolast.organization.application;

import com.apptolast.organization.domain.WorkSessionState;
import java.util.UUID;
import java.util.function.Function;

public interface WorkSessionChanging {
  WorkSessionTransitionConfirmation commit(
      String owner,
      UUID session,
      UUID key,
      String action,
      WorkSessionRevision expected,
      com.apptolast.organization.domain.WorkSessionCloseNotes notes,
      Function<WorkSessionState, WorkSessionTransition> operation);

  default WorkSessionTransitionConfirmation commit(
      String owner,
      UUID session,
      UUID key,
      String action,
      WorkSessionRevision expected,
      Function<WorkSessionState, WorkSessionTransition> operation) {
    return commit(owner, session, key, action, expected, null, operation);
  }
}
