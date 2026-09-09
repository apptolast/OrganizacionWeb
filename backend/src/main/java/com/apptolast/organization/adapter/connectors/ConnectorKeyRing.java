package com.apptolast.organization.adapter.connectors;

import java.util.Base64;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

/**
 * Llavero de conectores (hallazgo B5). {@code app.connectors.key} cifra las escrituras nuevas y
 * {@code app.connectors.key-previous} sigue descifrando lo guardado antes de una rotación. Clave
 * ausente: el conector queda deshabilitado. Clave presente y mal formada: la aplicación no arranca.
 */
public final class ConnectorKeyRing {
  private static final int KEY_BYTES = 32;
  private static final String CURRENT = "app.connectors.key";
  private static final String PREVIOUS = "app.connectors.key-previous";
  private final List<ConnectorKey> keys;

  private ConnectorKeyRing(List<ConnectorKey> keys) {
    this.keys = keys;
  }

  public static ConnectorKeyRing of(String current, String previous) {
    if (current == null) return new ConnectorKeyRing(List.of());
    var head = parse(CURRENT, current);
    return new ConnectorKeyRing(
        previous == null ? List.of(head) : List.of(head, parse(PREVIOUS, previous)));
  }

  public boolean enabled() {
    return !keys.isEmpty();
  }

  ConnectorKey current() {
    return keys.getFirst();
  }

  /** Se prueban en orden, la vigente primero: quien decide es la etiqueta de GCM. */
  List<ConnectorKey> candidates() {
    return keys;
  }

  private static ConnectorKey parse(String property, String raw) {
    byte[] material;
    try {
      material = Base64.getDecoder().decode(raw.trim());
    } catch (IllegalArgumentException error) {
      throw malformed(property);
    }
    if (material.length != KEY_BYTES) throw malformed(property);
    return new ConnectorKey(new SecretKeySpec(material, "AES"));
  }

  /** Nombra la propiedad y su variable de entorno: @s9 de la feature 28 exige la segunda. */
  private static IllegalArgumentException malformed(String property) {
    var variable = CURRENT.equals(property) ? "APP_CONNECTOR_KEY" : "APP_CONNECTOR_KEY_PREVIOUS";
    return new IllegalArgumentException(
        property
            + " ("
            + variable
            + ") debe ser base64 de exactamente 32 bytes; revisa su variable de entorno");
  }

  record ConnectorKey(SecretKeySpec material) {}
}
