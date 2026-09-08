package com.apptolast.organization.domain;

import java.util.List;

public final class AutomationTemplateException extends RuntimeException {
  private final List<FieldError> errors;

  public AutomationTemplateException(List<FieldError> errors) {
    super("Revisa las plantillas indicadas.");
    this.errors = List.copyOf(errors);
  }

  public List<FieldError> errors() {
    return errors;
  }
}
