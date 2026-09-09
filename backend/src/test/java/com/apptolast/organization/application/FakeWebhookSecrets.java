package com.apptolast.organization.application;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Reversible stand-in for AES-GCM binding the same associated data as the real adapter (B6). */
class FakeWebhookSecrets implements WebhookSecrets {
  static final WebhookSecrets KEYED = new FakeWebhookSecrets();

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
    return prefix(ownerId, endpointId).concat(secret).getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
    var text = new String(ciphertext, StandardCharsets.UTF_8);
    var prefix = prefix(ownerId, endpointId);
    if (!text.startsWith(prefix)) throw new IllegalStateException("Wrong associated data");
    return text.substring(prefix.length());
  }

  private static String prefix(String ownerId, UUID endpointId) {
    return "cipher:" + ownerId + "|" + endpointId + ":";
  }
}
