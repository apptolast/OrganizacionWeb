package com.apptolast.organization.application;

public final class ExportTooLargeException extends RuntimeException {
  public ExportTooLargeException() {
    super("La exportación supera el límite permitido.");
  }
}
