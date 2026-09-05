package com.apptolast.organization.domain;

import java.util.List;

public final class ValidationException extends RuntimeException {
  private final List<FieldError> errors;

  public ValidationException(List<FieldError> errors) {
    super("Revisa los campos indicados.");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
