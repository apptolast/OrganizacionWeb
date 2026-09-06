package com.apptolast.organization.application;

import java.util.UUID;

public interface StartWorkSessionUseCase {
  WorkSessionConfirmation start(
      String owner, UUID project, UUID task, UUID key, int plannedMinutes);
}
