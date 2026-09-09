package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.adapter.feed.HttpCalendarFeed;
import com.apptolast.organization.application.AddressPolicy;
import com.apptolast.organization.application.OutboundGuard;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/** Cableado de la feature 28: @s9 el arranque, @s11 la guardia y @s13 el plazo de descarga. */
class ExternalCalendarWiringTest {
  static final ApplicationConfiguration CONFIGURATION = new ApplicationConfiguration();
  static final ConnectorConfiguration CONNECTORS = new ConnectorConfiguration();
  static final String OWNER = "persona-a";
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";
  static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

  static InetAddress address(String literal) {
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  /**
   * El cifrado es ahora el puerto único de conectores, que provee la feature 27. Este carril solo
   * comprueba que sigue cumpliendo lo que necesita: @s9 el arranque y @s2 la ida y vuelta.
   */
  @Test
  void s9_aMalformedKeyStopsTheStartupWithoutRevealingItsValue() {
    var thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> CONNECTORS.secretCipher("no-es-base64-de-32-bytes", ""));
    assertTrue(thrown.getMessage().contains("APP_CONNECTOR_KEY"), thrown.getMessage());
    assertFalse(thrown.getMessage().contains("no-es-base64-de-32-bytes"));
  }

  @Test
  void s2_aConfiguredKeyRoundTripsTheAddress() {
    var cipher = CONNECTORS.secretCipher(KEY, "");
    assertTrue(cipher.enabled());
    assertEquals(URL, cipher.decrypt(OWNER, cipher.encrypt(OWNER, URL)).orElseThrow());
  }

  @Test
  void s2_theStoredFormatIsTwelveBytesOfNonceFollowedByTheSeal() {
    var cipher = CONNECTORS.secretCipher(KEY, "");
    var stored = cipher.encrypt(OWNER, URL);
    assertEquals(
        12 + URL.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 16, stored.length);
    assertFalse(
        new String(stored, java.nio.charset.StandardCharsets.ISO_8859_1).contains("abc123"));
  }

  @Test
  void s8_anAbsentKeyLeavesTheConnectorsDisabled() {
    assertFalse(CONNECTORS.secretCipher("", "").enabled());
  }

  @Test
  void b10_theDefaultPolicyBlocksPrivateAddresses() {
    var policy = CONFIGURATION.connectorAddressPolicy(false);
    assertFalse(policy.allows(address("127.0.0.1")));
    assertFalse(policy.allows(address("169.254.169.254")));
    assertTrue(policy.allows(address("93.184.216.34")));
  }

  @Test
  void b10_thePolicyIsOnlyRelaxedOnPurpose() {
    assertTrue(CONFIGURATION.connectorAddressPolicy(true).allows(address("127.0.0.1")));
  }

  @Test
  void s11_theGuardCombinesTheResolverAndThePolicy() {
    var guard =
        CONFIGURATION.outboundGuard(
            host -> java.util.List.of(address("10.0.0.5")),
            AddressPolicy.blockingPrivateAddresses());
    assertEquals(OutboundGuard.Verdict.BLOCKED, guard.check("interno.example.test"));
  }

  @Test
  void s4_theSystemResolverAnswersEmptyForAHostThatDoesNotExist() {
    assertTrue(CONFIGURATION.systemHostResolver().resolve("no-existe.invalid").isEmpty());
  }

  @Test
  void s4_theSystemResolverResolvesLoopback() {
    assertFalse(CONFIGURATION.systemHostResolver().resolve("localhost").isEmpty());
  }

  @Test
  void s13_theFeedIsTheHttpOneWithTheContractTimeout() {
    assertInstanceOf(HttpCalendarFeed.class, CONFIGURATION.calendarFeed());
    assertEquals(java.time.Duration.ofSeconds(5), HttpCalendarFeed.TIMEOUT);
    assertEquals(1024 * 1024, HttpCalendarFeed.LIMIT);
  }

  @Test
  void s12_theAuditIsWired() {
    assertInstanceOf(
        com.apptolast.organization.adapter.logging.Slf4jExternalCalendarAudit.class,
        CONFIGURATION.externalCalendarAudit());
  }
}
