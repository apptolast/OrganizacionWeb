package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.WebhookAudit;
import com.apptolast.organization.application.WebhookSecrets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookConnectorStartupTest {
  private final List<String> audited = new ArrayList<>();

  private final WebhookAudit audit =
      new WebhookAudit() {
        @Override
        public void attempt(UUID endpointId, UUID eventId, String status, String errorClass) {}

        @Override
        public void discarded(UUID endpointId, UUID eventId, String code) {}

        @Override
        public void workerError(String code) {
          audited.add(code);
        }
      };

  private static final WebhookSecrets KEYED =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return true;
        }

        @Override
        public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
          return new byte[0];
        }

        @Override
        public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
          return "";
        }
      };

  @Test
  void s9_anAbsentKeyIsAuditedOnceAtStartupAndNotOncePerCycle() {
    var startup = new WebhookConnectorStartup(WebhookSecrets.DISABLED, audit);

    startup.report();
    startup.report();

    assertEquals(List.of("CONFIGURATION_ERROR"), audited, "exactly once, however often it runs");
  }

  @Test
  void s9_aPresentKeyAuditsNothing() {
    new WebhookConnectorStartup(KEYED, audit).report();

    assertTrue(audited.isEmpty());
  }
}
