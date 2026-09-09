package com.apptolast.organization.application;

/**
 * Guardia SSRF: un host solo se admite cuando resuelve y todas sus direcciones están permitidas.
 * Basta una dirección prohibida para rechazarlo entero, porque el cliente HTTP elegiría cualquiera
 * de ellas.
 *
 * <p>Riesgo residual conocido: entre esta comprobación y la conexión el DNS puede cambiar
 * (rebinding DNS). Se acepta y se mitiga repitiendo la comprobación en cada sincronización, no solo
 * al guardar.
 */
public final class OutboundHostGuard implements OutboundGuard {
  private final HostResolver resolver;
  private final AddressPolicy policy;

  public OutboundHostGuard(HostResolver resolver, AddressPolicy policy) {
    this.resolver = resolver;
    this.policy = policy;
  }

  @Override
  public Verdict check(String host) {
    java.util.List<java.net.InetAddress> addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (RuntimeException unavailable) {
      return Verdict.UNRESOLVABLE;
    }
    if (addresses == null || addresses.isEmpty()) return Verdict.UNRESOLVABLE;
    return addresses.stream().allMatch(policy::allows) ? Verdict.ALLOWED : Verdict.BLOCKED;
  }
}
