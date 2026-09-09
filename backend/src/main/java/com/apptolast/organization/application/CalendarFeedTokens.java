package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Optional;

/** One active token per owner; only its SHA-256 fingerprint is ever stored or looked up. */
public interface CalendarFeedTokens {
  Optional<Instant> createdAt(String owner);

  void replace(String owner, byte[] fingerprint, Instant createdAt);

  void revoke(String owner);

  Optional<String> ownerOf(byte[] fingerprint);
}
