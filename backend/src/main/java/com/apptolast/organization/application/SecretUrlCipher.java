package com.apptolast.organization.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifra la dirección iCal en reposo con AES-256-GCM: byte de versión de clave, nonce aleatorio por
 * escritura y propietario como dato adicional autenticado. Ningún mensaje de error revela la clave
 * ni la dirección.
 *
 * <p>Enmienda B5 de la revisión de seguridad: el byte de versión y la clave anterior permiten rotar
 * {@code APP_CONNECTOR_KEY} sin inutilizar las suscripciones ya guardadas. Una clave mal formada
 * detiene el arranque; una clave ausente deja los conectores en modo degradado.
 */
public final class SecretUrlCipher {
  public static final int NONCE_LENGTH = 12;
  public static final int VERSION_LENGTH = 1;
  public static final int HEADER_LENGTH = VERSION_LENGTH + NONCE_LENGTH;
  public static final int KEY_LENGTH = 32;
  private static final int TAG_BITS = 128;
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final String CURRENT_SETTING = "APP_CONNECTOR_KEY";
  private static final String PREVIOUS_SETTING = "APP_CONNECTOR_KEY_PREVIOUS";

  private final VersionedKey current;
  private final List<VersionedKey> readable;
  private final SecureRandom random;

  private SecretUrlCipher(VersionedKey current, List<VersionedKey> readable, SecureRandom random) {
    this.current = current;
    this.readable = readable;
    this.random = random;
  }

  public static SecretUrlCipher of(String configuredKey) {
    return of(configuredKey, null);
  }

  public static SecretUrlCipher of(String configuredKey, String previousKey) {
    var currentKey = VersionedKey.of(configuredKey, CURRENT_SETTING);
    var keys = new ArrayList<VersionedKey>();
    keys.add(currentKey);
    if (previousKey != null && !previousKey.isEmpty())
      keys.add(VersionedKey.of(previousKey, PREVIOUS_SETTING));
    return new SecretUrlCipher(currentKey, List.copyOf(keys), new SecureRandom());
  }

  public byte[] encrypt(String ownerId, String url) {
    var nonce = new byte[NONCE_LENGTH];
    random.nextBytes(nonce);
    try {
      var cipher = cipherFor(Cipher.ENCRYPT_MODE, current, nonce, ownerId);
      var sealed = cipher.doFinal(url.getBytes(StandardCharsets.UTF_8));
      var stored = new byte[HEADER_LENGTH + sealed.length];
      stored[0] = current.version();
      System.arraycopy(nonce, 0, stored, VERSION_LENGTH, NONCE_LENGTH);
      System.arraycopy(sealed, 0, stored, HEADER_LENGTH, sealed.length);
      return stored;
    } catch (java.security.GeneralSecurityException error) {
      throw new IllegalStateException("No se ha podido cifrar la dirección del conector.");
    }
  }

  /** Vacío significa {@code SECRET_UNREADABLE}: hay que volver a pedir la dirección. */
  public Optional<String> decrypt(String ownerId, byte[] stored) {
    if (stored == null || stored.length <= HEADER_LENGTH) return Optional.empty();
    var nonce = Arrays.copyOfRange(stored, VERSION_LENGTH, HEADER_LENGTH);
    return candidates(stored[0])
        .map(key -> open(key, nonce, ownerId, stored))
        .flatMap(Optional::stream)
        .findFirst();
  }

  /** El byte de versión solo ordena los intentos; la etiqueta GCM es quien decide. */
  private java.util.stream.Stream<VersionedKey> candidates(byte version) {
    return readable.stream()
        .sorted(Comparator.comparing(key -> key.version() == version ? 0 : 1));
  }

  private Optional<String> open(VersionedKey key, byte[] nonce, String ownerId, byte[] stored) {
    try {
      var plain =
          cipherFor(Cipher.DECRYPT_MODE, key, nonce, ownerId)
              .doFinal(stored, HEADER_LENGTH, stored.length - HEADER_LENGTH);
      return Optional.of(new String(plain, StandardCharsets.UTF_8));
    } catch (java.security.GeneralSecurityException unreadable) {
      return Optional.empty();
    }
  }

  private static Cipher cipherFor(int mode, VersionedKey key, byte[] nonce, String ownerId)
      throws java.security.GeneralSecurityException {
    var cipher = Cipher.getInstance(TRANSFORMATION);
    cipher.init(mode, key.material(), new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD(ownerId.getBytes(StandardCharsets.UTF_8));
    return cipher;
  }

  private record VersionedKey(SecretKeySpec material, byte version) {
    static VersionedKey of(String configured, String setting) {
      byte[] decoded;
      try {
        decoded = Base64.getDecoder().decode(configured == null ? "" : configured);
      } catch (IllegalArgumentException malformed) {
        throw unusable(setting);
      }
      if (decoded.length != KEY_LENGTH) throw unusable(setting);
      return new VersionedKey(new SecretKeySpec(decoded, KEY_ALGORITHM), version(decoded));
    }

    /** Identificador derivado del material: sobrevive a la rotación entre las dos ranuras. */
    private static byte version(byte[] material) {
      try {
        return MessageDigest.getInstance("SHA-256").digest(material)[0];
      } catch (java.security.NoSuchAlgorithmException impossible) {
        throw new IllegalStateException("Falta SHA-256 en la plataforma.", impossible);
      }
    }

    private static IllegalStateException unusable(String setting) {
      return new IllegalStateException(
          setting + " debe contener 32 bytes en base64 para cifrar las direcciones de los conectores.");
    }
  }
}
