package com.apptolast.organization.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomFieldInput(UUID fieldId, Object value) {
  public Object canonical(CustomFieldType type, int index) {
    if (value == null) return null;
    boolean compatible =
        switch (type) {
          case TEXT, DATE -> value instanceof String;
          case NUMBER -> value instanceof BigDecimal;
          case BOOLEAN -> value instanceof Boolean;
        };
    if (!compatible)
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  "values[" + index + "].value",
                  "INVALID_TYPE",
                  "El valor debe corresponder al tipo de campo.")));
    if (type == CustomFieldType.BOOLEAN) return (Boolean) value;
    if (type == CustomFieldType.TEXT) {
      var text = (String) value;
      if (text.codePointCount(0, text.length()) > 1000
          || text.codePoints().anyMatch(point -> point == 0 || point >= 0xd800 && point <= 0xdfff))
        throw new ValidationException(
            java.util.List.of(
                new FieldError(
                    "values[" + index + "].value",
                    "INVALID_VALUE",
                    "Escribe hasta 1000 caracteres válidos.")));
      return text.isEmpty() ? null : text;
    }
    if (type == CustomFieldType.DATE) {
      var date = (String) value;
      try {
        if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
            || java.time.LocalDate.parse(date).getYear() < 1)
          throw new java.time.DateTimeException("Invalid date");
        return date;
      } catch (java.time.DateTimeException error) {
        throw new ValidationException(
            java.util.List.of(
                new FieldError(
                    "values[" + index + "].value",
                    "INVALID_VALUE",
                    "Escribe una fecha válida entre 0001 y 9999.")));
      }
    }
    try {
      int number = ((BigDecimal) value).intValueExact();
      if (number < -1000000000 || number > 1000000000) throw new ArithmeticException();
      return number;
    } catch (ArithmeticException error) {
      throw new ValidationException(
          java.util.List.of(
              new FieldError(
                  "values[" + index + "].value",
                  "INVALID_VALUE",
                  "Escribe un entero entre -1000000000 y 1000000000.")));
    }
  }
}
