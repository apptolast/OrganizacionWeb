package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.time.Instant;

/**
 * Lo único que la API cuenta sobre una conexión. No hay hueco para el token: ni cifrado ni en
 * claro, ni siquiera para su longitud.
 */
public record ConnectionView(
    String repository, String login, String status, Instant connectedAt, IssueImportReceipt lastImport) {
  public static ConnectionView of(StoredConnection connection, IssueImportReceipt lastImport) {
    return new ConnectionView(
        connection.repository(),
        connection.login(),
        connection.status(),
        connection.connectedAt(),
        lastImport);
  }
}
