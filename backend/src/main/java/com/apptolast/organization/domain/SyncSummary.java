package com.apptolast.organization.domain;

/**
 * Lo que describe una sincronización correcta. Los contadores hablan de todo el feed, no solo de lo
 * que cupo en la ventana almacenada.
 */
public record SyncSummary(
    String snapshotZoneId,
    int imported,
    int skippedRecurring,
    int skippedCancelled,
    int skippedInvalid,
    boolean truncated) {
  public SyncSummary {
    if (snapshotZoneId == null || snapshotZoneId.isBlank())
      throw new IllegalArgumentException("Toda sincronización se resuelve en una zona conocida");
    if (imported < 0 || skippedRecurring < 0 || skippedCancelled < 0 || skippedInvalid < 0)
      throw new IllegalArgumentException("Los contadores no pueden ser negativos");
  }
}
