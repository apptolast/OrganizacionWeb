package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s4 rechaza en el PUT los host que no resuelven o que resuelven a una dirección prohibida; @s11
 *     los rechaza otra vez en la sincronización, porque el DNS puede cambiar entre ambos momentos.
 */
class OutboundHostGuardTest {
  static InetAddress address(String literal) {
    try {
      return InetAddress.getByName(literal);
    } catch (UnknownHostException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  static OutboundHostGuard guardResolving(Map<String, List<String>> zone) {
    HostResolver resolver =
        host -> zone.getOrDefault(host, List.of()).stream().map(OutboundHostGuardTest::address).toList();
    return new OutboundHostGuard(resolver, AddressPolicy.blockingPrivateAddresses());
  }

  @Test
  void s4_aHostThatResolvesOnlyToPublicAddressesIsAllowed() {
    var guard = guardResolving(Map.of("feed.example.test", List.of("93.184.216.34")));
    assertEquals(OutboundHostGuard.Verdict.ALLOWED, guard.check("feed.example.test"));
  }

  @Test
  void s4_aHostWithNoAddressesIsUnresolvable() {
    assertEquals(OutboundHostGuard.Verdict.UNRESOLVABLE, guardResolving(Map.of()).check("no-existe.invalid"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "127.0.0.1",
        "10.0.0.5",
        "172.16.0.1",
        "172.31.255.254",
        "192.168.1.1",
        "169.254.169.254",
        "0.0.0.0",
        "224.0.0.1",
        "::1",
        "fe80::1",
        "fc00::1",
        "::ffff:10.0.0.1"
      })
  void s11_aHostThatResolvesToAForbiddenAddressIsBlocked(String literal) {
    var guard = guardResolving(Map.of("interno.example.test", List.of(literal)));
    assertEquals(OutboundHostGuard.Verdict.BLOCKED, guard.check("interno.example.test"));
  }

  @Test
  void s4_oneForbiddenAddressAmongPublicOnesBlocksTheWholeHost() {
    var guard =
        guardResolving(Map.of("dual.example.test", List.of("93.184.216.34", "::1")));
    assertEquals(OutboundHostGuard.Verdict.BLOCKED, guard.check("dual.example.test"));
  }

  @Test
  void s11_aHostThatStopsResolvingIsRejectedToo() {
    assertEquals(OutboundHostGuard.Verdict.UNRESOLVABLE, guardResolving(Map.of()).check("feed.example.test"));
  }

  @Test
  void s11_aResolverFailureIsTreatedAsUnresolvableAndNeverAsAllowed() {
    HostResolver failing =
        host -> {
          throw new IllegalStateException("el DNS no responde");
        };
    var guard = new OutboundHostGuard(failing, AddressPolicy.blockingPrivateAddresses());
    assertEquals(OutboundHostGuard.Verdict.UNRESOLVABLE, guard.check("feed.example.test"));
  }

  @Test
  void b10_theOverridingPolicyAllowsLoopbackForEndToEndTestsOnly() {
    var guard =
        new OutboundHostGuard(
            host -> List.of(address("127.0.0.1")), AddressPolicy.allowingPrivateAddresses());
    assertEquals(OutboundHostGuard.Verdict.ALLOWED, guard.check("localhost"));
  }
}
