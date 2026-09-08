package com.apptolast.organization.application;

import java.time.Instant;
import java.util.function.*;

public interface ApiQuotaAdmission {
  void consume(ApiCredentialAccess access, Supplier<Instant> now, Predicate<String> enabledOwner);
}
