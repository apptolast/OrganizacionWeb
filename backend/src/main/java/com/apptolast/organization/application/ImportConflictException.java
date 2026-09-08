package com.apptolast.organization.application;

public final class ImportConflictException extends RuntimeException {
  public ImportConflictException() {
    super("El archivo no se puede incorporar sin cambiar datos existentes.");
  }
}
