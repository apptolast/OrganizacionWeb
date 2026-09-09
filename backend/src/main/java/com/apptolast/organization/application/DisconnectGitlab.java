package com.apptolast.organization.application;

/**
 * Borra la conexión de GitLab y con ella el único texto cifrado del token. Las tareas, sus enlaces
 * y los recibos sobreviven: son trabajo propio, no credenciales. Desconectar dos veces seguidas
 * responde igual sin ser un error.
 */
public final class DisconnectGitlab implements DisconnectGitlabUseCase {
  private final GitlabConnectionStore connections;
  private final IssueImportReceiptStore receipts;
  private final SecretCipher cipher;
  private final java.time.Clock clock;

  public DisconnectGitlab(
      GitlabConnectionStore connections,
      IssueImportReceiptStore receipts,
      SecretCipher cipher,
      java.time.Clock clock) {
    this.connections = connections;
    this.receipts = receipts;
    this.cipher = cipher;
    this.clock = clock;
  }

  @Override
  public void execute(String ownerId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    ImportGuard.requireIdle(receipts, ownerId, clock);
    connections.delete(ownerId);
  }
}
