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
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidIntentions")
  void s2_rejectsInvalidIntentBeforeCallingCommit(
      String name, List<String> scopes, int days, String field) {
    ApiCredentialCommit commit =
        (owner, id, intent, issue) -> {
          throw new AssertionError("Invalid input reached storage");
        };
    var useCase = new CreateApiCredential(commit, Clock.systemUTC(), new SecureRandom());
    var error =
        assertThrows(
            com.apptolast.organization.domain.ApiCredentialInvalidException.class,
            () -> useCase.create("owner", UUID.randomUUID(), name, scopes, days));
    assertEquals(field, error.errors().getFirst().field());
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidIntentions() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "\u2003 ", List.of("projects:read"), 30, "name"),
        org.junit.jupiter.params.provider.Arguments.of(
            "x".repeat(81), List.of("projects:read"), 30, "name"),
        org.junit.jupiter.params.provider.Arguments.of(
            "private\nvalue", List.of("projects:read"), 30, "name"),
        org.junit.jupiter.params.provider.Arguments.of("name", List.of(), 30, "scopes"),
        org.junit.jupiter.params.provider.Arguments.of("name", List.of("unknown"), 30, "scopes"),
        org.junit.jupiter.params.provider.Arguments.of(
            "name", List.of("projects:read", "projects:read"), 30, "scopes"),
        org.junit.jupiter.params.provider.Arguments.of(
            "name", List.of("projects:read"), 31, "expiresInDays"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"0000-12-31T23:59:59Z", "9999-12-31T23:59:59Z", "+10000-01-01T00:00:00Z"})
  void s6_unrepresentableClockNeverIssuesSecret(String instant) {
    ApiCredentialCommit commit =
        (owner, id, intent, issue) -> {
          var ignored = issue.get();
          throw new AssertionError("Invalid time issued a credential");
        };
    var random =
        new SecureRandom() {
          @Override
          public void nextBytes(byte[] bytes) {
            throw new AssertionError("Invalid time generated secret");
          }
        };
    var useCase =
        new CreateApiCredential(
            commit, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), random);
    assertThrows(
        StorageUnavailableException.class,
        () -> useCase.create("owner", UUID.randomUUID(), "name", List.of("projects:read"), 30));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "7,2026-09-15T12:34:56.123456Z",
    "30,2026-10-08T12:34:56.123456Z",
    "90,2026-12-07T12:34:56.123456Z"
  })
  void s5_eachTermUsesOneUtcClockCapture(int days, String expiry) {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var clock =
        new Clock() {
          @Override
          public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            assertEquals(1, calls.incrementAndGet());
            return Instant.parse("2026-09-08T12:34:56.123456789Z");
          }
        };
    ApiCredentialCommit commit =
        (owner, id, intent, issue) -> {
          var value = issue.get();
          return new ApiCredentialCreation(value.credential(), value.secret());
        };
    var result =
        new CreateApiCredential(commit, clock, new SecureRandom())
            .create("owner", UUID.randomUUID(), "Term", List.of("tasks:read"), days);
    assertEquals(Instant.parse("2026-09-08T12:34:56.123456Z"), result.credential().createdAt());
    assertEquals(Instant.parse(expiry), result.credential().expiresAt());
    assertEquals(1, calls.get());
  }

  @Test
  void s2_unpairedSurrogateCannotBeSilentlyChangedByPersistenceEncoding() {
    ApiCredentialCommit commit =
        (owner, id, intent, issue) -> {
          throw new AssertionError("Invalid Unicode reached JDBC");
        };
    var create = new CreateApiCredential(commit, Clock.systemUTC(), new SecureRandom());
    var invalid = "name" + Character.toString((char) 0xd800);
    var error =
        assertThrows(
            com.apptolast.organization.domain.ApiCredentialInvalidException.class,
            () -> create.create("owner", UUID.randomUUID(), invalid, List.of("tasks:read"), 7));
    assertEquals("name", error.errors().getFirst().field());
  }

  @Test
  void s1_metadataAndVerifierRetainOwnershipAcrossPort() {
    var scopes = new java.util.ArrayList<>(List.of("projects:read"));
    var verifier = new byte[32];
    verifier[0] = 42;
    var now = Instant.parse("2026-09-08T12:00:00Z");
    var metadata =
        new com.apptolast.organization.domain.ApiCredential(
            UUID.randomUUID(), "Owned", scopes, now, now.plusSeconds(86400), null);
    var issued = new ApiCredentialIssuance(metadata, "test-only", verifier);
    scopes.clear();
    verifier[0] = 0;
    var borrowed = issued.verifier();
    borrowed[0] = 1;
    assertEquals(List.of("projects:read"), issued.credential().scopes());
    assertEquals(42, issued.verifier()[0]);
  }

  @Test
  void s3_normalizesUnicodeAndOwnsScopesInCanonicalOrder() {
    var scopes = new java.util.ArrayList<>(List.of("history:read", "projects:read", "tasks:write"));
    var raw = "\u0085\u00a0\u2003" + "\ud83d\ude00".repeat(80) + "\u2003\u00a0\u0085";
    ApiCredentialCommit commit =
        (owner, id, intent, issue) -> {
          scopes.clear();
          assertEquals(List.of("projects:read", "tasks:write", "history:read"), intent.scopes());
          assertEquals("\ud83d\ude00".repeat(80), intent.name());
          var issued = issue.get();
          assertThrows(
              UnsupportedOperationException.class, () -> issued.credential().scopes().clear());
          return new ApiCredentialCreation(issued.credential(), issued.secret());
        };
    var result =
        new CreateApiCredential(commit, Clock.systemUTC(), new SecureRandom())
            .create("owner", UUID.randomUUID(), raw, scopes, 7);
    assertEquals(
        List.of("projects:read", "tasks:write", "history:read"), result.credential().scopes());
  }

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
