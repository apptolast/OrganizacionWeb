package com.apptolast.organization.application;

import java.time.Clock;

/**
 * El guardián de importación es por propietario y no por gestor: una persona no puede tener dos
 * importaciones a la vez aunque sean de proveedores distintos, y mientras una corre tampoco puede
 * sustituir ni soltar la conexión de la que esa importación está leyendo.
 *
 * <p>Un recibo en curso más viejo que el plazo dejó de existir de verdad —el proceso que lo abrió
 * se cayó— y deja de bloquear. El plazo es el mismo que aplica {@link ImportIssues}, en un solo
 * sitio, para que no puedan divergir.
 */
final class ImportGuard {
  private ImportGuard() {}

  static void requireIdle(IssueImportReceiptStore receipts, String ownerId, Clock clock) {
    if (receipts.importing(ownerId, clock.instant().minus(ImportIssues.ABANDONED_AFTER)))
      throw new IssueImportInProgressException();
  }
}
