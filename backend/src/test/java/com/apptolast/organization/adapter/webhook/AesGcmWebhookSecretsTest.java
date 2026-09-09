package com.apptolast.organization.adapter.webhook;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.WebhookSecrets;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AesGcmWebhookSecretsTest {
  private static final String KEY_A = key((byte) 1);
  private static final String KEY_B = key((byte) 2);
  private static final String KEY_C = key((byte) 3);
  private static final String OWNER = "owner-a";
  private static final UUID ID = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
  private static final UUID OTHER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";

  @Test
  void s8_secretIsEncryptedAndNeverStoredInClear() {
    WebhookSecrets secrets = AesGcmWebhookSecrets.from(KEY_A, null);
    assertTrue(secrets.available());
    var ciphertext = secrets.encrypt(OWNER, ID, SECRET);
    assertTrue(ciphertext.length >= 60, "version(1) + nonce(12) + text + tag(16)");
    assertEquals(-1, indexOf(ciphertext, SECRET.getBytes(StandardCharsets.UTF_8)));
    assertEquals(-1, indexOf(ciphertext, "whsec_".getBytes(StandardCharsets.UTF_8)));
    assertFalse(Arrays.equals(ciphertext, secrets.encrypt(OWNER, ID, SECRET)), "fresh nonce");
    assertEquals(SECRET, secrets.decrypt(OWNER, ID, ciphertext));
  }

  @Test
  void s8_b6_theAssociatedDataBindsBothTheOwnerAndTheEndpoint() {
    var secrets = AesGcmWebhookSecrets.from(KEY_A, null);
    var ciphertext = secrets.encrypt(OWNER, ID, SECRET);
    assertThrows(IllegalStateException.class, () -> secrets.decrypt(OWNER, OTHER_ID, ciphertext));
    assertThrows(IllegalStateException.class, () -> secrets.decrypt("owner-b", ID, ciphertext));
    assertEquals(SECRET, secrets.decrypt(OWNER, ID, ciphertext));
  }

  @Test
  void s8_b6_anOwnerThatContainsTheSeparatorIsStillDistinguished() {
    var secrets = AesGcmWebhookSecrets.from(KEY_A, null);
    var ciphertext = secrets.encrypt("a|b", ID, SECRET);
    assertThrows(IllegalStateException.class, () -> secrets.decrypt("a", ID, ciphertext));
    assertEquals(SECRET, secrets.decrypt("a|b", ID, ciphertext));
  }

  @Test
  void b5_theCiphertextOpensWithAKeyVersionByteThatIsStablePerKey() {
    var first = AesGcmWebhookSecrets.from(KEY_A, null).encrypt(OWNER, ID, SECRET);
    var second = AesGcmWebhookSecrets.from(KEY_A, null).encrypt(OWNER, ID, SECRET);
    assertEquals(first[0], second[0], "same key, same version byte");
    var underB = AesGcmWebhookSecrets.from(KEY_B, null).encrypt(OWNER, ID, SECRET);
    assertNotEquals(first[0], underB[0], "different key, different version byte");
  }

  @Test
  void b5_rotatingTheKeyStillOpensSecretsSealedWithThePreviousOne() {
    var sealedWithA = AesGcmWebhookSecrets.from(KEY_A, null).encrypt(OWNER, ID, SECRET);
    var rotated = AesGcmWebhookSecrets.from(KEY_B, KEY_A);
    assertEquals(SECRET, rotated.decrypt(OWNER, ID, sealedWithA));
    var sealedWithB = rotated.encrypt(OWNER, ID, SECRET);
    assertEquals(SECRET, rotated.decrypt(OWNER, ID, sealedWithB));
    assertEquals(
        sealedWithB[0], AesGcmWebhookSecrets.from(KEY_B, null).encrypt(OWNER, ID, SECRET)[0]);
  }

  @Test
  void b5_aKeyThatIsNeitherCurrentNorPreviousCannotOpen() {
    var sealedWithC = AesGcmWebhookSecrets.from(KEY_C, null).encrypt(OWNER, ID, SECRET);
    var rotated = AesGcmWebhookSecrets.from(KEY_B, KEY_A);
    assertThrows(IllegalStateException.class, () -> rotated.decrypt(OWNER, ID, sealedWithC));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void s9_b5_anAbsentKeyLeavesConnectorsDisabled(String absent) {
    assertNull(AesGcmWebhookSecrets.from(absent, null));
    assertNull(AesGcmWebhookSecrets.from(null, null));
    assertNull(AesGcmWebhookSecrets.from(null, KEY_A), "no current key means disabled");
  }

  @ParameterizedTest
  @ValueSource(strings = {"not base64!", "AAAA"})
  void s9_b5_aMalformedCurrentKeyFailsFastInsteadOfDegrading(String malformed) {
    assertThrows(IllegalStateException.class, () -> AesGcmWebhookSecrets.from(malformed, null));
  }

  @Test
  void s9_b5_keysOfTheWrongLengthFailFastAndAMalformedPreviousKeyToo() {
    assertThrows(
        IllegalStateException.class, () -> AesGcmWebhookSecrets.from(base64(new byte[31]), null));
    assertThrows(
        IllegalStateException.class, () -> AesGcmWebhookSecrets.from(base64(new byte[33]), null));
    assertThrows(
        IllegalStateException.class, () -> AesGcmWebhookSecrets.from(KEY_A, "not base64!"));
    assertNotNull(AesGcmWebhookSecrets.from(KEY_A, "   "), "a blank previous key is simply absent");
  }

  @Test
  void s9_theDisabledSecretsRefuseEveryOperation() {
    WebhookSecrets disabled = WebhookSecrets.DISABLED;
    assertFalse(disabled.available());
    assertThrows(IllegalStateException.class, () -> disabled.encrypt(OWNER, ID, "x"));
    assertThrows(IllegalStateException.class, () -> disabled.decrypt(OWNER, ID, new byte[60]));
  }

  private static String key(byte fill) {
    var bytes = new byte[32];
    Arrays.fill(bytes, fill);
    return base64(bytes);
  }

  private static String base64(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
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
