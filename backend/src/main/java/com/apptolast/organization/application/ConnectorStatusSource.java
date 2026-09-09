package com.apptolast.organization.application;

/**
 * Cada conector deriva su propia fila del catálogo de su propia fuente. El catálogo no sabe leer
 * webhooks ni calendarios: sólo sabe pedir, en orden, y respetar lo que le contesten.
 */
public interface ConnectorStatusSource {
  String id();

  /** Cierto si la fuente guarda secretos cifrados y por tanto depende de la clave del servidor. */
  boolean encryptsSecrets();

  ConnectorRow read(String ownerId);
}
