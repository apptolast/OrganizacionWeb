package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredentialIntent;
import java.util.UUID;
import java.util.function.Supplier;

public interface ApiCredentialCommit {
  ApiCredentialCreation create(
      String owner, UUID id, ApiCredentialIntent intent, Supplier<ApiCredentialIssuance> issue);
}
