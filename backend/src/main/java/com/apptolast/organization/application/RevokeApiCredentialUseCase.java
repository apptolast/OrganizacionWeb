package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.util.*;

public interface RevokeApiCredentialUseCase {
  Optional<ApiCredential> revoke(String owner, UUID id);
}
