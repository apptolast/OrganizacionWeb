package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * La fila de los webhooks (25). Un solo endpoint activo basta para estar conectado; si no queda
 * ninguno y alguno se apagó por agotar los reintentos, la fila es un error con ese código y el
 * instante en que se apagó.
 *
 * <p>Apagar un endpoint a mano no es un error: es una decisión del propietario, así que sin ninguno
 * activo y sin agotamientos la fila vuelve a {@code not_connected}.
 */
public final class WebhookStatusSource implements ConnectorStatusSource {
  public static final String ID = "webhooks";

  private final WebhookEndpoints endpoints;
  private final WebhookDeliveries deliveries;

  public WebhookStatusSource(WebhookEndpoints endpoints, WebhookDeliveries deliveries) {
    this.endpoints = endpoints;
    this.deliveries = deliveries;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return true;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    var configured = endpoints.list(ownerId);
    if (configured.isEmpty()) return ConnectorRow.notConnected(ID);
    var lastDelivery = lastDeliveryAt(ownerId, configured);
    if (configured.stream().anyMatch(WebhookEndpoint::isActive))
      return ConnectorRow.connected(ID, lastDelivery);
    var exhaustedAt = exhaustedAt(configured);
    if (exhaustedAt == null) return ConnectorRow.notConnected(ID);
    return ConnectorRow.error(
        ID, lastDelivery, new ConnectorError(WebhookEndpoint.DELIVERY_EXHAUSTED, exhaustedAt));
  }

  private Instant lastDeliveryAt(String ownerId, List<WebhookEndpoint> configured) {
    return configured.stream()
        .flatMap(endpoint -> deliveries.list(ownerId, endpoint.id()).stream())
        .map(WebhookDelivery::updatedAt)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }

  private static Instant exhaustedAt(List<WebhookEndpoint> configured) {
    return configured.stream()
        .filter(endpoint -> WebhookEndpoint.DELIVERY_EXHAUSTED.equals(endpoint.disabledReason()))
        .map(WebhookEndpoint::disabledAt)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(null);
  }
}
