package com.apptolast.organization.application;

public final class ImportKeyReusedException extends RuntimeException {
  public ImportKeyReusedException() {
    super("La clave ya corresponde a otro archivo.");
  }
}
