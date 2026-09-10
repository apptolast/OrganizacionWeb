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
    "ghp_ab cd,INVALID_FORMAT",
    "ghp_café,INVALID_FORMAT",
    "ghp_a\tb,INVALID_FORMAT"
  })
  void s6_rejectsAbsentEmptyOrNonPrintableAsciiTokens(String raw, String code) {
    var error = assertThrows(ValidationException.class, () -> new PersonalAccessToken(raw));
    assertEquals(1, error.errors().size());
    assertEquals("token", error.errors().getFirst().field());
    assertEquals(code, error.errors().getFirst().code());
  }

  /**
   * La pista son los últimos cuatro caracteres y es lo único del token que sale del servidor: se
   * guarda en claro en {@code token_hint} y viaja en el DTO. Con cuatro caracteres o menos esa
   * pista ES el token entero, así que el secreto acabaría en claro en la base de datos y en una
   * respuesta de API. El dominio no puede admitir un token que no se pueda pistar sin desvelarlo.
   */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"a", "ab", "abc", "abcd"})
  void b_rejectsTokensSoShortThatTheHintWouldBeTheWholeSecret(String raw) {
    var error = assertThrows(ValidationException.class, () -> new PersonalAccessToken(raw));
    assertEquals("TOO_SHORT", error.errors().getFirst().code());
    assertEquals("token", error.errors().getFirst().field());
  }

  @Test
  void b_theHintIsAStrictSuffixOfEveryTokenTheDomainAccepts() {
    var shortest = new PersonalAccessToken("abcde");

    assertEquals("bcde", shortest.hint());
    assertNotEquals(shortest.value(), shortest.hint());
  }

  /**
   * @s6 «el token sólo admite caracteres ASCII sin espacios». El alfabeto es el imprimible, de
   *     {@code !} (0x21) a {@code ~} (0x7e); el carácter que hay justo encima del techo es DEL
   *     (0x7f), que es de control y no cabe en una cabecera HTTP. Sin esta fila, mover el techo de
   *     {@code < 0x7f} a {@code <= 0x7f} no rompía ninguna prueba: superviviente de frontera vivo
   *     en el informe de mutación de la campaña de cierre.
   */
  @Test
  void s6_rejectsTheDeleteControlCharacterJustAboveThePrintableRange() {
    var error = assertThrows(ValidationException.class, () -> new PersonalAccessToken("ghp_ab"));

    assertEquals("INVALID_FORMAT", error.errors().getFirst().code());
    assertEquals("token", error.errors().getFirst().field());
  }

  @Test
  void s6_rejectsTwoHundredFiftySixCharacters() {
    var error =
        assertThrows(ValidationException.class, () -> new PersonalAccessToken("a".repeat(256)));
    assertEquals("TOO_LONG", error.errors().getFirst().code());
  }
}
