package com.apptolast.organization.application;

import static java.time.temporal.ChronoUnit.MICROS;

import com.apptolast.organization.domain.GithubRepository;
import com.apptolast.organization.domain.PersonalAccessToken;
import java.time.Clock;

/**
 * Guarda o sustituye la conexión del propietario. El repositorio y el token se validan antes de
 * abrir ninguna conexión saliente, y la fila sólo se escribe cuando el gestor externo ha confirmado
 * las credenciales: cualquier fallo deja la conexión anterior exactamente como estaba.
 */
public final class ConnectGithub implements ConnectGithubUseCase {
  private final ConnectorConnectionStore connections;
  private final IssueImportReceiptStore receipts;
  private final GithubRepositoryDirectory directory;
  private final SecretCipher cipher;
  private final ConnectorAudit audit;
  private final Clock clock;

  public ConnectGithub(
      ConnectorConnectionStore connections,
      IssueImportReceiptStore receipts,
      GithubRepositoryDirectory directory,
      SecretCipher cipher,
      ConnectorAudit audit,
      Clock clock) {
    this.connections = connections;
    this.receipts = receipts;
    this.directory = directory;
    this.cipher = cipher;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public ConnectionView execute(String ownerId, String repository, String token) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    var target = GithubRepository.parse(repository);
    var secret = new PersonalAccessToken(token);
    var identity = identify(ownerId, target, secret);
    var connectedAt = clock.instant().truncatedTo(MICROS);
    var row =
        new StoredConnection(
            identity.fullName(),
            identity.login(),
            StoredConnection.VALID,
            cipher.encrypt(ownerId, secret.value()),
            connectedAt);
    connections.save(ownerId, row);
    audit.connected(
        GithubIssueConnections.SOURCE, ownerId, row.repository(), row.login());
    return ConnectionView.of(row, receipts.latest(ownerId, GithubIssueConnections.SOURCE).orElse(null));
  }

  private RepositoryIdentity identify(
      String ownerId, GithubRepository target, PersonalAccessToken secret) {
    try {
      return directory.verify(target.fullName(), secret.value());
    } catch (IssueSourceException error) {
      audit.connectionRefused(
          GithubIssueConnections.SOURCE,
          ownerId,
          target.fullName(),
          ConnectorFailures.connectErrorCode(error),
          error.providerStatus());
      throw ConnectorFailures.whileConnecting(error);
    }
  }
}
