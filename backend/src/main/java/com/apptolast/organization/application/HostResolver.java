package com.apptolast.organization.application;

import java.net.InetAddress;
import java.util.List;

/**
 * Resuelve un nombre de host a todas sus direcciones. Lista vacía significa que no resuelve. Existe
 * como puerto para poder describir en las pruebas zonas DNS que no se pueden montar de verdad.
 */
public interface HostResolver {
  List<InetAddress> resolve(String host);
}
