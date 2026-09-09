package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.ConnectorAudit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe la bitácora del conector con lo que sirve para diagnosticar —gestor, propietario,
 * proyecto, código HTTP del proveedor y contadores— y nada más. El token no aparece porque ningún
 * método lo recibe.
 */
public final class Slf4jConnectorAudit implements ConnectorAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.connectors");

  @Override
  public void connected(String connector, String ownerId, String project, String account) {
    LOG.info(
        "connector={} outcome=connected owner={} project={} account={}",
        connector,
        ownerId,
        project,
        account);
  }

  @Override
  public void connectionRefused(
      String connector, String ownerId, String project, String errorCode, int providerStatus) {
    LOG.warn(
        "connector={} outcome=connection_refused owner={} project={} code={} providerStatus={}",
        connector,
        ownerId,
        project,
        errorCode,
        providerStatus);
  }

  @Override
  public void importFinished(
      String connector,
      String ownerId,
      String project,
      UUID importId,
      int created,
      int skipped,
      int failed,
      boolean truncated) {
    LOG.info(
        "connector={} outcome=import_finished owner={} project={} importId={} created={}"
            + " skipped={} failed={} truncated={}",
        connector,
        ownerId,
        project,
        importId,
        created,
        skipped,
        failed,
        truncated);
  }

  @Override
  public void importFailed(
      String connector,
      String ownerId,
      String project,
      UUID importId,
      String errorCode,
      int created,
      int providerStatus) {
    LOG.warn(
        "connector={} outcome=import_failed owner={} project={} importId={} code={}"
            + " created={} providerStatus={}",
        connector,
        ownerId,
        project,
        importId,
        errorCode,
        created,
        providerStatus);
  }
}
