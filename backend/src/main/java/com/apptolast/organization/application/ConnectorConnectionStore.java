package com.apptolast.organization.application;

import java.util.Optional;

/** Puerto de salida de la conexión del conector: como mucho una por propietario. */
public interface ConnectorConnectionStore {
  Optional<StoredConnection> find(String ownerId);

  void save(String ownerId, StoredConnection connection);

  /** {@code true} si la fila existía; borrar dos veces seguidas no es un error. */
  boolean delete(String ownerId);

  void invalidate(String ownerId);
}
