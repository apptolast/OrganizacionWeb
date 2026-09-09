package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.ConnectorAudit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Escribe la bitácora del conector con lo que sirve para diagnosticar —propietario, repositorio,
 * código HTTP de GitHub y contadores— y nada más. El token no aparece porque ningún método lo
 * recibe.
 */
public final class Slf4jConnectorAudit implements ConnectorAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.connectors");

  @Override
  public void connected(String ownerId, String repository, String login) {
    LOG.info(
        "connector=github outcome=connected owner={} repository={} login={}",
        ownerId,
        repository,
        login);
  }

  @Override
  public void connectionRefused(
      String ownerId, String repository, String errorCode, int githubStatus) {
    LOG.warn(
        "connector=github outcome=connection_refused owner={} repository={} code={} githubStatus={}",
        ownerId,
        repository,
        errorCode,
        githubStatus);
  }

  @Override
  public void importFinished(
      String ownerId,
      String repository,
      UUID importId,
      int created,
      int skipped,
      int failed,
      boolean truncated) {
    LOG.info(
        "connector=github outcome=import_finished owner={} repository={} importId={} created={}"
            + " skipped={} failed={} truncated={}",
        ownerId,
        repository,
        importId,
        created,
        skipped,
        failed,
        truncated);
  }

  @Override
  public void importFailed(
      String ownerId,
      String repository,
      UUID importId,
      String errorCode,
      int created,
      int githubStatus) {
    LOG.warn(
        "connector=github outcome=import_failed owner={} repository={} importId={} code={}"
            + " created={} githubStatus={}",
        ownerId,
        repository,
        importId,
        errorCode,
        created,
        githubStatus);
  }
}
