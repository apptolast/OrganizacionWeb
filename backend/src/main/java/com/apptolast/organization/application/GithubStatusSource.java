package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.time.Instant;

/**
 * La fila del conector de GitHub (27). Su fila no guarda ni código ni instante de error: lo único
 * que puede estar mal en una conexión de 27 es que el token deje de valer, y eso lo dice su propio
 * estado {@code invalid}. El instante del error es entonces el de la última importación —que es
 * cuando se descubrió— y, sin ninguna, el del alta.
 */
public final class GithubStatusSource implements ConnectorStatusSource {
  public static final String ID = "github";

  private final ConnectorConnectionStore connections;
  private final IssueImportReceiptStore receipts;

  public GithubStatusSource(
      ConnectorConnectionStore connections, IssueImportReceiptStore receipts) {
    this.connections = connections;
    this.receipts = receipts;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return true;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    return connections
        .find(ownerId)
        .map(connection -> row(ownerId, connection))
        .orElseGet(() -> ConnectorRow.notConnected(ID));
  }

  private ConnectorRow row(String ownerId, StoredConnection connection) {
    var activity = lastActivity(ownerId, connection);
    if (connection.isValid()) return ConnectorRow.connected(ID, activity);
    return ConnectorRow.error(ID, activity, new ConnectorError("CONNECTION_INVALID", activity));
  }

  private Instant lastActivity(String ownerId, StoredConnection connection) {
    return receipts
        .latest(ownerId, GithubIssueConnections.SOURCE)
        .map(GithubStatusSource::movedAt)
        .orElseGet(connection::connectedAt);
  }

  /** Una importación en curso todavía no ha terminado: lo último que se sabe es cuándo empezó. */
  private static Instant movedAt(IssueImportReceipt receipt) {
    return receipt.finishedAt() == null ? receipt.startedAt() : receipt.finishedAt();
  }
}
