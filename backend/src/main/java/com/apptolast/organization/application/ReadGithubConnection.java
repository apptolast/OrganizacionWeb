package com.apptolast.organization.application;

/**
 * Devuelve la conexión del propietario junto a su última importación, que es la de mayor instante
 * de inicio. Sin conexión no hay nada que contar: {@link ConnectionNotFoundException}.
 */
public final class ReadGithubConnection implements ReadGithubConnectionUseCase {
  private final ConnectorConnectionStore connections;
  private final IssueImportReceiptStore receipts;
  private final SecretCipher cipher;

  public ReadGithubConnection(
      ConnectorConnectionStore connections, IssueImportReceiptStore receipts, SecretCipher cipher) {
    this.connections = connections;
    this.receipts = receipts;
    this.cipher = cipher;
  }

  @Override
  public ConnectionView execute(String ownerId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    var connection = connections.find(ownerId).orElseThrow(ConnectionNotFoundException::new);
    return ConnectionView.of(connection, receipts.latest(ownerId, GithubIssueConnections.SOURCE).orElse(null));
  }
}
