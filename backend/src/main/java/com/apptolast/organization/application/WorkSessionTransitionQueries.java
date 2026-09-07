package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

public interface WorkSessionTransitionQueries {
  Optional<WorkSessionTransitionReceipt> changeDetail(String owner, UUID id);

  Optional<WorkSessionTransitionReceipt> changeByRequest(String owner, UUID key);

  Optional<WorkSessionTransitionReceipt> closure(String owner, UUID session);
}
