package com.apptolast.organization.application;

import com.apptolast.organization.domain.Availability;
import com.apptolast.organization.domain.ExternalCalendarSnapshot;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.IcsCalendar;
import com.apptolast.organization.domain.IcsMalformedException;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.SyncSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Function;

/**
 * Descarga, analiza y reemplaza la instantánea. La descarga ocurre fuera de toda transacción y el
 * resultado se confirma condicionado a la versión leída antes de empezar: si otra escritura ganó, se
 * descarta sin reintentar.
 */
public final class SyncExternalCalendar implements ExternalCalendarUseCases.Sync {
  /** Por debajo de este umbral, una sincronización con {@code onlyIfStale} no se realiza. */
  public static final Duration FRESH = Duration.ofMinutes(15);

  private final ExternalCalendarStore store;
  private final SecretCipher cipher;
  private final OutboundGuard guard;
  private final CalendarFeed feed;
  private final AvailabilityQueries availability;
  private final ZoneCatalog zones;
  private final Clock clock;
  private final ExternalCalendarAudit audit;

  public SyncExternalCalendar(
      ExternalCalendarStore store,
      SecretCipher cipher,
      OutboundGuard guard,
      CalendarFeed feed,
      AvailabilityQueries availability,
      ZoneCatalog zones,
      Clock clock,
      ExternalCalendarAudit audit) {
    this.store = store;
    this.cipher = cipher;
    this.guard = guard;
    this.feed = feed;
    this.availability = availability;
    this.zones = zones;
    this.clock = clock;
    this.audit = audit;
  }

  @Override
  public SyncOutcome execute(String ownerId, boolean onlyIfStale) {
    var stored = store.find(ownerId).orElseThrow(ExternalCalendarNotConfiguredException::new);
    var now = clock.instant();
    if (onlyIfStale && !isStale(stored, now)) return new SyncOutcome(false, stored.subscription());
    long started = System.nanoTime();
    var attempt = attempt(ownerId, stored, now);
    audit.syncFinished(
        stored.subscription().urlHost(),
        attempt.error() == null ? SyncStatus.OK : SyncStatus.FAILED,
        attempt.error(),
        Duration.ofNanos(System.nanoTime() - started).toMillis());
    return commit(ownerId, stored.version(), attempt, now);
  }

  private static boolean isStale(StoredSubscription stored, Instant now) {
    var last = stored.subscription().lastAttemptAt();
    return last == null || last.isBefore(now.minus(FRESH));
  }

  /** Lo que se sabe tras intentar: o un resumen con sus eventos, o el código del fallo. */
  private record Attempt(
      FeedError error, SyncSummary summary, java.util.List<com.apptolast.organization.domain.ExternalEvent> events) {
    static Attempt failed(FeedError error) {
      return new Attempt(error, null, null);
    }
  }

  private Attempt attempt(String ownerId, StoredSubscription stored, Instant now) {
    var url = cipher.decrypt(ownerId, stored.urlCiphertext());
    if (url.isEmpty()) return Attempt.failed(FeedError.SECRET_UNREADABLE);
    if (guard.check(stored.subscription().urlHost()) != OutboundHostGuard.Verdict.ALLOWED)
      return Attempt.failed(FeedError.FEED_REJECTED);
    var fetched = feed.fetch(url.get());
    if (fetched instanceof FeedFetch.Failed failed) return Attempt.failed(failed.code());
    var zone = snapshotZone(ownerId);
    IcsCalendar calendar;
    try {
      calendar = IcsCalendar.parse(((FeedFetch.Downloaded) fetched).text(), zone, zones.zones());
    } catch (IcsMalformedException malformed) {
      return Attempt.failed(FeedError.FEED_MALFORMED);
    }
    var snapshot = ExternalCalendarSnapshot.select(calendar.events(), now);
    return new Attempt(
        null,
        new SyncSummary(
            zone.getId(),
            calendar.events().size(),
            calendar.skippedRecurring(),
            calendar.skippedCancelled(),
            calendar.skippedInvalid(),
            snapshot.truncated()),
        snapshot.events());
  }

  private SyncOutcome commit(String ownerId, long version, Attempt attempt, Instant now) {
    var committed =
        attempt.error() == null
            ? store.commitSuccess(ownerId, version, attempt.summary(), attempt.events(), now)
            : store.commitFailure(ownerId, version, attempt.error(), now);
    return committed
        .map(row -> new SyncOutcome(true, row.subscription()))
        .orElseGet(
            () ->
                new SyncOutcome(
                    false,
                    store
                        .find(ownerId)
                        .orElseThrow(ExternalCalendarNotConfiguredException::new)
                        .subscription()));
  }

  /** Zona de disponibilidad si el catálogo la reconoce; si no, UTC. */
  private ZoneId snapshotZone(String ownerId) {
    return availability
        .find(ownerId)
        .map(Availability::zoneId)
        .filter(zones.zones()::contains)
        .map((Function<String, ZoneId>) ZoneId::of)
        .orElse(UTC);
  }

  private static final ZoneId UTC = ZoneId.of("UTC");
}
