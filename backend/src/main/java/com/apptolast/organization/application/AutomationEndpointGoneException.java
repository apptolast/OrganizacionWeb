package com.apptolast.organization.application;

import java.util.UUID;

/**
 * El endpoint de una acción NOTIFY_WEBHOOK ya no es un endpoint activo de este propietario en el
 * instante de la escritura. No es lo mismo que {@link AutomationClaimedException}: allí la fila la
 * escribió otro y hay que dejarla en paz; aquí no hay fila de nadie y la regla se queda sin
 * resolver, que es la forma que tiene este sistema de perder ejecuciones para siempre.
 *
 * <p>Lo lanza el puerto transaccional desde dentro de la confirmación, así que nada de lo que
 * hubiera en ella llegó a escribirse; el ejecutor vuelve a confirmar el mismo evento con esta regla
 * ya resuelta en {@code ENDPOINT_NOT_FOUND}, que es lo que pide el contrato.
 */
public final class AutomationEndpointGoneException extends RuntimeException {
  private final UUID endpointId;

  public AutomationEndpointGoneException(UUID endpointId) {
    super("El endpoint ya no está activo para este propietario.");
    this.endpointId = endpointId;
  }

  public UUID endpointId() {
    return endpointId;
  }
}
