package com.apptolast.organization.application;

public final class ApiUnauthenticatedException extends RuntimeException {
  public ApiUnauthenticatedException() {
    super("Credencial no válida.");
  }
}
