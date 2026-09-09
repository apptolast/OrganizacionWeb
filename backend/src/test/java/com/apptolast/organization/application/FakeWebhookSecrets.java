package com.apptolast.organization.application;

import java.util.UUID;

/** Reversible stand-in for AES-GCM: the endpoint id acts as the additional authenticated data. */
class FakeWebhookSecrets implements WebhookSecrets {
  static final WebhookSecrets KEYED = new FakeWebhookSecrets();

  @Override
  public boolean available() {
    return true;
  }

  @Override
  public byte[] encrypt(UUID endpointId, String secret) {
    return prefix(endpointId).concat(secret).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Override
  public String decrypt(UUID endpointId, byte[] ciphertext) {
    var text = new String(ciphertext, java.nio.charset.StandardCharsets.UTF_8);
    if (!text.startsWith(prefix(endpointId))) throw new IllegalStateException("Wrong endpoint id");
    return text.substring(prefix(endpointId).length());
  }

  private static String prefix(UUID endpointId) {
    return "cipher:" + endpointId + ":";
  }
}
