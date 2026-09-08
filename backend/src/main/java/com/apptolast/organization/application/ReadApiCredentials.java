package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.util.Optional;
import java.util.UUID;

public final class ReadApiCredentials implements ReadApiCredentialsUseCase {
  private final ApiCredentialQueries queries;

  public ReadApiCredentials(ApiCredentialQueries queries) {
    this.queries = queries;
  }

  @Override
  public Optional<ApiCredential> find(String owner, UUID id) {
    return queries.find(owner, id);
  }

  @Override
  public com.apptolast.organization.domain.ApiCredentialPage list(String owner, String cursor) {
    return queries.list(owner, cursor);
  }
}
