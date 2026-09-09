package com.apptolast.organization.application;

import java.util.UUID;

public interface DeleteAutomationUseCase {
  void delete(String owner, UUID id, long expectedVersion);
}
