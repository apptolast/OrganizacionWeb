package com.apptolast.organization.adapter.webhook;

import com.apptolast.organization.application.WebhookSecrets;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM: version(1) || nonce(12) || ciphertext || tag(16).
 *
 * <p>Amendment B5: the leading byte identifies the key that sealed the row, so rotating {@code
 * APP_CONNECTOR_KEY} into {@code APP_CONNECTOR_KEY_PREVIOUS} keeps stored secrets readable. A
 * malformed key fails fast at startup; only an absent key degrades into {@link
 * WebhookSecrets#DISABLED}.
 *
 * <p>Amendment B6: the associated data is {@code ownerId + "|" + endpointId}.
 */
public final class AesGcmWebhookSecrets implements WebhookSecrets {
  private static final int KEY_BYTES = 32;
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private static final int VERSION_BYTES = 1;
  private static final String SEPARATOR = "|";

  private record VersionedKey(byte version, SecretKeySpec key) {}

  private final VersionedKey current;
  private final List<VersionedKey> known;
  private final SecureRandom random = new SecureRandom();

  private AesGcmWebhookSecrets(VersionedKey current, VersionedKey previous) {
    this.current = current;
    var all = new ArrayList<VersionedKey>();
    all.add(current);
    if (previous != null) all.add(previous);
    this.known = List.copyOf(all);
  }

  /**
   * Returns null when the current key is absent, which leaves connectors disabled. Throws when
   * either key is present but malformed, so a bad deployment fails at startup.
   */
  public static AesGcmWebhookSecrets from(String current, String previous) {
    if (isAbsent(current)) return null;
    return new AesGcmWebhookSecrets(
        versioned(current, "APP_CONNECTOR_KEY"),
        isAbsent(previous) ? null : versioned(previous, "APP_CONNECTOR_KEY_PREVIOUS"));
  }

  private static boolean isAbsent(String encoded) {
    return encoded == null || encoded.isBlank();
  }

  private static VersionedKey versioned(String encoded, String name) {
    byte[] key;
    try {
      key = Base64.getDecoder().decode(encoded.strip());
    } catch (IllegalArgumentException malformed) {
      throw new IllegalStateException(name + " is not valid base64", malformed);
    }
    if (key.length != KEY_BYTES)
      throw new IllegalStateException(name + " must decode to exactly " + KEY_BYTES + " bytes");
    return new VersionedKey(version(key), new SecretKeySpec(key, "AES"));
  }

  /** A stable identifier for the key material that reveals nothing about it. */
  private static byte version(byte[] key) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(key)[0];
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
    var nonce = new byte[NONCE_BYTES];
    random.nextBytes(nonce);
    try {
      var cipher = cipher(Cipher.ENCRYPT_MODE, current, nonce, ownerId, endpointId);
      var sealed = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(VERSION_BYTES + nonce.length + sealed.length)
          .put(current.version())
          .put(nonce)
          .put(sealed)
          .array();
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException("Webhook secret cannot be sealed", error);
    }
  }

  @Override
  public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
    if (ciphertext == null || ciphertext.length <= VERSION_BYTES + NONCE_BYTES)
      throw new IllegalStateException("Webhook secret cannot be opened");
    var nonce = Arrays.copyOfRange(ciphertext, VERSION_BYTES, VERSION_BYTES + NONCE_BYTES);
    var offset = VERSION_BYTES + NONCE_BYTES;
    for (var candidate : known) {
      if (candidate.version() != ciphertext[0]) continue;
      try {
        var cipher = cipher(Cipher.DECRYPT_MODE, candidate, nonce, ownerId, endpointId);
        return new String(
            cipher.doFinal(ciphertext, offset, ciphertext.length - offset), StandardCharsets.UTF_8);
      } catch (GeneralSecurityException wrongKeyOrData) {
        // Try the next key with the same version byte, if any.
      }
    }
    throw new IllegalStateException("Webhook secret cannot be opened");
  }

  private Cipher cipher(int mode, VersionedKey key, byte[] nonce, String ownerId, UUID endpointId)
      throws GeneralSecurityException {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key.key(), new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD(associatedData(ownerId, endpointId));
    return cipher;
  }

  private static byte[] associatedData(String ownerId, UUID endpointId) {
    return (ownerId + SEPARATOR + endpointId).getBytes(StandardCharsets.UTF_8);
  }
}
