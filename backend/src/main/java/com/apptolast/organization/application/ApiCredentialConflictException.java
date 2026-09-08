package com.apptolast.organization.application;

public final class ApiCredentialConflictException extends RuntimeException {
  public ApiCredentialConflictException() {
    super("La identidad del intento ya está ocupada.");
  }
}
