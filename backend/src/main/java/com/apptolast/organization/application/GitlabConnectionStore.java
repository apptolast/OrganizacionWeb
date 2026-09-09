package com.apptolast.organization.application;

import java.util.Optional;

/** Puerto de salida de la conexión de GitLab: como mucho una por propietario. */
public interface GitlabConnectionStore {
  Optional<GitlabConnection> find(String ownerId);

  void save(String ownerId, GitlabConnection connection);
}
