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
  private final String value;

  public PersonalAccessToken(String raw) {
    this(MAX_LENGTH, raw);
  }

  private PersonalAccessToken(int maxLength, String raw) {
    if (raw == null || raw.isEmpty()) throw invalid("REQUIRED", "Pega el token de acceso.");
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

  /** Los últimos cuatro caracteres: lo único del token que la interfaz puede mostrar. */
  public String hint() {
    return value.length() <= HINT_LENGTH ? value : value.substring(value.length() - HINT_LENGTH);
  }

  @Override
  public String toString() {
    return "PersonalAccessToken[redacted]";
  }

  private static ValidationException invalid(String code, String message) {
    return new ValidationException(List.of(new FieldError("token", code, message)));
  }
}
