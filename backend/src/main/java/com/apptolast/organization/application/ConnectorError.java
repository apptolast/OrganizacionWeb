package com.apptolast.organization.application;

import java.time.Instant;

/**
 * Último fallo de un conector reducido a lo que se puede publicar: un código estable y el instante.
 * No hay campo para el mensaje del proveedor, así que ninguna respuesta puede filtrarlo.
 */
public record ConnectorError(String code, Instant at) {}
