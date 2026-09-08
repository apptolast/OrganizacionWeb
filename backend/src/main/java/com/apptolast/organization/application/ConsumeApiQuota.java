package com.apptolast.organization.application;

import java.time.Clock;
import java.util.function.Predicate;

public final class ConsumeApiQuota implements ConsumeApiQuotaUseCase {
  private final ApiQuotaAdmission store;
  private final Clock clock;
  private final Predicate<String> enabledOwner;

  public ConsumeApiQuota(ApiQuotaAdmission store, Clock clock, Predicate<String> enabledOwner) {
    this.store = store;
    this.clock = clock;
    this.enabledOwner = enabledOwner;
  }

  @Override
  public void consume(ApiCredentialAccess access) {
    store.consume(access, () -> CustomizationTime.capture(clock), enabledOwner);
  }
}
