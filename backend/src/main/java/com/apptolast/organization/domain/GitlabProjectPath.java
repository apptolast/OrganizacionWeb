package com.apptolast.organization.domain;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Ruta de un proyecto de GitLab en forma {@code grupo/proyecto}, con hasta tres subgrupos
 * intermedios. Se valida antes de componer ninguna URL saliente: el alfabeto cerrado y la
 * prohibición de los segmentos {@code .} y {@code ..} son lo que impide que una ruta escrita por
 * una persona salte fuera de {@code /projects/} al codificarla.
 */
public record GitlabProjectPath(String value) {
  private static final int MAX_LENGTH = 255;
  private static final int MIN_SEGMENTS = 2;
  private static final int MAX_SEGMENTS = 5;
  private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9_.+-]+");

  public static GitlabProjectPath parse(String raw) {
    if (raw == null || raw.isEmpty()) throw invalid("REQUIRED", "Indica la ruta del proyecto.");
    if (raw.codePointCount(0, raw.length()) > MAX_LENGTH)
      throw invalid("TOO_LONG", "La ruta admite hasta 255 caracteres.");
    var segments = raw.split("/", -1);
    if (segments.length < MIN_SEGMENTS || segments.length > MAX_SEGMENTS) throw malformed();
    for (var segment : segments) if (!isUsable(segment)) throw malformed();
    return new GitlabProjectPath(raw);
  }

  /** Un segmento hecho sólo de puntos nombra el directorio actual o el de arriba: no es proyecto. */
  private static boolean isUsable(String segment) {
    return SEGMENT.matcher(segment).matches() && !segment.chars().allMatch(c -> c == '.');
  }

  private static ValidationException malformed() {
    return invalid("INVALID_FORMAT", "Escribe la ruta como grupo/proyecto.");
  }

  private static ValidationException invalid(String code, String message) {
    return new ValidationException(List.of(new FieldError("projectPath", code, message)));
  }
}
