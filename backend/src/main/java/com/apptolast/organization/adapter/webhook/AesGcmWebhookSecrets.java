package com.apptolast.organization.adapter.webhook;

import com.apptolast.organization.application.WebhookSecrets;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM: nonce(12) || ciphertext || tag(16), with the endpoint id as associated data. */
public final class AesGcmWebhookSecrets implements WebhookSecrets {
  private static final int KEY_BYTES = 32;
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;
  private final SecretKeySpec key;
  private final SecureRandom random = new SecureRandom();

  private AesGcmWebhookSecrets(byte[] key) {
    this.key = new SecretKeySpec(key, "AES");
  }

  /** Returns null unless the value is base64 of exactly 32 bytes. */
  public static AesGcmWebhookSecrets fromBase64(String encoded) {
    if (encoded == null || encoded.isBlank()) return null;
    byte[] key;
    try {
      key = Base64.getDecoder().decode(encoded.strip());
    } catch (IllegalArgumentException malformed) {
      return null;
    }
    return key.length == KEY_BYTES ? new AesGcmWebhookSecrets(key) : null;
  }

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public byte[] encrypt(UUID endpointId, String secret) {
    var nonce = new byte[NONCE_BYTES];
    random.nextBytes(nonce);
    try {
      var cipher = cipher(Cipher.ENCRYPT_MODE, nonce, endpointId);
      var sealed = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
      return ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array();
    } catch (GeneralSecurityException error) {
      throw new IllegalStateException(error);
    }
  }

  @Override
  public String decrypt(UUID endpointId, byte[] ciphertext) {
    try {
      var nonce = java.util.Arrays.copyOfRange(ciphertext, 0, NONCE_BYTES);
      var cipher = cipher(Cipher.DECRYPT_MODE, nonce, endpointId);
      var opened = cipher.doFinal(ciphertext, NONCE_BYTES, ciphertext.length - NONCE_BYTES);
      return new String(opened, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | RuntimeException error) {
      throw new IllegalStateException("Webhook secret cannot be opened", error);
    }
  }

  private Cipher cipher(int mode, byte[] nonce, UUID endpointId) throws GeneralSecurityException {
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, nonce));
    cipher.updateAAD(endpointId.toString().getBytes(StandardCharsets.UTF_8));
    return cipher;
  }
}
