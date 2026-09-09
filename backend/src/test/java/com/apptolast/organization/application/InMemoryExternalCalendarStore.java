package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.SyncSummary;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Doble en memoria con la misma semántica de versión que el adaptador de PostgreSQL. */
final class InMemoryExternalCalendarStore implements ExternalCalendarStore {
  private final Map<String, StoredSubscription> rows = new HashMap<>();
  private final Map<String, List<ExternalEvent>> snapshots = new HashMap<>();

  @Override
  public Optional<StoredSubscription> find(String ownerId) {
    return Optional.ofNullable(rows.get(ownerId));
  }

  @Override
  public StoredSubscription create(
      String ownerId, UUID id, ExternalCalendarInput input, byte[] urlCiphertext, Instant now) {
    var subscription =
        new ExternalCalendarSubscription(
            id, input.label(), input.urlHost(), input.urlTail(), null, null, null, null, null, 0, 0,
            0, 0, false, now);
    return put(ownerId, new StoredSubscription(subscription, urlCiphertext, 0));
  }

  @Override
  public StoredSubscription relabel(
      String ownerId, String label, byte[] urlCiphertext, Instant now) {
    var previous = rows.get(ownerId);
    var kept = previous.subscription();
    return put(
        ownerId,
        new StoredSubscription(
            new ExternalCalendarSubscription(
                kept.id(), label, kept.urlHost(), kept.urlTail(), kept.lastAttemptAt(),
                kept.lastSyncAt(), kept.lastStatus(), kept.lastError(), kept.snapshotZoneId(),
                kept.imported(), kept.skippedRecurring(), kept.skippedCancelled(),
                kept.skippedInvalid(), kept.truncated(), now),
            urlCiphertext,
            previous.version() + 1));
  }

  @Override
  public StoredSubscription rebind(
      String ownerId, ExternalCalendarInput input, byte[] urlCiphertext, Instant now) {
    var previous = rows.get(ownerId);
    snapshots.remove(ownerId);
    return put(
        ownerId,
        new StoredSubscription(
            new ExternalCalendarSubscription(
                previous.subscription().id(), input.label(), input.urlHost(), input.urlTail(),
                null, null, null, null, null, 0, 0, 0, 0, false, now),
            urlCiphertext,
            previous.version() + 1));
  }

  @Override
  public boolean delete(String ownerId) {
    snapshots.remove(ownerId);
    return rows.remove(ownerId) != null;
  }

  @Override
  public Optional<StoredSubscription> commitSuccess(
      String ownerId,
      long expectedVersion,
      SyncSummary summary,
      List<ExternalEvent> events,
      Instant syncAt) {
    var previous = rows.get(ownerId);
    if (previous == null || previous.version() != expectedVersion) return Optional.empty();
    snapshots.put(ownerId, List.copyOf(events));
    var kept = previous.subscription();
    return Optional.of(
        put(
            ownerId,
            new StoredSubscription(
                new ExternalCalendarSubscription(
                    kept.id(), kept.label(), kept.urlHost(), kept.urlTail(), syncAt, syncAt,
                    SyncStatus.OK, null, summary.snapshotZoneId(), summary.imported(),
                    summary.skippedRecurring(), summary.skippedCancelled(),
                    summary.skippedInvalid(), summary.truncated(), syncAt),
                previous.urlCiphertext(),
                previous.version() + 1)));
  }

  @Override
  public Optional<StoredSubscription> commitFailure(
      String ownerId, long expectedVersion, FeedError error, Instant attemptAt) {
    var previous = rows.get(ownerId);
    if (previous == null || previous.version() != expectedVersion) return Optional.empty();
    var kept = previous.subscription();
    return Optional.of(
        put(
            ownerId,
            new StoredSubscription(
                new ExternalCalendarSubscription(
                    kept.id(), kept.label(), kept.urlHost(), kept.urlTail(), attemptAt,
                    kept.lastSyncAt(), SyncStatus.FAILED, error, kept.snapshotZoneId(),
                    kept.imported(), kept.skippedRecurring(), kept.skippedCancelled(),
                    kept.skippedInvalid(), kept.truncated(), attemptAt),
                previous.urlCiphertext(),
                previous.version() + 1)));
  }

  @Override
  public List<ExternalEvent> events(String ownerId, Instant from, Instant to) {
    return snapshots.getOrDefault(ownerId, List.of()).stream()
        .filter(event -> event.startAt().isBefore(to) && event.endAt().isAfter(from))
        .sorted(Comparator.comparing(ExternalEvent::startAt).thenComparing(ExternalEvent::uid))
        .toList();
  }

  List<ExternalEvent> stored(String ownerId) {
    return snapshots.getOrDefault(ownerId, List.of());
  }

  void seedSnapshot(String ownerId, List<ExternalEvent> events) {
    snapshots.put(ownerId, List.copyOf(events));
  }

  StoredSubscription put(String ownerId, StoredSubscription row) {
    rows.put(ownerId, row);
    return row;
  }
}
