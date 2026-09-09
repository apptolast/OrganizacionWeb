package com.apptolast.organization.application;

import java.util.ArrayList;
import java.util.List;

/**
 * Reúne las seis filas del catálogo preguntando a cada conector por la suya. El orden lo fija la
 * lista de fuentes que se inyecta y se conserva tal cual: la pantalla depende de que no baile.
 *
 * <p>Sin clave de cifrado, a las fuentes que guardan secretos no se les pregunta siquiera. No es
 * una optimización: es lo que impide que un intento de descifrado fallido se convierta en un error
 * del catálogo entero, cuando lo honesto es decir que ese conector está deshabilitado.
 */
public final class ReadConnectorCatalog implements ReadConnectorCatalogUseCase {
  private final List<ConnectorStatusSource> sources;
  private final SecretCipher cipher;

  public ReadConnectorCatalog(List<ConnectorStatusSource> sources, SecretCipher cipher) {
    this.sources = List.copyOf(sources);
    this.cipher = cipher;
  }

  @Override
  public ConnectorCatalog execute(String ownerId) {
    var rows = new ArrayList<ConnectorRow>();
    for (var source : sources) rows.add(rowOf(source, ownerId));
    return new ConnectorCatalog(rows);
  }

  private ConnectorRow rowOf(ConnectorStatusSource source, String ownerId) {
    if (source.encryptsSecrets() && !cipher.enabled()) return ConnectorRow.disabled(source.id());
    return source.read(ownerId);
  }
}
