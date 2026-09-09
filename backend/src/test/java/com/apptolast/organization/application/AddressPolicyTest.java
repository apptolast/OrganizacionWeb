package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Enmienda B2: política de egreso compartida por las features 25, 27 y 28. */
class AddressPolicyTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "127.0.0.1",
        "127.255.255.254",
        "0.0.0.0",
        "0.1.2.3",
        "0.255.255.255",
        "10.1.2.3",
        "172.16.0.9",
        "172.31.255.255",
        "192.168.1.1",
        "100.64.0.1",
        "100.127.255.255",
        "169.254.169.254",
        "224.0.0.1",
        "239.255.255.255",
        "192.0.0.1",
        "192.0.0.255",
        "192.88.99.1",
        "198.18.0.1",
        "198.19.255.255",
        "240.0.0.1",
        "255.255.255.255",
        "::1",
        "::",
        "fe80::1",
        "fc00::1",
        "fd12::1",
        "ff02::1"
      })
  void s5_b2_reservedAndPrivateLiteralsAreBlocked(String literal) throws UnknownHostException {
    assertTrue(AddressPolicy.isBlocked(InetAddress.getByName(literal)), literal);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "::ffff:10.0.0.1",
        "::ffff:127.0.0.1",
        "::ffff:169.254.1.1",
        "::ffff:198.18.0.1",
        "::10.0.0.1",
        "2002:0a00:0001::1",
        "2002:7f00:0001::1",
        "2002:c0a8:0101::1",
        "64:ff9b::10.0.0.1",
        "64:ff9b::127.0.0.1"
      })
  void s5_b2_embeddedIpv4FormsAreNormalisedBeforeJudging(String literal)
      throws UnknownHostException {
    assertTrue(AddressPolicy.isBlocked(InetAddress.getByName(literal)), literal);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "203.0.113.5",
        "8.8.8.8",
        "172.15.255.255",
        "172.32.0.1",
        "100.63.255.255",
        "100.128.0.1",
        "192.0.1.1",
        "192.88.100.1",
        "198.17.255.255",
        "198.20.0.1",
        "2001:db8::1",
        "::ffff:203.0.113.5",
        "2002:cb00:7105::1",
        "64:ff9b::203.0.113.5"
      })
  void s5_b2_publicLiteralsAreAllowed(String literal) throws UnknownHostException {
    assertFalse(AddressPolicy.isBlocked(InetAddress.getByName(literal)), literal);
  }
}
