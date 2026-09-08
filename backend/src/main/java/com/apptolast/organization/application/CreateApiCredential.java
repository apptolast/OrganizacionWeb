package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import com.apptolast.organization.domain.ApiCredentialIntent;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class CreateApiCredential implements CreateApiCredentialUseCase {
  private final ApiCredentialCommit commit;
  private final Clock clock;
  private final SecureRandom random;

  public CreateApiCredential(ApiCredentialCommit commit, Clock clock, SecureRandom random) {
    this.commit = commit;
    this.clock = clock;
    this.random = random;
  }

  @Override
  public ApiCredentialCreation create(
      String owner, UUID id, String name, List<String> scopes, int expiresInDays) {
    var intent = new ApiCredentialIntent(name, scopes, expiresInDays);
    return commit.create(
        owner,
        id,
        intent,
        () -> {
          var now = CustomizationTime.capture(clock);
          java.time.Instant expires;
          try {
            expires = now.plus(expiresInDays, ChronoUnit.DAYS);
            if (expires.atOffset(java.time.ZoneOffset.UTC).getYear() > 9999)
              throw new IllegalArgumentException("Credential expiration outside public range");
          } catch (RuntimeException error) {
            throw new StorageUnavailableException(error);
          }
          var credential =
              new ApiCredential(id, intent.name(), intent.scopes(), now, expires, null);
          var bytes = new byte[32];
          random.nextBytes(bytes);
          try {
            var verifier = MessageDigest.getInstance("SHA-256").digest(bytes);
            var secret =
                "owp_" + id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            return new ApiCredentialIssuance(credential, secret, verifier);
          } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
          }
        });
  }
}
