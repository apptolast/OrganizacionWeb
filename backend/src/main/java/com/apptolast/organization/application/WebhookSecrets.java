package com.apptolast.organization.application;

import java.util.UUID;

/** Encrypts webhook secrets at rest; unavailable when the connector key is missing or invalid. */
public interface WebhookSecrets {
  WebhookSecrets DISABLED =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return false;
        }

        @Override
        public byte[] encrypt(UUID endpointId, String secret) {
          throw new IllegalStateException("Connectors are disabled");
        }

        @Override
        public String decrypt(UUID endpointId, byte[] ciphertext) {
          throw new IllegalStateException("Connectors are disabled");
        }
      };

  boolean available();

  byte[] encrypt(UUID endpointId, String secret);

  String decrypt(UUID endpointId, byte[] ciphertext);
}
