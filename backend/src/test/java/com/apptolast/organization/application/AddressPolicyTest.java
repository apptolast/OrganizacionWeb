package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Enmienda B2 de la revisión de seguridad: una única política de egreso compartida por los
 * conectores (features 25, 27 y 28).
 *
 * <p>Este fichero es la unión de las dos baterías que traían las ramas de las features 25 y 28, que
 * llegaron con el mismo nombre y conductas contrarias para las formas IPv6 que encapsulan una IPv4.
 * Al integrar se eligió la conducta más restrictiva, la de la 28; los casos de la 25 que la
 * contradecían (6to4 y NAT64 con IPv4 pública embebida, que ella permitía) están reescritos y viven
 * ahora en {@link #b2_sixToFourAndNat64AreBlockedWholeEvenWithAPublicEmbeddedIpv4()}.
 */
class AddressPolicyTest {
  final AddressPolicy policy = AddressPolicy.blockingPrivateAddresses();

  @ParameterizedTest
  @CsvSource({
    "127.0.0.1,false",
    "127.255.255.254,false",
    "10.0.0.5,false",
    "10.1.2.3,false",
    "172.16.0.1,false",
    "172.16.0.9,false",
    "172.31.255.254,false",
    "172.31.255.255,false",
    "192.168.1.1,false",
    "169.254.169.254,false",
    "0.0.0.0,false",
    "0.1.2.3,false",
    "0.255.255.255,false",
    "100.64.0.1,false",
    "100.127.255.254,false",
    "100.127.255.255,false",
    "192.0.0.1,false",
    "192.0.0.255,false",
    "192.88.99.1,false",
    "198.18.0.1,false",
    "198.19.255.254,false",
    "198.19.255.255,false",
    "224.0.0.1,false",
    "239.255.255.255,false",
    "240.0.0.1,false",
    "255.255.255.255,false",
    "93.184.216.34,true",
    "8.8.8.8,true",
    "1.0.0.1,true",
    "203.0.113.5,true",
    "172.15.255.255,true",
    "172.32.0.1,true",
    "100.63.255.255,true",
    "100.128.0.1,true",
    "192.0.1.1,true",
    "192.88.100.1,true",
    "198.17.255.255,true",
    "198.20.0.1,true",
    "223.255.255.255,true"
  })
  void s4_s11_appliesTheIpv4CidrList(String literal, boolean allowed) throws Exception {
    assertEquals(allowed, policy.allows(InetAddress.getByName(literal)), literal);
  }

  @ParameterizedTest
  @CsvSource({
    "::1,false",
    "::,false",
    "fe80::1,false",
    "febf::1,false",
    "fc00::1,false",
    "fd12::1,false",
    "fdff::1,false",
    "ff02::1,false",
    "2606:2800:220:1:248:1893:25c8:1946,true",
    "2001:4860:4860::8888,true",
    "2001:db8::1,true"
  })
  void s4_s11_appliesTheIpv6CidrList(String literal, boolean allowed) throws Exception {
    assertEquals(allowed, policy.allows(InetAddress.getByName(literal)), literal);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "::ffff:10.0.0.1",
        "::ffff:127.0.0.1",
        "::ffff:169.254.169.254",
        "::ffff:169.254.1.1",
        "::ffff:198.18.0.1",
        "::93.184.216.34",
        "::10.0.0.1",
        "::a00:1",
        "2002:0a00:0001::1",
        "2002:7f00:0001::1",
        "2002:c0a8:0101::1",
        "2002:5db8:d822::1",
        "64:ff9b::10.0.0.1",
        "64:ff9b::127.0.0.1",
        "64:ff9b::5db8:d822"
      })
  void s4_s11_blocksEncapsulatedIpv4Forms(String literal) throws Exception {
    assertFalse(policy.allows(InetAddress.getByName(literal)), literal);
  }

  @Test
  void s4_normalizesMappedPublicAddressesInsteadOfBlockingThemWholesale() throws Exception {
    assertTrue(policy.allows(InetAddress.getByName("::ffff:93.184.216.34")));
    assertTrue(policy.allows(InetAddress.getByName("::ffff:8.8.8.8")));
    assertTrue(policy.allows(InetAddress.getByName("::ffff:203.0.113.5")));
  }

  /**
   * La decisión de seguridad que zanja la discrepancia entre las dos ramas, escrita como contrato y
   * no dejada al azar de la implementación que sobreviviera: 6to4 y NAT64 se prohíben por rango
   * entero, incluso cuando la IPv4 que encapsulan es pública y sería alcanzable en su forma normal.
   * Solo la forma mapeada se normaliza.
   */
  @Test
  void b2_sixToFourAndNat64AreBlockedWholeEvenWithAPublicEmbeddedIpv4() throws Exception {
    assertTrue(
        policy.allows(InetAddress.getByName("203.0.113.5")),
        "la IPv4 embebida, en su forma normal, sí es alcanzable");
    assertFalse(
        policy.allows(InetAddress.getByName("2002:cb00:7105::1")),
        "6to4 de 203.0.113.5: prohibida por pertenecer a 2002::/16");
    assertFalse(
        policy.allows(InetAddress.getByName("64:ff9b::203.0.113.5")),
        "NAT64 de 203.0.113.5: prohibida por pertenecer a 64:ff9b::/96");
    assertFalse(
        policy.allows(InetAddress.getByName("::203.0.113.5")),
        "compatible de 203.0.113.5: prohibida por pertenecer a ::/96");
  }

  @Test
  void b10_theEndToEndProfileMayAllowPrivateAddressesExplicitly() throws Exception {
    var permissive = AddressPolicy.allowingPrivateAddresses();
    assertTrue(permissive.allows(InetAddress.getByName("127.0.0.1")));
    assertTrue(permissive.allows(InetAddress.getByName("10.0.0.5")));
  }
}
