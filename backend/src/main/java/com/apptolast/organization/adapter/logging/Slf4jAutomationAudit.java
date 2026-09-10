package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.AutomationAudit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe la bitácora del ejecutor de automatizaciones. Sólo identificadores: el título renderizado
 * y el nombre del proyecto son contenido del propietario y no llegan hasta aquí, porque el puerto
 * no los admite.
 *
 * <p>El {@code Logger} vive en el adaptador y no en el caso de uso para que la capa de aplicación
 * no dependa de {@code org.slf4j}, que es lo que exige la guarda hexagonal. Mismo reparto que
 * {@link Slf4jConnectorAudit}.
 */
public final class Slf4jAutomationAudit implements AutomationAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.automations");

  @Override
  public void runFinished(UUID ruleId, UUID eventId, String status, int attempt, String errorCode) {
    LOG.info(
        "Automation run; ruleId={} eventId={} outcome={} attempt={} code={}",
        ruleId,
        eventId,
        status,
        attempt,
        errorCode);
  }

  @Override
  public void cycleFailed(String ownerId, UUID eventId, String category) {
    LOG.warn(
        "Automation cycle stopped for one owner; ownerId={} eventId={} category={}",
        ownerId,
        eventId,
        category);
  }
}
