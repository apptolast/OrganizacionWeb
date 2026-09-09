package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CalendarFeedSecretTest {
  @Test
  void s1_issuesFortyThreeBase64UrlCharactersFromThirtyTwoRandomOctets() {
    var secret = CalendarFeedSecret.issue(new SecureRandom());
    assertEquals(43, secret.token().length());
    assertTrue(secret.token().matches("[A-Za-z0-9_-]{43}"), secret.token());
  }

  @Test
  void s1_fingerprintIsTheThirtyTwoOctetSha256OfTheFortyThreeCharacters() throws Exception {
    var secret = CalendarFeedSecret.issue(new SecureRandom());
    var expected =
        MessageDigest.getInstance("SHA-256")
            .digest(secret.token().getBytes(StandardCharsets.US_ASCII));
    assertEquals(32, secret.fingerprint().length);
    assertArrayEquals(expected, secret.fingerprint());
    assertArrayEquals(expected, CalendarFeedSecret.fingerprintOf(secret.token()));
  }

  @Test
  void s1_consumesThirtyTwoOctetsOfRandomnessAndNeverRepeatsATokenAcrossIssues() {
    var consumed = new int[1];
    var random =
        new SecureRandom() {
          @Override
          public void nextBytes(byte[] bytes) {
            consumed[0] = bytes.length;
            super.nextBytes(bytes);
          }
        };
    var tokens = new HashSet<String>();
    for (int issue = 0; issue < 200; issue++) tokens.add(CalendarFeedSecret.issue(random).token());
    assertEquals(32, consumed[0]);
    assertEquals(200, tokens.size());
  }

  /**
   * @s15: any candidate resolves through the full digest, never through a shape check.
   */
  @ParameterizedTest
  @ValueSource(strings = {"", "corto", "otra/ruta", "no-base64url:+/=", "ñ"})
  void s15_fingerprintAcceptsAnyCandidateAndYieldsThirtyTwoOctets(String candidate) {
    assertEquals(32, CalendarFeedSecret.fingerprintOf(candidate).length);
  }

  @Test
  void s15_candidatesThatDifferOnlyInTheLastCharacterHaveUnrelatedFingerprints() {
    var secret = CalendarFeedSecret.issue(new SecureRandom());
    var truncated = secret.token().substring(0, 42);
    assertFalse(
        java.util.Arrays.equals(secret.fingerprint(), CalendarFeedSecret.fingerprintOf(truncated)));
    assertFalse(
        java.util.Arrays.equals(
            secret.fingerprint(), CalendarFeedSecret.fingerprintOf(secret.token() + "A")));
  }
}
