package com.apptolast.organization.application;

public final class ImportTooLargeException extends RuntimeException {
  public ImportTooLargeException() {
    super("El archivo supera el límite de importación.");
  }
}
