package com.apptolast.organization.application;

/**
 * Borra la conexión y con ella el único texto cifrado del token. Las tareas, los enlaces y los
 * recibos ya importados sobreviven: son datos propios, no credenciales. Es idempotente, así que
 * desconectar dos veces seguidas responde igual sin ser un error.
 */
public final class DisconnectGithub implements DisconnectGithubUseCase {
  private final ConnectorConnectionStore connections;
  private final SecretCipher cipher;

  public DisconnectGithub(ConnectorConnectionStore connections, SecretCipher cipher) {
    this.connections = connections;
    this.cipher = cipher;
  }

  @Override
  public void execute(String ownerId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    connections.delete(ownerId);
  }
}
