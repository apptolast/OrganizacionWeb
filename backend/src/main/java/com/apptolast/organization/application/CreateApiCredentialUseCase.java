package com.apptolast.organization.application;

import java.util.List;
import java.util.UUID;

public interface CreateApiCredentialUseCase {
  ApiCredentialCreation create(
      String owner, UUID id, String name, List<String> scopes, int expiresInDays);
}
