package com.apptolast.organization.domain;

import java.util.HashSet;
import java.util.List;

public record CustomizationView(CustomizationScope scope, List<String> visibleFields) {
  public CustomizationView {
    var allowed =
        scope == CustomizationScope.PROJECT
            ? List.of("createdAt", "updatedAt")
            : List.of("completionCriterion", "estimatedMinutes", "createdAt", "updatedAt");
    if (!allowed.containsAll(visibleFields))
      throw new ValidationException(
          List.of(
              new FieldError(
                  "visibleFields",
                  "INVALID_VALUE",
                  "Selecciona campos disponibles para esta vista.")));
    if (new HashSet<>(visibleFields).size() != visibleFields.size())
      throw new ValidationException(
          List.of(
              new FieldError(
                  "visibleFields", "INVALID_VALUE", "Selecciona cada campo una sola vez.")));
    visibleFields = List.copyOf(visibleFields);
  }
}
