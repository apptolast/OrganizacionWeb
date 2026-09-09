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
  private final ImportReceiptStore receipts;
  private final IssueSource source;
  private final SecretCipher cipher;
  private final Clock clock;

  public ConnectGithub(
      ConnectorConnectionStore connections,
      ImportReceiptStore receipts,
      IssueSource source,
      SecretCipher cipher,
      Clock clock) {
    this.connections = connections;
    this.receipts = receipts;
    this.source = source;
    this.cipher = cipher;
    this.clock = clock;
  }

  @Override
  public ConnectionView execute(String ownerId, String repository, String token) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    var target = GithubRepository.parse(repository);
    var secret = new PersonalAccessToken(token);
    var identity = identify(target, secret);
    var connectedAt = clock.instant().truncatedTo(MICROS);
    var row =
        new StoredConnection(
            identity.fullName(),
            identity.login(),
            StoredConnection.VALID,
            cipher.encrypt(ownerId, secret.value()),
            connectedAt);
    connections.save(ownerId, row);
    return ConnectionView.of(row, receipts.latest(ownerId).orElse(null));
  }

  private RepositoryIdentity identify(GithubRepository target, PersonalAccessToken secret) {
    try {
      return source.verify(target.fullName(), secret.value());
    } catch (IssueSourceException error) {
      throw ConnectorFailures.whileConnecting(error);
    }
  }
}
