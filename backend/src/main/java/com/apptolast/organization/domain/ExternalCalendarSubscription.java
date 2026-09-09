package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Los quince campos que la aplicación publica de una suscripción. La dirección completa nunca forma
 * parte de este dato: solo el host y los cuatro últimos caracteres.
 */
public record ExternalCalendarSubscription(
    UUID id,
    String label,
    String urlHost,
    String urlTail,
    Instant lastAttemptAt,
    Instant lastSyncAt,
    SyncStatus lastStatus,
    FeedError lastError,
    String snapshotZoneId,
    int imported,
    int skippedRecurring,
    int skippedCancelled,
    int skippedInvalid,
    boolean truncated,
    Instant updatedAt) {}
