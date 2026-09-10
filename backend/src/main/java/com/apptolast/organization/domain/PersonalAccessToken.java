package com.apptolast.organization.domain;

import java.util.List;

/**
 * PAT en claro sólo en memoria: nunca aparece en toString, logs ni respuestas. Cada gestor de
 * issues fija su propio máximo, que es lo único que cambia entre proveedores; el alfabeto —ASCII
 * imprimible sin espacios— es común, porque es el que cabe en una cabecera HTTP.
 */
public final class PersonalAccessToken {
  private static final int MAX_LENGTH = 255;
  private static final int HINT_LENGTH = 4;

  /**
   * La pista es lo único del token que sale del servidor —se guarda en claro en {@code token_hint}
   * y viaja en el DTO—, así que un token que quepa entero en su pista se publicaría a sí mismo. El
   * mínimo es, por tanto, un carácter más de los que enseña la pista.
   */
  private static final int MIN_LENGTH = HINT_LENGTH + 1;

  private final String value;

  public PersonalAccessToken(String raw) {
    this(MAX_LENGTH, raw);
  }

  private PersonalAccessToken(int maxLength, String raw) {
    if (raw == null || raw.isEmpty()) throw invalid("REQUIRED", "Pega el token de acceso.");
    if (raw.length() < MIN_LENGTH)
      throw invalid("TOO_SHORT", "El token necesita al menos " + MIN_LENGTH + " caracteres.");
    if (raw.length() > maxLength)
      throw invalid("TOO_LONG", "El token admite hasta " + maxLength + " caracteres.");
    if (!raw.chars().allMatch(c -> c > 0x20 && c < 0x7f))
      throw invalid("INVALID_FORMAT", "El token sólo admite caracteres ASCII sin espacios.");
    this.value = raw;
  }

  public static PersonalAccessToken upTo(int maxLength, String raw) {
    return new PersonalAccessToken(maxLength, raw);
  }

  public String value() {
    return value;
  }

  /**
   * Los últimos cuatro caracteres: lo único del token que la interfaz puede mostrar. El mínimo de
   * longitud garantiza que siempre sea un sufijo estricto, nunca el token entero.
   */
  public String hint() {
    return value.substring(value.length() - HINT_LENGTH);
  }

  @Override
  public String toString() {
    return "PersonalAccessToken[redacted]";
  }

  private static ValidationException invalid(String code, String message) {
    return new ValidationException(List.of(new FieldError("token", code, message)));
  }
}
