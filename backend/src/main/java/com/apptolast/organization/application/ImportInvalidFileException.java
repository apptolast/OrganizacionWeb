package com.apptolast.organization.application;

public final class ImportInvalidFileException extends RuntimeException {
  public ImportInvalidFileException() {
    super("El archivo de importación no es válido.");
  }
}
