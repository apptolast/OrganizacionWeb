package com.apptolast.organization.application;

public final class AutomationLimitException extends RuntimeException {
  public AutomationLimitException() {
    super("Ya hay veinte reglas.");
  }
}
