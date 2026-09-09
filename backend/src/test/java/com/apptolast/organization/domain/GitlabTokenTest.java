package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s9 @s10 @s13 el PAT de GitLab: mismo tipo que el de 27 con el límite propio de este proveedor, y
 *     la pista de cuatro caracteres que es lo único del token que sale del servidor.
 */
class GitlabTokenTest {
  private static final int GITLAB_LIMIT = 200;

  @Test
  void s10_acceptsTwoHundredCharactersAndRejectsTwoHundredAndOne() {
    assertEquals(200, token("a".repeat(200)).value().length());
    assertEquals("TOO_LONG", codeOf("a".repeat(201)));
  }

  @ParameterizedTest
  @CsvSource({",REQUIRED", "'',REQUIRED", "'glpat-ab cd',INVALID_FORMAT"})
  void s10_rejectsAbsentEmptyOrSpacedTokens(String raw, String code) {
    assertEquals(code, codeOf(raw));
  }

  @Test
  void s9_theHintIsTheLastFourCharactersAndNothingElseLeavesTheToken() {
    assertEquals("WXYZ", token("glpat-xxxxxxxxxxxxxxxxWXYZ").hint());
    assertEquals("9Q2p", token("glpat-otro-secreto-largo9Q2p").hint());
    assertFalse(token("glpat-xxxxxxxxxxxxxxxxWXYZ").toString().contains("xxxx"));
  }

  private static PersonalAccessToken token(String raw) {
    return PersonalAccessToken.upTo(GITLAB_LIMIT, raw);
  }

  private static String codeOf(String raw) {
    var error =
        assertThrows(ValidationException.class, () -> token(raw)).errors().getFirst();
    assertEquals("token", error.field());
    return error.code();
  }
}
