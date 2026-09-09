package com.apptolast.organization.adapter.crypto;

import com.apptolast.organization.application.SecretCipher;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra la dirección iCal en reposo con AES-256-GCM. Formato almacenado fijado por el contrato:
 * nonce aleatorio de 12 bytes por escritura seguido del sellado, con el propietario como dato
 * adicional autenticado. Ningún mensaje de error revela la clave ni la dirección.
 *
 * <p>Enmienda B5 de la revisión de seguridad, reducida al formato del contrato: no hay byte de
 * versión de clave; una clave anterior opcional se prueba a ciegas y es la etiqueta GCM quien
 * decide. Una clave mal formada detiene el arranque; una clave ausente deja los conectores en modo
 * degradado.
 */
public final class AesGcmSecretCipher implements SecretCipher {
  public static final int NONCE_LENGTH = 12;
  public static final int HEADER_LENGTH = NONCE_LENGTH;
  public static final int KEY_LENGTH = 32;
  private static final int TAG_BITS = 128;
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final String CURRENT_SETTING = "APP_CONNECTOR_KEY";
  private static final String PREVIOUS_SETTING = "APP_CONNECTOR_KEY_PREVIOUS";

  private final SecretKey current;
  private final List<SecretKey> readable;
  private final SecureRandom random;

  private AesGcmSecretCipher(SecretKey current, List<SecretKey> readable, SecureRandom random) {
    this.current = current;
    this.readable = readable;
    this.random = random;
  }

  public static AesGcmSecretCipher of(String configuredKey) {
    return of(configuredKey, null);
  }

  public static AesGcmSecretCipher of(String configuredKey, String previousKey) {
    var currentKey = material(configuredKey, CURRENT_SETTING);
    var keys = new ArrayList<SecretKey>();
    keys.add(currentKey);
    if (previousKey != null && !previousKey.isEmpty())
      keys.add(material(previousKey, PREVIOUS_SETTING));
    return new AesGcmSecretCipher(currentKey, List.copyOf(keys), new SecureRandom());
  }

  @Override
  public byte[] encrypt(String ownerId, String url) {
    var nonce = new byte[NONCE_LENGTH];
    random.nextBytes(nonce);
    try {
      var cipher = cipherFor(Cipher.ENCRYPT_MODE, current, nonce, ownerId);
      var sealed = cipher.doFinal(url.getBytes(StandardCharsets.UTF_8));
      var stored = new byte[NONCE_LENGTH + sealed.length];
      System.arraycopy(nonce, 0, stored, 0, NONCE_LENGTH);
      System.arraycopy(sealed, 0, stored, NONCE_LENGTH, sealed.length);
      return stored;
    } catch (java.security.GeneralSecurityException error) {
      throw new IllegalStateException("No se ha podido cifrar la dirección del conector.");
    }
  }

  /** Vacío significa {@code SECRET_UNREADABLE}: hay que volver a pedir la dirección. */
  @Override
  public Optional<String> decrypt(String ownerId, byte[] stored) {
    if (stored == null || stored.length <= NONCE_LENGTH) return Optional.empty();
    var nonce = Arrays.copyOf(stored, NONCE_LENGTH);
    return readable.stream()
        .map(key -> open(key, nonce, ownerId, stored))
        .flatMap(Optional::stream)
        .findFirst();
  }

  private Optional<String> open(SecretKey key, byte[] nonce, String ownerId, byte[] stored) {
    try {
      var plain =
          cipherFor(Cipher.DECRYPT_MODE, key, nonce, ownerId)
              .doFinal(stored, NONCE_LENGTH, stored.length - NONCE_LENGTH);
      return Optional.of(new String(plain, StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException unreadable) {
      return Optional.empty();
    }
  }

  private static Cipher cipherFor(int mode, SecretKey key, byte[] nonce, String ownerId)
      throws java.security.GeneralSecurityException {
    var cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD(ownerId.getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  private static SecretKey material(String configured, String setting) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(configured == null ? "" : configured);
    } catch (IllegalArgumentException malformed) {
      throw unusable(setting);
    }
    if (decoded.length != KEY_LENGTH) throw unusable(setting);
    return new SecretKeySpec(decoded, KEY_ALGORITHM);
  }

  private static IllegalStateException unusable(String setting) {
    return new IllegalStateException(
        setting
            + " debe contener 32 bytes en base64 para cifrar las direcciones de los conectores.");
  }
}
