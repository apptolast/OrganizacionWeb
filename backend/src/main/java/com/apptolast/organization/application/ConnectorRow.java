package com.apptolast.organization.application;

import java.time.Instant;

/**
 * Una fila del catálogo: cuatro campos y ni uno más. Lo que cada conector sabe de sí mismo se
 * reduce aquí a un estado, un instante y un error con código estable, porque el catálogo es una
 * vista de un vistazo y no la pantalla de detalle de nadie.
 */
public record ConnectorRow(
    String id, String status, Instant lastActivityAt, ConnectorError lastError) {
  public static final String NOT_CONNECTED = "not_connected";
  public static final String DISABLED = "disabled";
  public static final String CONNECTED = "connected";
  public static final String ERROR = "error";

  /** La integración existe y responde; el instante es lo último que se sabe de ella. */
  public static ConnectorRow connected(String id, Instant lastActivityAt) {
    return new ConnectorRow(id, CONNECTED, lastActivityAt, null);
  }

  /** La integración existe y está rota: se publica el código y cuándo, nunca el texto ajeno. */
  public static ConnectorRow error(String id, Instant lastActivityAt, ConnectorError lastError) {
    return new ConnectorRow(id, ERROR, lastActivityAt, lastError);
  }

  /** Sin integración no hay error: hay ausencia, que es lo que la pantalla ofrece configurar. */
  public static ConnectorRow notConnected(String id) {
    return new ConnectorRow(id, NOT_CONNECTED, null, null);
  }

  /** Sin clave de cifrado no se puede ni mirar: no se intenta descifrar para poder informar. */
  public static ConnectorRow disabled(String id) {
    return new ConnectorRow(id, DISABLED, null, null);
  }
}
