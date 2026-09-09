package com.apptolast.organization.application;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

/**
 * Lista CIDR explícita de direcciones a las que nunca se abre una conexión saliente, con
 * normalización previa de las formas IPv4 mapeada, compatible, 6to4 y NAT64.
 *
 * <p>No basta con los predicados de {@link InetAddress}: {@code isSiteLocalAddress} solo cubre
 * {@code fec0::/10} en IPv6, y ni 6to4 ni NAT64 tienen predicado alguno.
 */
final class PublicAddressPolicy implements AddressPolicy {
  private static final List<Cidr> BLOCKED_IPV4 =
      List.of(
          Cidr.of("0.0.0.0", 8),
          Cidr.of("10.0.0.0", 8),
          Cidr.of("100.64.0.0", 10),
          Cidr.of("127.0.0.0", 8),
          Cidr.of("169.254.0.0", 16),
          Cidr.of("172.16.0.0", 12),
          Cidr.of("192.0.0.0", 24),
          Cidr.of("192.88.99.0", 24),
          Cidr.of("192.168.0.0", 16),
          Cidr.of("198.18.0.0", 15),
          Cidr.of("224.0.0.0", 4),
          Cidr.of("240.0.0.0", 4));
  private static final List<Cidr> BLOCKED_IPV6 =
      List.of(
          Cidr.of("::", 96),
          Cidr.of("64:ff9b::", 96),
          Cidr.of("2002::", 16),
          Cidr.of("fc00::", 7),
          Cidr.of("fe80::", 10),
          Cidr.of("ff00::", 8));
  private static final byte[] MAPPED_PREFIX = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1};

  @Override
  public boolean allows(InetAddress address) {
    var resolved = unmap(address);
    var blocked = resolved.getAddress().length == 4 ? BLOCKED_IPV4 : BLOCKED_IPV6;
    return blocked.stream().noneMatch(range -> range.contains(resolved));
  }

  /** Solo la forma mapeada se normaliza; compatible, 6to4 y NAT64 se bloquean por rango entero. */
  private static InetAddress unmap(InetAddress address) {
    if (!(address instanceof Inet6Address)) return address;
    var bytes = address.getAddress();
    if (!java.util.Arrays.equals(
        java.util.Arrays.copyOf(bytes, MAPPED_PREFIX.length), MAPPED_PREFIX)) return address;
    try {
      return InetAddress.getByAddress(
          java.util.Arrays.copyOfRange(bytes, MAPPED_PREFIX.length, bytes.length));
    } catch (java.net.UnknownHostException impossible) {
      return address;
    }
  }

  private record Cidr(byte[] network, int prefixLength) {
    static Cidr of(String literal, int prefixLength) {
      try {
        return new Cidr(InetAddress.getByName(literal).getAddress(), prefixLength);
      } catch (java.net.UnknownHostException error) {
        throw new IllegalStateException("Rango mal declarado: " + literal, error);
      }
    }

    boolean contains(InetAddress address) {
      var bytes = address.getAddress();
      if (bytes.length != network.length) return false;
      int wholeBytes = prefixLength / Byte.SIZE;
      for (int i = 0; i < wholeBytes; i++) if (bytes[i] != network[i]) return false;
      int remainingBits = prefixLength % Byte.SIZE;
      if (remainingBits == 0) return true;
      int mask = 0xff << (Byte.SIZE - remainingBits);
      return (bytes[wholeBytes] & mask) == (network[wholeBytes] & mask);
    }
  }
}
