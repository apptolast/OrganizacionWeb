package com.apptolast.organization.application;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de los recibos. {@code begin} es la sección crítica: cierra como
 * {@code INTERRUPTED} el recibo en curso anterior a {@code staleBefore} y garantiza que nunca haya
 * más de un recibo {@code running} por propietario, o lanza {@link IssueImportInProgressException}.
 */
public interface IssueImportReceiptStore {
  IssueImportReceipt begin(
      String ownerId, UUID projectId, String repository, Instant startedAt, Instant staleBefore);

  void progress(String ownerId, UUID importId, int created, int skipped, int failed);

  IssueImportReceipt finish(
      String ownerId,
      UUID importId,
      String status,
      String errorCode,
      boolean truncated,
      Instant finishedAt);

  Optional<IssueImportReceipt> find(String ownerId, UUID importId);

  Optional<IssueImportReceipt> latest(String ownerId);
}
