package com.apptolast.organization.application;

import java.util.List;
import java.util.UUID;

public record ApiCredentialAccess(UUID id, String owner, List<String> scopes) {
  public ApiCredentialAccess {
    scopes = List.copyOf(scopes);
  }
}
