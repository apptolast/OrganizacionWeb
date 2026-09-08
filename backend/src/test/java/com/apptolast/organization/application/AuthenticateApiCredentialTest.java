package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AuthenticateApiCredentialTest {
  @ParameterizedTest
  @MethodSource("malformedTokens")
  void s19_invalidTokenIsRejectedBeforeStorage(String token) {
    ApiCredentialAuthentication credentials =
        (id, verifier, now) -> {
          throw new AssertionError("Malformed token reached storage");
        };
    var useCase = new AuthenticateApiCredential(credentials, Clock.systemUTC(), owner -> true);
    assertThrows(ApiUnauthenticatedException.class, () -> useCase.authenticate(token));
  }

  static Stream<String> malformedTokens() {
    var valid =
        "owp_00000000-0000-0000-0000-00000000000a."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
    return Stream.of(
        null,
        "",
        valid + "=",
        valid.replace("owp_", "bad_"),
        valid.replace("000a.", "000A."),
        valid.substring(0, 83) + "B",
        valid.replace("00000000-0000-0000-0000-00000000000a", "1-1-1-1-1"),
        valid.substring(0, 40) + "," + valid.substring(41));
  }
}
