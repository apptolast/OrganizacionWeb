package com.apptolast.organization.application;

import java.util.List;

/** El catálogo entero: seis filas en orden fijo y nada alrededor. */
public record ConnectorCatalog(List<ConnectorRow> connectors) {
  public ConnectorCatalog {
    connectors = List.copyOf(connectors);
  }

  public ConnectorRow row(String id) {
    return connectors.stream()
        .filter(row -> row.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("conector desconocido"));
  }
}
