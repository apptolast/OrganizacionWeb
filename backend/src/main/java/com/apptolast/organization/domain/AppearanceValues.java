package com.apptolast.organization.domain;

import java.util.List;
import java.util.Set;

public record AppearanceValues(String theme, String accentLight, String accentDark) {
  public AppearanceValues {
    theme = theme(theme);
    accentLight = accentLight(accentLight);
    accentDark = accentDark(accentDark);
  }

  public static String theme(String value) {
    if (!Set.of("LIGHT", "DARK", "SYSTEM").contains(value))
      throw new ValidationException(
          List.of(new FieldError("theme", "INVALID_VALUE", "Selecciona un tema válido.")));
    return value;
  }

  public static String accentLight(String value) {
    return color(
        value,
        "accentLight",
        List.of("#F8F9F5", "#FFFFFF", "#FDFEFB", "#EEF1E9", "#DFE8D9", "#D0DFC9"));
  }

  public static String accentDark(String value) {
    return color(
        value,
        "accentDark",
        List.of("#111827", "#1F2937", "#182232", "#0B1220", "#28394A", "#33485C"));
  }

  private static String color(String value, String field, List<String> surfaces) {
    if (!value.matches("#[0-9a-fA-F]{6}"))
      throw new ValidationException(
          List.of(
              new FieldError(field, "INVALID_VALUE", "Introduce un color hexadecimal completo.")));
    for (var surface : surfaces) {
      if (contrast(value, surface) < 4.5)
        throw new ValidationException(
            List.of(
                new FieldError(
                    field,
                    "INSUFFICIENT_CONTRAST",
                    "Selecciona un color con suficiente contraste.")));
    }
    return value.toUpperCase(java.util.Locale.ROOT);
  }

  private static double contrast(String first, String second) {
    double a = luminance(first), b = luminance(second);
    return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
  }

  private static double luminance(String color) {
    return 0.2126 * channel(color, 1) + 0.7152 * channel(color, 3) + 0.0722 * channel(color, 5);
  }

  private static double channel(String color, int offset) {
    double value = Integer.parseInt(color.substring(offset, offset + 2), 16) / 255.0;
    return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
  }
}
