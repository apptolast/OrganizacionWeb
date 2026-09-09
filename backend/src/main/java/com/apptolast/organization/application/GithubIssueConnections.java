package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Optional;

/**
 * La conexión de GitHub vista por el caso de uso compartido de importación. GitHub nombra su
 * proyecto igual al pedir issues que al enseñarlo, así que la ruta y la referencia coinciden.
 */
public final class GithubIssueConnections implements IssueConnections {
  public static final String SOURCE = "github";

  private final ConnectorConnectionStore connections;

  public GithubIssueConnections(ConnectorConnectionStore connections) {
    this.connections = connections;
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public Optional<IssueConnection> find(String ownerId) {
    return connections
        .find(ownerId)
        .map(
            row ->
                new IssueConnection(
                    row.repository(), row.repository(), row.tokenCiphertext(), row.isValid()));
  }

  @Override
  public void invalidate(String ownerId, String errorCode, Instant at) {
    connections.invalidate(ownerId);
  }

  /**
   * La conexión de 27 no guarda el último fallo: su pantalla lo lee del recibo de la importación,
   * que es donde 27 decidió que viviera.
   */
  @Override
  public void recordFailure(String ownerId, String errorCode, Instant at) {
    // Sin sitio donde anotarlo, y sin inventarle uno a una feature ya cerrada.
  }
}
