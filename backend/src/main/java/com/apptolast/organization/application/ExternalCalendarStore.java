package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Una suscripción como máximo por propietario. Las dos confirmaciones de sincronización devuelven
 * vacío cuando otra escritura ganó la carrera por la versión: entonces nada cambia y la
 * sincronización se descarta.
 */
public interface ExternalCalendarStore {
  Optional<StoredSubscription> find(String ownerId);

  StoredSubscription create(
      String ownerId, UUID id, ExternalCalendarInput input, byte[] urlCiphertext, Instant now);

  /** Cambia solo la etiqueta: conserva instantánea, contadores y estado. */
  StoredSubscription relabel(String ownerId, String label, Instant now);

  /** Cambia la dirección: borra la instantánea y reinicia estado y contadores. */
  StoredSubscription rebind(
      String ownerId, ExternalCalendarInput input, byte[] urlCiphertext, Instant now);

  boolean delete(String ownerId);

  Optional<StoredSubscription> commitSuccess(
      String ownerId,
      long expectedVersion,
      SyncSummary summary,
      List<ExternalEvent> events,
      Instant syncAt);

  Optional<StoredSubscription> commitFailure(
      String ownerId, long expectedVersion, FeedError error, Instant attemptAt);

  List<ExternalEvent> events(String ownerId, Instant from, Instant to);
}
