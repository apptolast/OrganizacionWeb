package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Recibo persistido de una importación. Es el único cuerpo que la API devuelve para una
 * importación, con sus once campos siempre presentes: lo que el POST responde es exactamente lo
 * que el GET del recibo vuelve a leer, también tras un reinicio.
 */
public record IssueImportReceipt(
    UUID id,
    UUID projectId,
    String repository,
    String status,
    int created,
    int skipped,
    int failed,
    boolean truncated,
    String errorCode,
    Instant startedAt,
    Instant finishedAt) {
  public static final String RUNNING = "running";
  public static final String COMPLETED = "completed";
  public static final String FAILED = "failed";
  private static final Set<String> STATUSES = Set.of(RUNNING, COMPLETED, FAILED);

  public IssueImportReceipt {
    if (id == null || projectId == null || repository == null || startedAt == null)
      throw new IllegalArgumentException("An import receipt requires identity and a start instant");
    if (!STATUSES.contains(status))
      throw new IllegalArgumentException("An import receipt is running, completed or failed");
    if (created < 0 || skipped < 0 || failed < 0)
      throw new IllegalArgumentException("Import counters never go below zero");
    if (RUNNING.equals(status) != (finishedAt == null))
      throw new IllegalArgumentException("Only a running receipt lacks a finish instant");
    if (finishedAt != null && finishedAt.isBefore(startedAt))
      throw new IllegalArgumentException("An import cannot finish before it starts");
    if (RUNNING.equals(status) && errorCode != null)
      throw new IllegalArgumentException("A running receipt carries no error code");
  }

  public boolean isRunning() {
    return RUNNING.equals(status);
  }

  public IssueImportReceipt withCounters(int created, int skipped, int failed) {
    return new IssueImportReceipt(
        id,
        projectId,
        repository,
        status,
        created,
        skipped,
        failed,
        truncated,
        errorCode,
        startedAt,
        finishedAt);
  }

  public IssueImportReceipt close(
      String status, String errorCode, boolean truncated, Instant finishedAt) {
    return new IssueImportReceipt(
        id,
        projectId,
        repository,
        status,
        created,
        skipped,
        failed,
        truncated,
        errorCode,
        startedAt,
        finishedAt.isBefore(startedAt) ? startedAt : finishedAt);
  }
}
