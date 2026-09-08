package com.apptolast.organization.application;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.*;
import java.util.function.Predicate;

public final class AuthenticateApiCredential implements AuthenticateApiCredentialUseCase {
  private final ApiCredentialAuthentication credentials;
  private final Clock clock;
  private final Predicate<String> enabledOwner;

  public AuthenticateApiCredential(
      ApiCredentialAuthentication credentials, Clock clock, Predicate<String> enabledOwner) {
    this.credentials = credentials;
    this.clock = clock;
    this.enabledOwner = enabledOwner;
  }

  @Override
  public ApiCredentialAccess authenticate(String token) {
    UUID id;
    byte[] secret;
    try {
      if (token == null
          || token.length() != 84
          || !token.startsWith("owp_")
          || token.charAt(40) != '.') throw new IllegalArgumentException();
      id = UUID.fromString(token.substring(4, 40));
      if (!id.toString().equals(token.substring(4, 40))) throw new IllegalArgumentException();
      secret = Base64.getUrlDecoder().decode(token.substring(41));
      if (secret.length != 32
          || !Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(secret)
              .equals(token.substring(41))) throw new IllegalArgumentException();
    } catch (IllegalArgumentException error) {
      throw new ApiUnauthenticatedException();
    }
    try {
      var access =
          credentials
              .authenticate(
                  id,
                  MessageDigest.getInstance("SHA-256").digest(secret),
                  CustomizationTime.capture(clock))
              .orElseThrow(ApiUnauthenticatedException::new);
      if (!enabledOwner.test(access.owner())) throw new ApiUnauthenticatedException();
      return access;
    } catch (NoSuchAlgorithmException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
