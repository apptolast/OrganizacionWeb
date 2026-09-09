package com.apptolast.organization.application;

/**
 * Devuelve la conexión de GitLab del propietario. Sin fila la respuesta no es un error: es una
 * vista {@code not_connected}, que es lo que la pantalla necesita para ofrecer el formulario.
 */
public final class ReadGitlabConnection implements ReadGitlabConnectionUseCase {
  private final GitlabConnectionStore connections;
  private final String apiBase;
  private final SecretCipher cipher;

  public ReadGitlabConnection(
      GitlabConnectionStore connections, String apiBase, SecretCipher cipher) {
    this.connections = connections;
    this.apiBase = apiBase;
    this.cipher = cipher;
  }

  @Override
  public GitlabConnectionView execute(String ownerId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    return connections
        .find(ownerId)
        .map(row -> GitlabConnectionView.of(row, apiBase))
        .orElseGet(GitlabConnectionView::notConnected);
  }
}
