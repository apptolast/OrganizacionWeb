package com.apptolast.organization.adapter.crypto;

import com.apptolast.organization.application.SecretCipher;
import java.util.Optional;

/**
 * Elige el cifrado de los conectores a partir de la configuración. Clave ausente: los conectores
 * quedan deshabilitados y ningún secreto se puede leer ni escribir. Clave presente pero mal
 * formada: el arranque falla, sin revelar su valor (@s9).
 */
public final class ConnectorCipher {
  private ConnectorCipher() {}

  public static SecretCipher from(String key, String previousKey) {
    return key == null || key.isBlank()
        ? new Unavailable()
        : AesGcmSecretCipher.of(key, previousKey);
  }

  public static boolean isConfigured(String key) {
    return key != null && !key.isBlank();
  }

  private static final class Unavailable implements SecretCipher {
    @Override
    public byte[] encrypt(String ownerId, String url) {
      throw new IllegalStateException("APP_CONNECTOR_KEY no está definida.");
    }

    @Override
    public Optional<String> decrypt(String ownerId, byte[] stored) {
      return Optional.empty();
    }
  }
}
