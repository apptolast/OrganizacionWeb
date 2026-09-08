package com.apptolast.organization.domain;

import java.util.List;

public record CustomFieldLabel(String value) {
  public CustomFieldLabel {
    value = value.replaceAll("(?U)^\\s+|\\s+$", "");
    if (value.isEmpty()
        || value.codePointCount(0, value.length()) > 60
        || value.codePoints().anyMatch(point -> point == 0 || point >= 0xd800 && point <= 0xdfff))
      throw new ValidationException(
          List.of(
              new FieldError(
                  "label",
                  "INVALID_VALUE",
                  "Escribe una etiqueta válida de entre 1 y 60 caracteres.")));
  }
}
