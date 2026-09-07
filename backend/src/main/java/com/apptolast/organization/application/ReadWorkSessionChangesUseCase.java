package com.apptolast.organization.application;

import java.util.UUID;

public interface ReadWorkSessionChangesUseCase {
  WorkSessionTransitionReceipt detail(String owner, UUID id);

  WorkSessionTransitionReceipt byRequest(String owner, UUID key);

  WorkSessionTransitionReceipt closure(String owner, UUID session);
}
