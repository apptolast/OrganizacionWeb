package com.apptolast.organization.domain;

import java.util.List;

public final class ApiCredentialInvalidException extends RuntimeException {
  private final List<FieldError> errors;

  public ApiCredentialInvalidException(String field) {
    super("Revisa los campos indicados.");
    errors = List.of(new FieldError(field, "INVALID_VALUE", "Indica un valor válido."));
  }

  public List<FieldError> errors() {
    return errors;
  }
}
