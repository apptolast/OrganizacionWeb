package com.apptolast.organization.application;

import java.time.Instant;

/**
 * Lo único que la API cuenta sobre la conexión de GitLab: ocho campos y ni uno más, sin hueco para
 * el token. Sin conexión no hay 404 sino una vista {@code not_connected} con el resto en nulo, para
 * que la interfaz tenga una sola forma de respuesta que leer.
 */
public record GitlabConnectionView(
    String status,
    String apiBase,
    String projectPath,
    Long projectId,
    String tokenHint,
    Instant lastActivityAt,
    ConnectorError lastError,
    Long version) {
  public static final String NOT_CONNECTED = "not_connected";

  public static GitlabConnectionView notConnected() {
    return new GitlabConnectionView(NOT_CONNECTED, null, null, null, null, null, null, null);
  }

  public static GitlabConnectionView of(
      GitlabConnection connection, String apiBase, Instant lastActivityAt) {
    return new GitlabConnectionView(
        connection.status(),
        apiBase,
        connection.projectPath(),
        connection.projectId(),
        connection.tokenHint(),
        lastActivityAt,
        connection.lastError(),
        connection.version());
  }
}
