package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;
import java.time.Clock;
import java.time.Instant;

/**
 * La fila de las credenciales de API (24). Está conectada mientras quede una credencial vigente: ni
 * revocada ni caducada. Se recorren las páginas hasta encontrar la primera, porque un propietario
 * con muchas credenciales revocadas no puede aparecer como desconectado sólo por el tamaño de la
 * primera página.
 *
 * <p>{@code lastActivityAt} es siempre nulo: la feature 24 no registra el uso de una credencial, y
 * publicar la fecha de alta como si fuera actividad sería inventarla.
 */
public final class ApiCredentialStatusSource implements ConnectorStatusSource {
  public static final String ID = "api_credentials";

  private final ApiCredentialQueries credentials;
  private final Clock clock;

  public ApiCredentialStatusSource(ApiCredentialQueries credentials, Clock clock) {
    this.credentials = credentials;
    this.clock = clock;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return false;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    var now = clock.instant();
    String cursor = null;
    do {
      var page = credentials.list(ownerId, cursor);
      if (page.items().stream().anyMatch(credential -> vigent(credential, now)))
        return ConnectorRow.connected(ID, null);
      cursor = page.nextCursor();
    } while (cursor != null);
    return ConnectorRow.notConnected(ID);
  }

  private static boolean vigent(ApiCredential credential, Instant now) {
    return credential.revokedAt() == null && now.isBefore(credential.expiresAt());
  }
}
