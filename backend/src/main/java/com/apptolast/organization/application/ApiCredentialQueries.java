package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.util.Optional;
import java.util.UUID;

public interface ApiCredentialQueries {
  Optional<ApiCredential> find(String owner, UUID id);

  com.apptolast.organization.domain.ApiCredentialPage list(String owner, String cursor);
}
