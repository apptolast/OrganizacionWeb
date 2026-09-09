package com.apptolast.organization.adapter.connectors;

import com.apptolast.organization.application.ConnectorsDisabledException;
import com.apptolast.organization.application.SecretCipher;
import com.apptolast.organization.application.SecretUndecipherableException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM con nonce nuevo por escritura, etiqueta de 128 bits y el propietario como dato
 * autenticado adicional. Formato: 1 byte de versión de clave, 12 de nonce, texto cifrado y
 * etiqueta.
 */
public final class AesGcmSecretCipher implements SecretCipher {
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final int VERSION_BYTES = 1;
  private static final int SHORTEST = VERSION_BYTES + NONCE_BYTES + TAG_BITS / 8;
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
    var output = new byte[VERSION_BYTES + NONCE_BYTES + sealed.length];
    output[0] = key.version();
    System.arraycopy(nonce, 0, output, VERSION_BYTES, NONCE_BYTES);
    System.arraycopy(sealed, 0, output, VERSION_BYTES + NONCE_BYTES, sealed.length);
    return output;
  }

  @Override
  public String decrypt(String ownerId, byte[] ciphertext) {
    if (!ring.enabled()) throw new ConnectorsDisabledException();
    if (ciphertext == null || ciphertext.length <= SHORTEST)
      throw new SecretUndecipherableException();
    var nonce = Arrays.copyOfRange(ciphertext, VERSION_BYTES, VERSION_BYTES + NONCE_BYTES);
    var sealed = Arrays.copyOfRange(ciphertext, VERSION_BYTES + NONCE_BYTES, ciphertext.length);
    for (var key : ring.candidatesFor(ciphertext[0])) {
      try {
        return new String(
            cipher(Cipher.DECRYPT_MODE, key, nonce, ownerId).doFinal(sealed),
            StandardCharsets.UTF_8);
      } catch (GeneralSecurityException ignored) {
        // La etiqueta descarta la clave equivocada; se prueba la siguiente del llavero.
      }
    }
    throw new SecretUndecipherableException();
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
