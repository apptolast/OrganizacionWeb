package com.apptolast.organization.application;

import java.util.Optional;

/** Puerto de salida de la conexión de GitLab: como mucho una por propietario. */
public interface GitlabConnectionStore {
  Optional<GitlabConnection> find(String ownerId);

  void save(String ownerId, GitlabConnection connection);

  /** {@code true} si la fila existía; borrar dos veces seguidas no es un error. */
  boolean delete(String ownerId);
}
