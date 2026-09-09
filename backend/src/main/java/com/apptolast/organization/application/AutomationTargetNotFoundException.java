package com.apptolast.organization.application;

/** A referenced project is not an existing project of this owner; the field names the reference. */
public final class AutomationTargetNotFoundException extends RuntimeException {
  private final String field;

  public AutomationTargetNotFoundException(String field) {
    super("El proyecto indicado no existe.");
    this.field = field;
  }

  public String field() {
    return field;
  }
}
