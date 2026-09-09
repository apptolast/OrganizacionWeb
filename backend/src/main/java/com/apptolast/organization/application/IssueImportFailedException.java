package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;

/**
 * La importación arrancó y no pudo terminar. Lleva el recibo ya cerrado, así que quien responda
 * conoce el identificador, el código y los contadores parciales sin volver a consultar el almacén.
 */
public final class IssueImportFailedException extends RuntimeException {
  private final IssueImportReceipt receipt;
  private final int retryAfterSeconds;

  public IssueImportFailedException(IssueImportReceipt receipt, int retryAfterSeconds) {
    this.receipt = receipt;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public IssueImportReceipt receipt() {
    return receipt;
  }

  /** Segundos que pide el gestor externo antes de reintentar; cero cuando no aplica. */
  public int retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
