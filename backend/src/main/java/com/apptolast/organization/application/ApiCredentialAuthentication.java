package com.apptolast.organization.application;

import java.time.Instant;
import java.util.*;

public interface ApiCredentialAuthentication {
  Optional<ApiCredentialAccess> authenticate(UUID id, byte[] verifier, Instant now);
}
