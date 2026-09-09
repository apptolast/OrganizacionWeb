package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s10 la ruta del proyecto se valida antes de componer ninguna URL saliente, así que ni una sola
 *     de las formas rechazadas llega a abrir una conexión.
 */
class GitlabProjectPathTest {
  @ParameterizedTest
  @ValueSource(strings = {"grupo/proyecto", "grupo/sub/proyecto", "a/b/c/d/e", "gr_u.p+o-1/p.r-o"})
  void s10_acceptsTwoToFiveSegmentsOfTheAllowedAlphabet(String raw) {
    assertEquals(raw, GitlabProjectPath.parse(raw).value());
  }

  @ParameterizedTest
  @CsvSource({
    "soloproyecto, INVALID_FORMAT",
    "grupo/../otro, INVALID_FORMAT",
    "a/b/c/d/e/f, INVALID_FORMAT",
    "'grupo/mi proyecto', INVALID_FORMAT",
    "'grupo/', INVALID_FORMAT",
    "'/proyecto', INVALID_FORMAT",
    "'grupo//proyecto', INVALID_FORMAT",
    "'https://gitlab.com/grupo/proyecto', INVALID_FORMAT"
  })
  void s10_rejectsWhatCannotBeAProjectPath(String raw, String code) {
    assertEquals(code, codeOf(raw));
  }

  @Test
  void s10_acceptsTwoHundredAndFiftyFiveCharactersAndRejectsTwoHundredAndFiftySix() {
    assertEquals("g/" + "a".repeat(253), GitlabProjectPath.parse("g/" + "a".repeat(253)).value());
    assertEquals("TOO_LONG", codeOf("g/" + "a".repeat(254)));
  }

  @Test
  void s10_missingPathIsRequiredRatherThanMalformed() {
    assertEquals("REQUIRED", codeOf(null));
    assertEquals("REQUIRED", codeOf(""));
  }

  private static String codeOf(String raw) {
    var error =
        assertThrows(ValidationException.class, () -> GitlabProjectPath.parse(raw))
            .errors()
            .getFirst();
    assertEquals("projectPath", error.field());
    return error.code();
  }
}
