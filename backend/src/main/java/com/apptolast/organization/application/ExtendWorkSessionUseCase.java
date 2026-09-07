package com.apptolast.organization.application;

import java.util.UUID;

public interface ExtendWorkSessionUseCase {
  WorkSessionTransitionConfirmation extend(
      String owner, UUID session, UUID key, WorkSessionRevision expected, int additionalMinutes);
}
