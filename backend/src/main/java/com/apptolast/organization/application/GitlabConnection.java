package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Fila de conexión de GitLab tal y como vive en el almacén. El token sólo existe aquí como texto
 * cifrado, y {@code toString} no lo expone para que ningún volcado accidental lo publique.
 */
public record GitlabConnection(
    String projectPath,
    long projectId,
    String tokenHint,
    String status,
    byte[] tokenCiphertext,
    Instant lastActivityAt,
    String lastErrorCode,
    Instant lastErrorAt,
    long version) {
  public static final String CONNECTED = "connected";
  public static final String ERROR = "error";

  public boolean isConnected() {
    return CONNECTED.equals(status);
  }

  /** La misma fila, ya inservible: el token cifrado se conserva porque la pantalla lo describe. */
  public GitlabConnection withError(String errorCode, Instant at) {
    return new GitlabConnection(
        projectPath,
        projectId,
        tokenHint,
        ERROR,
        tokenCiphertext,
        lastActivityAt,
        errorCode,
        at,
        version);
  }

  /** El último fallo publicable, o {@code null} si la conexión no arrastra ninguno. */
  public ConnectorError lastError() {
    return lastErrorCode == null ? null : new ConnectorError(lastErrorCode, lastErrorAt);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof GitlabConnection row
        && projectId == row.projectId
        && version == row.version
        && projectPath.equals(row.projectPath)
        && tokenHint.equals(row.tokenHint)
        && status.equals(row.status)
        && Objects.equals(lastActivityAt, row.lastActivityAt)
        && Objects.equals(lastErrorCode, row.lastErrorCode)
        && Objects.equals(lastErrorAt, row.lastErrorAt)
        && Arrays.equals(tokenCiphertext, row.tokenCiphertext);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            projectPath, projectId, tokenHint, status, lastActivityAt, lastErrorCode, lastErrorAt,
            version)
        + Arrays.hashCode(tokenCiphertext);
  }

  @Override
  public String toString() {
    return "GitlabConnection[projectPath="
        + projectPath
        + ", projectId="
        + projectId
        + ", status="
        + status
        + ", token=redacted]";
  }
}
