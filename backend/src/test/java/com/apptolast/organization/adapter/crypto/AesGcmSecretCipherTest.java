package com.apptolast.organization.adapter.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AesGcmSecretCipherTest {
  static final String OWNER = "persona-a";
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";
  static final String K1 = key((byte) 1);
  static final String K2 = key((byte) 2);

  static String key(byte filler) {
    var material = new byte[32];
    java.util.Arrays.fill(material, filler);
    return Base64.getEncoder().encodeToString(material);
  }

  @Test
  void s2_storesANonceOfTwelveBytesFollowedByCiphertextWithoutThePlainUrl() {
    var stored = AesGcmSecretCipher.of(K1).encrypt(OWNER, URL);
    assertEquals(12, AesGcmSecretCipher.NONCE_LENGTH);
    assertEquals(
        AesGcmSecretCipher.NONCE_LENGTH,
        AesGcmSecretCipher.HEADER_LENGTH,
        "el nonce abre el cifrado, sin cabecera propia");
    assertTrue(stored.length > AesGcmSecretCipher.HEADER_LENGTH);
    assertFalse(
        new String(stored, StandardCharsets.ISO_8859_1).contains("abc123"),
        "el cifrado no puede contener la URL en claro");
  }

  /** El contrato fija el formato: nonce y sellado, sin byte de versión de clave. */
  @Test
  void s2_theStoredBytesStartWithTheNonceItself() {
    var cipher = AesGcmSecretCipher.of(K1);
    var stored = cipher.encrypt(OWNER, URL);
    var rebuilt = new byte[stored.length];
    System.arraycopy(stored, 0, rebuilt, 0, stored.length);
    assertEquals(
        URL,
        cipher.decrypt(OWNER, rebuilt).orElseThrow(),
        "descifrar solo puede depender de los 12 primeros bytes como nonce");
    assertNotEquals(
        Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(stored, 12)),
        Base64.getEncoder()
            .encodeToString(java.util.Arrays.copyOf(cipher.encrypt(OWNER, URL), 12)));
  }

  @Test
  void s3_encryptingTwiceProducesDifferentCiphertextsThatDecryptToTheSameUrl() {
    var cipher = AesGcmSecretCipher.of(K1);
    var first = cipher.encrypt(OWNER, URL);
    var second = cipher.encrypt(OWNER, URL);
    assertNotEquals(
        Base64.getEncoder().encodeToString(first), Base64.getEncoder().encodeToString(second));
    assertNotEquals(
        Base64.getEncoder().encodeToString(nonce(first)),
        Base64.getEncoder().encodeToString(nonce(second)));
    assertEquals(URL, cipher.decrypt(OWNER, first).orElseThrow());
    assertEquals(URL, cipher.decrypt(OWNER, second).orElseThrow());
  }

  static byte[] nonce(byte[] stored) {
    return java.util.Arrays.copyOf(stored, AesGcmSecretCipher.NONCE_LENGTH);
  }

  @Test
  void b5_rotatingTheKeyStillReadsWhatThePreviousKeySealed() {
    var sealedWithK1 = AesGcmSecretCipher.of(K1).encrypt(OWNER, URL);
    var rotated = AesGcmSecretCipher.of(K2, K1);
    assertEquals(URL, rotated.decrypt(OWNER, sealedWithK1).orElseThrow());
    assertEquals(URL, rotated.decrypt(OWNER, rotated.encrypt(OWNER, URL)).orElseThrow());
  }

  @Test
  void b5_rotationSealsWithTheCurrentKeySoTheOldOneCanBeRetired() {
    var rotated = AesGcmSecretCipher.of(K2, K1);
    var resealed = rotated.encrypt(OWNER, URL);
    assertEquals(URL, AesGcmSecretCipher.of(K2).decrypt(OWNER, resealed).orElseThrow());
    assertTrue(AesGcmSecretCipher.of(K1).decrypt(OWNER, resealed).isEmpty());
  }

  @Test
  void b5_aThirdKeyIsStillUnreadableAfterRotation() {
    var sealedWithThird = AesGcmSecretCipher.of(key((byte) 3)).encrypt(OWNER, URL);
    assertTrue(AesGcmSecretCipher.of(K2, K1).decrypt(OWNER, sealedWithThird).isEmpty());
  }

  @Test
  void b5_anAbsentPreviousKeyIsSimplyIgnored() {
    var cipher = AesGcmSecretCipher.of(K1, null);
    assertEquals(URL, cipher.decrypt(OWNER, cipher.encrypt(OWNER, URL)).orElseThrow());
  }

  @Test
  void b5_aMalformedPreviousKeyAlsoStopsTheStartup() {
    var thrown =
        assertThrows(IllegalStateException.class, () -> AesGcmSecretCipher.of(K1, "no-es-base64"));
    assertTrue(thrown.getMessage().contains("APP_CONNECTOR_KEY_PREVIOUS"), thrown.getMessage());
  }

  @Test
  void s29_anotherKeyCannotDecrypt() {
    var stored = AesGcmSecretCipher.of(K1).encrypt(OWNER, URL);
    assertTrue(AesGcmSecretCipher.of(K2).decrypt(OWNER, stored).isEmpty());
  }

  @Test
  void s2_ownerIsAuthenticatedAdditionalData() {
    var cipher = AesGcmSecretCipher.of(K1);
    var stored = cipher.encrypt(OWNER, URL);
    assertTrue(cipher.decrypt("persona-b", stored).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 12, 13, 14, 28})
  void s29_truncatedOrForgedCiphertextIsUnreadable(int length) {
    var cipher = AesGcmSecretCipher.of(K1);
    var stored = cipher.encrypt(OWNER, URL);
    assertTrue(cipher.decrypt(OWNER, java.util.Arrays.copyOf(stored, length)).isEmpty());
  }

  @Test
  void s29_flippingOneBitOfTheCiphertextIsUnreadable() {
    var cipher = AesGcmSecretCipher.of(K1);
    var stored = cipher.encrypt(OWNER, URL);
    stored[stored.length - 1] ^= 1;
    assertTrue(cipher.decrypt(OWNER, stored).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-es-base64-de-32-bytes", "c2hvcnQ=", "!!!!", ""})
  void s9_aMalformedKeyStopsTheStartupWithoutRevealingItsValue(String configured) {
    var thrown = assertThrows(IllegalStateException.class, () -> AesGcmSecretCipher.of(configured));
    assertTrue(thrown.getMessage().contains("APP_CONNECTOR_KEY"), thrown.getMessage());
    assertFalse(thrown.getMessage().contains(configured) && !configured.isEmpty());
  }

  @Test
  void s9_aKeyOfThirtyOneBytesIsRejected() {
    var thrown =
        assertThrows(
            IllegalStateException.class,
            () -> AesGcmSecretCipher.of(Base64.getEncoder().encodeToString(new byte[31])));
    assertTrue(thrown.getMessage().contains("APP_CONNECTOR_KEY"));
  }
}
