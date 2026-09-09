package com.apptolast.organization.domain;

import java.util.List;

public final class WebhookInvalidException extends RuntimeException {
  private final List<FieldError> errors;

  public WebhookInvalidException(List<String> fields) {
    super("Revisa los campos indicados.");
    errors =
        fields.stream()
            .map(field -> new FieldError(field, "INVALID_VALUE", "Indica un valor válido."))
            .toList();
  }

  public List<FieldError> errors() {
    return errors;
  }
}
