package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Bitácora del conector. Ninguna firma admite el token: la imposibilidad de registrarlo es del
 * tipo, no de la disciplina de quien escribe la línea de log.
 */
public interface ConnectorAudit {
  void connected(String ownerId, String repository, String login);

  void connectionRefused(String ownerId, String repository, String errorCode, int githubStatus);

  void importFinished(
      String ownerId,
      String repository,
      UUID importId,
      int created,
      int skipped,
      int failed,
      boolean truncated);

  void importFailed(
      String ownerId,
      String repository,
      UUID importId,
      String errorCode,
      int created,
      int githubStatus);
}
