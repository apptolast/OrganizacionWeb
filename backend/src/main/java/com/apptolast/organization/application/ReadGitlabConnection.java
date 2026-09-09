package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.time.Instant;

/**
 * Devuelve la conexión de GitLab del propietario. Sin fila la respuesta no es un error: es una
 * vista {@code not_connected}, que es lo que la pantalla necesita para ofrecer el formulario.
 *
 * <p>La última actividad no se guarda en la fila: se deduce del último recibo de este mismo gestor,
 * y sin recibos es el instante en que se conectó. Deducirla es lo que impide que se quede vieja
 * cuando una importación termina y nadie se acuerda de actualizar la conexión.
 */
public final class ReadGitlabConnection implements ReadGitlabConnectionUseCase {
  private final GitlabConnectionStore connections;
  private final IssueImportReceiptStore receipts;
  private final String apiBase;
  private final SecretCipher cipher;

  public ReadGitlabConnection(
      GitlabConnectionStore connections,
      IssueImportReceiptStore receipts,
      String apiBase,
      SecretCipher cipher) {
    this.connections = connections;
    this.receipts = receipts;
    this.apiBase = apiBase;
    this.cipher = cipher;
  }

  @Override
  public GitlabConnectionView execute(String ownerId) {
    if (!cipher.enabled()) throw new ConnectorsDisabledException();
    return connections
        .find(ownerId)
        .map(row -> GitlabConnectionView.of(row, apiBase, lastActivity(ownerId, row)))
        .orElseGet(GitlabConnectionView::notConnected);
  }

  private Instant lastActivity(String ownerId, GitlabConnection connection) {
    return receipts
        .latest(ownerId, GitlabIssueConnections.SOURCE)
        .map(ReadGitlabConnection::movedAt)
        .orElse(connection.lastActivityAt());
  }

  /** Una importación en curso todavía no ha terminado: lo último que se sabe es cuándo empezó. */
  private static Instant movedAt(IssueImportReceipt receipt) {
    return receipt.finishedAt() == null ? receipt.startedAt() : receipt.finishedAt();
  }
}
