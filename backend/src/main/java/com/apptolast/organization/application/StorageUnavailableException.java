package com.apptolast.organization.application;

public final class StorageUnavailableException extends RuntimeException {
  public StorageUnavailableException(Throwable cause) {
    super("El almacenamiento no está disponible.", cause);
  }
}
