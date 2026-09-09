package com.apptolast.organization.application;

import java.net.InetAddress;

/**
 * Decide si la aplicación puede abrir una conexión saliente hacia una dirección concreta. La
 * comparten los conectores (features 25, 27 y 28); al integrar se unifica con la implementación de
 * la feature 25. Enmienda B2 de la revisión de seguridad.
 */
public interface AddressPolicy {
  boolean allows(InetAddress address);

  static AddressPolicy blockingPrivateAddresses() {
    return new PublicAddressPolicy();
  }

  /**
   * Desactiva la guardia SSRF. Solo es admisible en el perfil de pruebas de extremo a extremo, con
   * {@code APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=true}; nunca en producción.
   */
  static AddressPolicy allowingPrivateAddresses() {
    return address -> true;
  }
}
