package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateApiCredentialTest {
  @Test
  void s1_s5_issuesOneSecretAndOnlyVerifierAcrossCommitBoundary() throws Exception {
    var id = UUID.randomUUID();
    var now = Instant.parse("2026-09-08T12:34:56.123456789Z");
    var bytes = new byte[32];
    Arrays.fill(bytes, (byte) 71);
    var calls = new int[1];
    var random =
        new SecureRandom() {
          @Override
          public void nextBytes(byte[] target) {
            calls[0]++;
            assertEquals(32, target.length);
            System.arraycopy(bytes, 0, target, 0, 32);
          }
        };
    var expectedVerifier = MessageDigest.getInstance("SHA-256").digest(bytes);
    ApiCredentialCommit commit =
        (owner, requestedId, intent, issue) -> {
          assertEquals("owner", owner);
          assertEquals(id, requestedId);
          assertEquals("Automation", intent.name());
          assertEquals(List.of("projects:read"), intent.scopes());
          assertEquals(30, intent.expiresInDays());
          var issued = issue.get();
          assertArrayEquals(expectedVerifier, issued.verifier());
          assertFalse(issued.toString().contains(issued.secret()));
          return new ApiCredentialCreation(issued.credential(), issued.secret());
        };
    CreateApiCredentialUseCase useCase =
        new CreateApiCredential(commit, Clock.fixed(now, ZoneOffset.UTC), random);
    var result = useCase.create("owner", id, "Automation", List.of("projects:read"), 30);
    assertEquals(id, result.credential().id());
    assertEquals("Automation", result.credential().name());
    assertEquals(List.of("projects:read"), result.credential().scopes());
    assertEquals(Instant.parse("2026-09-08T12:34:56.123456Z"), result.credential().createdAt());
    assertEquals(Instant.parse("2026-10-08T12:34:56.123456Z"), result.credential().expiresAt());
    assertNull(result.credential().revokedAt());
    assertEquals(
        "owp_" + id + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),
        result.secret());
    assertEquals(1, calls[0]);
  }
}
