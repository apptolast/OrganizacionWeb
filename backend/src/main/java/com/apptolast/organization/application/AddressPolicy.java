package com.apptolast.organization.application;

import java.net.InetAddress;

/**
 * Egress policy shared by every outbound connector (features 25, 27 and 28): the destinations a
 * request must never reach. Amendment B2 of the security review.
 *
 * <p>IPv6 forms that embed an IPv4 address (mapped, compatible, 6to4 and NAT64) are normalised to
 * that address first, so an internal destination cannot be smuggled through the longer notation.
 */
public final class AddressPolicy {
  private static final int IPV4_BYTES = 4;
  private static final int SIXTO4_EMBEDDED_OFFSET = 2;
  private static final int TRAILING_EMBEDDED_OFFSET = 12;

  private AddressPolicy() {}

  public static boolean isBlocked(InetAddress address) {
    return isBlocked(address.getAddress());
  }

  private static boolean isBlocked(byte[] address) {
    if (address.length == IPV4_BYTES) return blockedIpv4(address);
    var embedded = embeddedIpv4(address);
    return embedded == null ? blockedIpv6(address) : blockedIpv4(embedded);
  }

  private static byte[] embeddedIpv4(byte[] address) {
    if (ipv4Mapped(address) || ipv4Compatible(address))
      return slice(address, TRAILING_EMBEDDED_OFFSET);
    if (nat64(address)) return slice(address, TRAILING_EMBEDDED_OFFSET);
    if (sixToFour(address)) return slice(address, SIXTO4_EMBEDDED_OFFSET);
    return null;
  }

  /** ::ffff:a.b.c.d */
  private static boolean ipv4Mapped(byte[] address) {
    return zeroes(address, 0, 10) && byteAt(address, 10) == 0xff && byteAt(address, 11) == 0xff;
  }

  /** ::a.b.c.d */
  private static boolean ipv4Compatible(byte[] address) {
    return zeroes(address, 0, 12);
  }

  /** 64:ff9b::/96 */
  private static boolean nat64(byte[] address) {
    return byteAt(address, 0) == 0x00
        && byteAt(address, 1) == 0x64
        && byteAt(address, 2) == 0xff
        && byteAt(address, 3) == 0x9b
        && zeroes(address, 4, 12);
  }

  /** 2002::/16 */
  private static boolean sixToFour(byte[] address) {
    return byteAt(address, 0) == 0x20 && byteAt(address, 1) == 0x02;
  }

  private static boolean blockedIpv4(byte[] address) {
    int first = byteAt(address, 0);
    int second = byteAt(address, 1);
    int third = byteAt(address, 2);
    return first == 0 // 0.0.0.0/8
        || first == 10 // 10.0.0.0/8
        || first == 127 // 127.0.0.0/8
        || first == 172 && (second & 0xf0) == 16 // 172.16.0.0/12
        || first == 192 && second == 168 // 192.168.0.0/16
        || first == 100 && (second & 0xc0) == 64 // 100.64.0.0/10
        || first == 169 && second == 254 // 169.254.0.0/16
        || first == 192 && second == 0 && third == 0 // 192.0.0.0/24
        || first == 192 && second == 88 && third == 99 // 192.88.99.0/24
        || first == 198 && (second & 0xfe) == 18 // 198.18.0.0/15
        || (first & 0xf0) == 224 // 224.0.0.0/4
        || (first & 0xf0) == 240; // 240.0.0.0/4, incluye 255.255.255.255
  }

  private static boolean blockedIpv6(byte[] address) {
    int first = byteAt(address, 0);
    return (first & 0xfe) == 0xfc // fc00::/7
        || first == 0xfe && (byteAt(address, 1) & 0xc0) == 0x80 // fe80::/10
        || first == 0xff; // ff00::/8
  }

  private static byte[] slice(byte[] address, int offset) {
    var embedded = new byte[IPV4_BYTES];
    System.arraycopy(address, offset, embedded, 0, IPV4_BYTES);
    return embedded;
  }

  private static boolean zeroes(byte[] address, int from, int toExclusive) {
    for (var index = from; index < toExclusive; index++) if (address[index] != 0) return false;
    return true;
  }

  private static int byteAt(byte[] address, int index) {
    return address[index] & 0xff;
  }
}
