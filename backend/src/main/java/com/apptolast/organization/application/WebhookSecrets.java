package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Encrypts webhook secrets at rest; unavailable when the connector key is missing.
 *
 * <p>Amendment B6: the associated data binds both the owner and the endpoint, so a row moved or
 * restored under another owner no longer opens.
 */
public interface WebhookSecrets {
  WebhookSecrets DISABLED =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return false;
        }

        @Override
        public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
          throw new IllegalStateException("Connectors are disabled");
        }

        @Override
        public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
          throw new IllegalStateException("Connectors are disabled");
        }
      };

  boolean available();

  byte[] encrypt(String ownerId, UUID endpointId, String secret);

  String decrypt(String ownerId, UUID endpointId, byte[] ciphertext);
}
