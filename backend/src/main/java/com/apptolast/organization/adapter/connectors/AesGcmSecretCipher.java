package com.apptolast.organization.adapter.connectors;

import com.apptolast.organization.application.ConnectorsDisabledException;
import com.apptolast.organization.application.SecretCipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM con nonce nuevo por escritura, etiqueta de 128 bits y el propietario como dato
 * autenticado adicional. Formato: 12 bytes de nonce seguidos del sellado (texto cifrado y
 * etiqueta), que es el que fijan los dos contratos: @s1 de la feature 27 exige octet_length 12 + 14
 * + 16 y @s2 de la feature 28, "12 bytes de nonce más cifrado".
 *
 * <p>La rotación del hallazgo B5 sobrevive sin byte de versión: el llavero prueba las claves y es
 * la etiqueta de GCM quien decide, que es lo que ya hacía antes.
 */
public final class AesGcmSecretCipher implements SecretCipher {
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final int SHORTEST = NONCE_BYTES + TAG_BITS / 8;
  private final ConnectorKeyRing ring;
  private final SecureRandom random;

  public AesGcmSecretCipher(ConnectorKeyRing ring, SecureRandom random) {
    this.ring = ring;
    this.random = random;
  }

  @Override
  public boolean enabled() {
    return ring.enabled();
  }

  @Override
  public byte[] encrypt(String ownerId, String plaintext) {
    if (!ring.enabled()) throw new ConnectorsDisabledException();
    var key = ring.current();
    var nonce = new byte[NONCE_BYTES];
    random.nextBytes(nonce);
    byte[] sealed;
    try {
      sealed = cipher(Cipher.ENCRYPT_MODE, key, nonce, ownerId).doFinal(bytes(plaintext));
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("AES-GCM encryption failed", error);
    }
    var output = new byte[NONCE_BYTES + sealed.length];
    System.arraycopy(nonce, 0, output, 0, NONCE_BYTES);
    System.arraycopy(sealed, 0, output, NONCE_BYTES, sealed.length);
    return output;
  }

  @Override
  public Optional<String> decrypt(String ownerId, byte[] ciphertext) {
    if (!ring.enabled()) throw new ConnectorsDisabledException();
    if (ciphertext == null || ciphertext.length <= SHORTEST) return Optional.empty();
    var nonce = Arrays.copyOf(ciphertext, NONCE_BYTES);
    var sealed = Arrays.copyOfRange(ciphertext, NONCE_BYTES, ciphertext.length);
    for (var key : ring.candidates()) {
      try {
        return Optional.of(
            new String(
                cipher(Cipher.DECRYPT_MODE, key, nonce, ownerId).doFinal(sealed),
                StandardCharsets.UTF_8));
      } catch (GeneralSecurityException ignored) {
        // La etiqueta descarta la clave equivocada; se prueba la siguiente del llavero.
      }
    }
    return Optional.empty();
  }

  private static Cipher cipher(
      int mode, ConnectorKeyRing.ConnectorKey key, byte[] nonce, String ownerId)
      throws GeneralSecurityException {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key.material(), new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD(bytes(ownerId));
    return cipher;
  }

  private static byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
