package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.time.Clock;
import java.util.*;

public final class RevokeApiCredential implements RevokeApiCredentialUseCase {
  private final ApiCredentialRevocations store;
  private final Clock clock;

  public RevokeApiCredential(ApiCredentialRevocations store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  @Override
  public Optional<ApiCredential> revoke(String owner, UUID id) {
    return store.revoke(owner, id, () -> CustomizationTime.capture(clock));
  }
}
