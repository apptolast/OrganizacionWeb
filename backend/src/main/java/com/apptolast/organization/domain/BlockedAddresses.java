package com.apptolast.organization.domain;

import java.net.InetAddress;

/** Egress policy: destinations that a webhook must never reach (SSRF guard). */
public final class BlockedAddresses {
  private BlockedAddresses() {}

  public static boolean isBlocked(InetAddress address) {
    if (address.isLoopbackAddress()
        || address.isAnyLocalAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) return true;
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) return carrierGradeNat(bytes);
    return uniqueLocalIpv6(bytes);
  }

  /** 100.64.0.0/10 */
  private static boolean carrierGradeNat(byte[] bytes) {
    return (bytes[0] & 0xff) == 100 && (bytes[1] & 0xc0) == 0x40;
  }

  /** fc00::/7 */
  private static boolean uniqueLocalIpv6(byte[] bytes) {
    return (bytes[0] & 0xfe) == 0xfc;
  }
}
