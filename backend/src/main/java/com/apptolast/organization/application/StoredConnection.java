package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Arrays;

/**
 * Fila de conexión tal y como vive en el almacén: el token sólo existe aquí como texto cifrado.
 * {@code toString} no expone el texto cifrado para que ningún volcado accidental lo publique.
 */
public record StoredConnection(
    String repository, String login, String status, byte[] tokenCiphertext, Instant connectedAt) {
  public static final String VALID = "valid";
  public static final String INVALID = "invalid";

  public boolean isValid() {
    return VALID.equals(status);
  }

  public StoredConnection asInvalid() {
    return new StoredConnection(repository, login, INVALID, tokenCiphertext, connectedAt);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof StoredConnection row
        && repository.equals(row.repository)
        && login.equals(row.login)
        && status.equals(row.status)
        && connectedAt.equals(row.connectedAt)
        && Arrays.equals(tokenCiphertext, row.tokenCiphertext);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(repository, login, status, connectedAt)
        + Arrays.hashCode(tokenCiphertext);
  }

  @Override
  public String toString() {
    return "StoredConnection[repository=" + repository + ", status=" + status + ", token=redacted]";
  }
}
