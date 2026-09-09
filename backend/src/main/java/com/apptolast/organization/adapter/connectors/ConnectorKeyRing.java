package com.apptolast.organization.adapter.connectors;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
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

  /** El byte de versión sólo ordena a las candidatas: quien decide es la etiqueta de GCM. */
  List<ConnectorKey> candidatesFor(byte keyVersion) {
    return keys.stream()
        .sorted(Comparator.comparing((ConnectorKey key) -> key.version() != keyVersion))
        .toList();
  }

  private static ConnectorKey parse(String property, String raw) {
    byte[] material;
    try {
      material = Base64.getDecoder().decode(raw.trim());
    } catch (IllegalArgumentException error) {
      throw malformed(property);
    }
    if (material.length != KEY_BYTES) throw malformed(property);
    return new ConnectorKey(version(material), new SecretKeySpec(material, "AES"));
  }

  private static byte version(byte[] material) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(material)[0];
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 must be available", error);
    }
  }

  private static IllegalArgumentException malformed(String property) {
    return new IllegalArgumentException(
        property + " debe ser base64 de exactamente 32 bytes; revisa su variable de entorno");
  }

  record ConnectorKey(byte version, SecretKeySpec material) {}
}
