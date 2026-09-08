package com.apptolast.organization.adapter.webhook;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.WebhookSecrets;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AesGcmWebhookSecretsTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  @Test
  void s8_secretIsEncryptedWithTheEndpointIdAsAssociatedDataAndNeverStoredInClear() {
    WebhookSecrets secrets = AesGcmWebhookSecrets.fromBase64(KEY);
    assertTrue(secrets.available());
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var secret = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";
    var ciphertext = secrets.encrypt(id, secret);
    assertTrue(ciphertext.length >= 60, "nonce(12) + text + tag(16)");
    assertEquals(-1, indexOf(ciphertext, secret.getBytes(StandardCharsets.UTF_8)));
    assertEquals(-1, indexOf(ciphertext, "whsec_".getBytes(StandardCharsets.UTF_8)));
    assertFalse(java.util.Arrays.equals(ciphertext, secrets.encrypt(id, secret)), "fresh nonce");
    assertEquals(secret, secrets.decrypt(id, ciphertext));
    assertThrows(
        IllegalStateException.class,
        () -> secrets.decrypt(UUID.fromString("00000000-0000-4000-8000-000000000001"), ciphertext));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "not base64!", "AAAA"})
  void s9_missingOrMalformedKeyLeavesConnectorsDisabled(String key) {
    assertNull(AesGcmWebhookSecrets.fromBase64(key));
    assertNull(AesGcmWebhookSecrets.fromBase64(null));
  }

  @Test
  void s9_keyOfThirtyOneBytesIsRejectedAndThirtyTwoAccepted() {
    assertNull(AesGcmWebhookSecrets.fromBase64(Base64.getEncoder().encodeToString(new byte[31])));
    assertNull(AesGcmWebhookSecrets.fromBase64(Base64.getEncoder().encodeToString(new byte[33])));
    assertNotNull(AesGcmWebhookSecrets.fromBase64(KEY));
    WebhookSecrets disabled = WebhookSecrets.DISABLED;
    assertFalse(disabled.available());
    assertThrows(IllegalStateException.class, () -> disabled.encrypt(UUID.randomUUID(), "x"));
    assertThrows(
        IllegalStateException.class, () -> disabled.decrypt(UUID.randomUUID(), new byte[60]));
  }

  private static int indexOf(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i + needle.length <= haystack.length; i++) {
      for (int j = 0; j < needle.length; j++) if (haystack[i + j] != needle[j]) continue outer;
      return i;
    }
    return -1;
  }
}
