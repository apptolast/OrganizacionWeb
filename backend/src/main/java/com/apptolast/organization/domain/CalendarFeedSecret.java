package com.apptolast.organization.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The secret of a calendar feed: 43 base64url characters shown once and never persisted, plus the
 * 32-octet SHA-256 fingerprint that is the only key stored and the only key looked up.
 */
public record CalendarFeedSecret(String token, byte[] fingerprint) {
  private static final int TOKEN_OCTETS = 32;

  public static CalendarFeedSecret issue(SecureRandom random) {
    var bytes = new byte[TOKEN_OCTETS];
    random.nextBytes(bytes);
    var token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return new CalendarFeedSecret(token, fingerprintOf(token));
  }

  /** Accepts any candidate so that resolution never branches on the shape of the path segment. */
  public static byte[] fingerprintOf(String candidate) {
    try {
      return MessageDigest.getInstance("SHA-256")
          .digest(candidate.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  @Override
  public byte[] fingerprint() {
    return fingerprint.clone();
  }
}
