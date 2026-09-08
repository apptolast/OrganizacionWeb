package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PersonalAccessTokenTest {
  @Test
  void s6_acceptsPrintableAsciiUpToTwoHundredFiftyFiveAndHidesValueFromToString() {
    var token = new PersonalAccessToken("!" + "a".repeat(253) + "~");
    assertEquals(255, token.value().length());
    assertFalse(token.toString().contains("aaa"));
  }

  @ParameterizedTest
  @CsvSource({
    ",REQUIRED",
    "'',REQUIRED",
    "'" + "a" + "',OK",
    "ghp_ab cd,INVALID_FORMAT",
    "ghp_café,INVALID_FORMAT",
    "ghp_a\tb,INVALID_FORMAT"
  })
  void s6_rejectsAbsentEmptyOrNonPrintableAsciiTokens(String raw, String code) {
    if (code.equals("OK")) {
      assertEquals(raw, new PersonalAccessToken(raw).value());
      return;
    }
    var error = assertThrows(ValidationException.class, () -> new PersonalAccessToken(raw));
    assertEquals(1, error.errors().size());
    assertEquals("token", error.errors().getFirst().field());
    assertEquals(code, error.errors().getFirst().code());
  }

  @Test
  void s6_rejectsTwoHundredFiftySixCharacters() {
    var error =
        assertThrows(ValidationException.class, () -> new PersonalAccessToken("a".repeat(256)));
    assertEquals("TOO_LONG", error.errors().getFirst().code());
  }
}
