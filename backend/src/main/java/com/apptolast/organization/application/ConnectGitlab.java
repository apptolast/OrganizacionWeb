package com.apptolast.organization.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.apptolast.organization.domain.GitlabProjectPath;
import com.apptolast.organization.domain.PersonalAccessToken;
import java.time.Clock;

/**
 * Guarda o sustituye la conexión de GitLab del propietario. La ruta y el token se validan antes de
 * abrir ninguna conexión saliente, y la fila sólo se escribe cuando GitLab ha confirmado el
 * proyecto: cualquier fallo deja la conexión anterior exactamente como estaba.
 */
public final class ConnectGitlab implements ConnectGitlabUseCase {
  /** GitLab acorta más que GitHub: un PAT suyo no pasa de doscientos caracteres. */
  private static final int TOKEN_LIMIT = 200;

  private static final long FIRST_VERSION = 1L;

  private final GitlabConnectionStore connections;
  private final GitlabProjectDirectory directory;
  private final String apiBase;
  private final SecretCipher cipher;
  private final Clock clock;

  public ConnectGitlab(
      GitlabConnectionStore connections,
      GitlabProjectDirectory directory,
      String apiBase,
      SecretCipher cipher,
      Clock clock) {
    this.connections = connections;
    this.directory = directory;
    this.apiBase = apiBase;
    this.cipher = cipher;
    this.clock = clock;
  }

  @Override
  public GitlabConnectionView execute(String ownerId, String token, String projectPath) {
    var path = GitlabProjectPath.parse(projectPath);
    var secret = PersonalAccessToken.upTo(TOKEN_LIMIT, token);
    var project = verify(path, secret);
    var row =
        new GitlabConnection(
            project.pathWithNamespace(),
            project.id(),
            secret.hint(),
            GitlabConnection.CONNECTED,
            cipher.encrypt(ownerId, secret.value()),
            clock.instant().truncatedTo(MICROS),
            null,
            null,
            nextVersion(ownerId));
    connections.save(ownerId, row);
    return GitlabConnectionView.of(row, apiBase);
  }

  /** Cada sustitución de token sube la versión; se publica sólo para diagnóstico. */
  private long nextVersion(String ownerId) {
    return connections.find(ownerId).map(row -> row.version() + 1).orElse(FIRST_VERSION);
  }

  private GitlabProject verify(GitlabProjectPath path, PersonalAccessToken secret) {
    try {
      return directory.verify(path.value(), secret.value());
    } catch (IssueSourceException error) {
      throw ConnectorFailures.whileConnectingGitlab(error);
    }
  }
}
