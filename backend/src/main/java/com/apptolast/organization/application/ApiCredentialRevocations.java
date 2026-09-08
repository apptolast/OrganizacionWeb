package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public interface ApiCredentialRevocations {
  Optional<ApiCredential> revoke(String owner, UUID id, Supplier<Instant> now);
}
