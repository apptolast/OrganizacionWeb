package com.apptolast.organization.application;

public final class ApiCredentialLimitException extends RuntimeException {
  public ApiCredentialLimitException() {
    super("Ya hay diez credenciales válidas.");
  }
}
