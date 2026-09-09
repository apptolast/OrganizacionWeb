package com.apptolast.organization.application;

/**
 * Borra la conexión de GitLab y con ella el único texto cifrado del token. Las tareas, sus enlaces
 * y los recibos sobreviven: son trabajo propio, no credenciales. Desconectar dos veces seguidas
 * responde igual sin ser un error.
 */
public final class DisconnectGitlab implements DisconnectGitlabUseCase {
  private final GitlabConnectionStore connections;
  private final SecretCipher cipher;

  public DisconnectGitlab(GitlabConnectionStore connections, SecretCipher cipher) {
    this.connections = connections;
    this.cipher = cipher;
  }

  @Override
  public void execute(String ownerId) {
    connections.delete(ownerId);
  }
}
