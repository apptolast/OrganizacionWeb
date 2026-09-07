package com.apptolast.organization.application;

import java.util.UUID;

public interface ChangeWorkSessionUseCase {
  WorkSessionTransitionConfirmation close(
      String owner,
      UUID session,
      UUID key,
      WorkSessionRevision expected,
      com.apptolast.organization.domain.WorkSessionCloseNotes notes);

  WorkSessionTransitionConfirmation pause(
      String owner, UUID session, UUID key, WorkSessionRevision expected);

  WorkSessionTransitionConfirmation resume(
      String owner, UUID session, UUID key, WorkSessionRevision expected);
}
