package com.apptolast.organization.domain;

import java.util.List;

/** PAT en claro sólo en memoria: nunca aparece en toString, logs ni respuestas. */
public final class PersonalAccessToken {
  private static final int MAX_LENGTH = 255;
  private final String value;

  public PersonalAccessToken(String raw) {
    if (raw == null || raw.isEmpty()) throw invalid("REQUIRED", "Pega el token de acceso.");
    if (raw.length() > MAX_LENGTH)
      throw invalid("TOO_LONG", "El token admite hasta 255 caracteres.");
    if (!raw.chars().allMatch(c -> c > 0x20 && c < 0x7f))
      throw invalid("INVALID_FORMAT", "El token sólo admite caracteres ASCII sin espacios.");
    this.value = raw;
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return "PersonalAccessToken[redacted]";
  }

  private static ValidationException invalid(String code, String message) {
    return new ValidationException(List.of(new FieldError("token", code, message)));
  }
}
