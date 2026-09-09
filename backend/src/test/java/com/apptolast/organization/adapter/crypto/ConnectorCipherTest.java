package com.apptolast.organization.adapter.crypto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s8 clave ausente, @s9 clave mal formada, @s29 clave rotada.
 */
class ConnectorCipherTest {
  static final String OWNER = "persona-a";
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void s8_anAbsentKeyLeavesTheConnectorsDisabled(String configured) {
    var cipher = ConnectorCipher.from(configured, "");
    assertFalse(ConnectorCipher.isConfigured(configured));
    assertTrue(cipher.decrypt(OWNER, new byte[64]).isEmpty());
    assertThrows(IllegalStateException.class, () -> cipher.encrypt(OWNER, URL));
  }

  @Test
  void s8_aNullKeyIsTreatedAsAbsent() {
    assertFalse(ConnectorCipher.isConfigured(null));
    assertTrue(ConnectorCipher.from(null, null).decrypt(OWNER, new byte[64]).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-es-base64-de-32-bytes", "c2hvcnQ=", "!!!!"})
  void s9_aMalformedKeyStopsTheStartupWithoutRevealingItsValue(String configured) {
    var thrown =
        assertThrows(IllegalStateException.class, () -> ConnectorCipher.from(configured, ""));
    assertTrue(thrown.getMessage().contains("APP_CONNECTOR_KEY"), thrown.getMessage());
    assertFalse(thrown.getMessage().contains(configured));
  }
}
