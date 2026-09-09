package com.apptolast.organization.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Repositorio de GitHub en forma {@code owner/repo}, recortado y validado antes de salir a la red.
 */
public record GithubRepository(String fullName) {
  private static final Pattern FULL_NAME =
      Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?/[A-Za-z0-9._-]{1,100}");

  public GithubRepository {
    if (fullName == null || !FULL_NAME.matcher(fullName).matches())
      throw invalid("INVALID_FORMAT", "Escribe el repositorio como propietario/nombre.");
  }

  public static GithubRepository parse(String raw) {
    if (raw == null) throw invalid("REQUIRED", "Indica el repositorio.");
    return new GithubRepository(raw.replaceAll("(?U)^\\s+|\\s+$", ""));
  }

  private static ValidationException invalid(String code, String message) {
    return new ValidationException(List.of(new FieldError("repository", code, message)));
  }
}
