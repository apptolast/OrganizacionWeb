package com.apptolast.organization.adapter.connectors;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.ConnectorsDisabledException;
import com.apptolast.organization.application.SecretCipher;
import com.apptolast.organization.application.SecretUndecipherableException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AesGcmSecretCipherTest {
  private static final String TOKEN = "ghp_secreto123";
  private static final String OWNER = "owner-a";
  private static final String KEY = key((byte) 7);
  private static final String OTHER_KEY = key((byte) 19);

  private static String key(byte fill) {
    var bytes = new byte[32];
    Arrays.fill(bytes, fill);
    return Base64.getEncoder().encodeToString(bytes);
  }

  private static SecretCipher cipher(String current, String previous) {
    return new AesGcmSecretCipher(ConnectorKeyRing.of(current, previous), new SecureRandom());
  }

  @Test
  void s1_ciphertextCarriesKeyVersionNonceAndTagAndHidesTheToken() {
    var sealed = cipher(KEY, null).encrypt(OWNER, TOKEN);
    assertEquals(1 + 12 + TOKEN.length() + 16, sealed.length);
    assertFalse(
        new String(sealed, StandardCharsets.ISO_8859_1)
            .contains(new String(TOKEN.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)));
    assertEquals(TOKEN, cipher(KEY, null).decrypt(OWNER, sealed));
  }

  @Test
  void s2_everyWriteUsesAFreshNonceAndKeepsTheKeyVersionStable() {
    var cipher = cipher(KEY, null);
    var first = cipher.encrypt(OWNER, TOKEN);
    var second = cipher.encrypt(OWNER, TOKEN);
    assertFalse(Arrays.equals(first, second));
    assertFalse(Arrays.equals(nonce(first), nonce(second)));
    assertEquals(first[0], second[0]);
  }

  private static byte[] nonce(byte[] sealed) {
    return Arrays.copyOfRange(sealed, 1, 13);
  }

  @Test
  void s2_anotherOwnerCannotReadTheCiphertextBecauseTheOwnerIsAuthenticatedData() {
    var cipher = cipher(KEY, null);
    var sealed = cipher.encrypt(OWNER, TOKEN);
    assertThrows(SecretUndecipherableException.class, () -> cipher.decrypt("owner-b", sealed));
  }

  @Test
  void s2_anotherKeyCannotReadTheCiphertext() {
    var sealed = cipher(KEY, null).encrypt(OWNER, TOKEN);
    var stranger = cipher(OTHER_KEY, null);
    assertThrows(SecretUndecipherableException.class, () -> stranger.decrypt(OWNER, sealed));
  }

  @Test
  void b5_rotatingTheKeyKeepsStoredTokensReadableAndNewWritesUseTheNewKey() {
    var before = cipher(KEY, null).encrypt(OWNER, TOKEN);
    var rotated = cipher(OTHER_KEY, KEY);
    assertEquals(TOKEN, rotated.decrypt(OWNER, before));
    var after = rotated.encrypt(OWNER, TOKEN);
    assertEquals(TOKEN, rotated.decrypt(OWNER, after));
    assertEquals(TOKEN, cipher(OTHER_KEY, null).decrypt(OWNER, after));
    assertThrows(
        SecretUndecipherableException.class, () -> cipher(OTHER_KEY, null).decrypt(OWNER, before));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 12, 13, 28, 29})
  void b5_aTruncatedOrEmptyCiphertextIsRejectedInsteadOfParsedOutOfBounds(int length) {
    var cipher = cipher(KEY, null);
    var truncated = new byte[length];
    assertThrows(SecretUndecipherableException.class, () -> cipher.decrypt(OWNER, truncated));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "no-es-base64!!", "AAAAAAAAAAAAAAAAAAAAAA==", "clave-de-32-mas-uno"})
  void s4_aMalformedKeyStopsTheStartupWithoutRevealingItsValue(String raw) {
    var value = raw.equals("clave-de-32-mas-uno") ? key((byte) 3) + "=" : raw;
    var error = assertThrows(IllegalArgumentException.class, () -> ConnectorKeyRing.of(value, null));
    assertTrue(error.getMessage().contains("app.connectors.key"));
    if (!value.isEmpty()) assertFalse(error.getMessage().contains(value));
  }

  @Test
  void s4_aMalformedPreviousKeyAlsoStopsTheStartup() {
    var error =
        assertThrows(IllegalArgumentException.class, () -> ConnectorKeyRing.of(KEY, "no-base64!!"));
    assertTrue(error.getMessage().contains("app.connectors.key-previous"));
  }

  @Test
  void s4_aThirtyThreeByteKeyIsRejected() {
    var bytes = new byte[33];
    Arrays.fill(bytes, (byte) 5);
    assertThrows(
        IllegalArgumentException.class,
        () -> ConnectorKeyRing.of(Base64.getEncoder().encodeToString(bytes), null));
  }

  @Test
  void s3_anAbsentKeyDisablesTheConnectorInsteadOfStoppingTheStartup() {
    var disabled = new AesGcmSecretCipher(ConnectorKeyRing.of(null, null), new SecureRandom());
    assertThrows(ConnectorsDisabledException.class, () -> disabled.encrypt(OWNER, TOKEN));
    assertThrows(ConnectorsDisabledException.class, () -> disabled.decrypt(OWNER, new byte[43]));
  }
}
