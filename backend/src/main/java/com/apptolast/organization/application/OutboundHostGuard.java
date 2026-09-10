package com.apptolast.organization.application;

/**
 * Guardia SSRF: un host solo se admite cuando resuelve y todas sus direcciones están permitidas.
 * Basta una dirección prohibida para rechazarlo entero, porque el cliente HTTP elegiría cualquiera
 * de ellas.
 *
 * <p>Esta guardia decide si una suscripción puede guardarse o sincronizarse; <b>no</b> es la que
 * abre la conexión. La segunda mitad de la enmienda B3 —conectar contra la dirección literal ya
 * validada, conservando el nombre en Host y en SNI— vive en {@code adapter.net.AnchoredConnection},
 * que usan por igual el calendario externo y el emisor de webhooks: cada uno resuelve una vez y
 * conecta él mismo contra lo que validó. Por eso ya no queda aquí ningún «riesgo residual de
 * rebinding aceptado»: no hay ventana entre la resolución y la conexión, porque quien conecta es
 * quien resolvió.
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
