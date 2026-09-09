package com.apptolast.organization.application;

import java.util.UUID;

/**
 * Bitácora del conector. Ninguna firma admite el token: la imposibilidad de registrarlo es del
 * tipo, no de la disciplina de quien escribe la línea de log. Cada línea nombra el gestor al que se
 * refiere, porque un propietario puede tener varios conectados a la vez.
 */
public interface ConnectorAudit {
  void connected(String connector, String ownerId, String project, String account);

  void connectionRefused(
      String connector, String ownerId, String project, String errorCode, int providerStatus);

  void importFinished(
      String connector,
      String ownerId,
      String project,
      UUID importId,
      int created,
      int skipped,
      int failed,
      boolean truncated);

  void importFailed(
      String connector,
      String ownerId,
      String project,
      UUID importId,
      String errorCode,
      int created,
      int providerStatus);
}
