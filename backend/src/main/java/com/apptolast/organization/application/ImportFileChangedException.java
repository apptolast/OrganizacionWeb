package com.apptolast.organization.application;

public final class ImportFileChangedException extends RuntimeException {
  public ImportFileChangedException() {
    super("El archivo no coincide con la vista previa.");
  }
}
