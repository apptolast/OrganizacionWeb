package com.apptolast.organization.domain;

import java.util.List;

public record WorkSessionCloseNotes(String progressNote, String nextStep) {
  public WorkSessionCloseNotes {
    progressNote = normalize("progressNote", progressNote);
    nextStep = normalize("nextStep", nextStep);
  }

  private static String normalize(String field, String value) {
    if (value == null) return "";
    if (value.codePointCount(0, value.length()) > 2000
        || value.codePoints().anyMatch(point -> point == 0 || (point >= 0xD800 && point <= 0xDFFF)))
      throw new ValidationException(
          List.of(
              new FieldError(
                  field, "INVALID_VALUE", "El texto admite hasta 2000 puntos de código válidos.")));
    return value;
  }
}
