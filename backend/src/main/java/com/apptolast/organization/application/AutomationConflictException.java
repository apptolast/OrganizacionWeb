package com.apptolast.organization.application;

public final class AutomationConflictException extends RuntimeException {
  public AutomationConflictException() {
    super("Otra petición cambió esta regla.");
  }
}
