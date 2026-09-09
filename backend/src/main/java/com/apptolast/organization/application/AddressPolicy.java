package com.apptolast.organization.application;

import java.net.InetAddress;

/**
 * Decide si la aplicación puede abrir una conexión saliente hacia una dirección concreta. Es la
 * única política de egreso del producto: la comparten los conectores (features 25, 27 y 28).
 * Enmienda B2 de la revisión de seguridad.
 *
 * <p>Al integrar las features 25 y 28 se unificaron las dos implementaciones que existían.
 * Sobrevive esta forma —interfaz, no clase estática— porque admite inyección y sustitución en
 * pruebas, y porque es la que soporta el interruptor {@code
 * app.connectors.allow-private-addresses}. La conducta que sobrevive es la más restrictiva de las
 * dos: las formas IPv6 que encapsulan una IPv4 (compatible {@code ::/96}, 6to4 {@code 2002::/16} y
 * NAT64 {@code 64:ff9b::/96}) se bloquean por rango entero en vez de normalizarse a la IPv4
 * embebida. Ver {@link PublicAddressPolicy}.
 *
 * <p>Ojo con el sentido: {@code allows(a) == true} significa PERMITIDA. Quien espere un predicado
 * de «está bloqueada» tiene que negarlo explícitamente.
 */
public interface AddressPolicy {
  boolean allows(InetAddress address);

  static AddressPolicy blockingPrivateAddresses() {
    return new PublicAddressPolicy();
  }

  /**
   * Desactiva la guardia SSRF. Solo es admisible en el perfil de pruebas de extremo a extremo, con
   * {@code APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=true}; nunca en producción. El envío de webhooks
   * de la feature 25 queda fuera de ese interruptor: usa siempre {@link
   * #blockingPrivateAddresses()}.
   */
  static AddressPolicy allowingPrivateAddresses() {
    return address -> true;
  }
}
