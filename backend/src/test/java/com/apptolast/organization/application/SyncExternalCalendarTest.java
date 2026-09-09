package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.domain.Availability;
import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s11, @s12, @s14, @s16, @s24, @s26, @s27, @s28, @s29.
 */
class SyncExternalCalendarTest {
  static final String OWNER = "persona-a";
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final Instant EARLIER = Instant.parse("2030-01-07T09:00:00Z");
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";
  static final String ICS =
      "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:u1@example\r\n"
          + "DTSTART:20300108T090000Z\r\nDTEND:20300108T100000Z\r\nSUMMARY:Reunión\r\n"
          + "END:VEVENT\r\nEND:VCALENDAR\r\n";

  InMemoryExternalCalendarStore store;
  RecordingFeed feed;
  RecordingAudit audit;
  String zoneOfOwner;
  OutboundHostGuard.Verdict verdict;
  Optional<String> decrypted;

  static final class RecordingFeed implements CalendarFeed {
    final List<String> requested = new ArrayList<>();
    FeedFetch answer = FeedFetch.downloaded(ICS);
    Runnable whileDownloading = () -> {};

    @Override
    public FeedFetch fetch(String url) {
      requested.add(url);
      whileDownloading.run();
      return answer;
    }
  }

  static final class RecordingAudit implements ExternalCalendarAudit {
    final List<String> lines = new ArrayList<>();

    @Override
    public void syncFinished(String host, SyncStatus status, FeedError error, long millis) {
      lines.add(host + " " + status + " " + error + " " + (millis >= 0));
    }
  }

  @BeforeEach
  void setUp() {
    store = new InMemoryExternalCalendarStore();
    feed = new RecordingFeed();
    audit = new RecordingAudit();
    zoneOfOwner = "Europe/Madrid";
    verdict = OutboundHostGuard.Verdict.ALLOWED;
    decrypted = Optional.of(URL);
  }

  SyncExternalCalendar sync() {
    return sync(NOW);
  }

  SyncExternalCalendar sync(Instant now) {
    SecretCipher cipher =
        new SecretCipher() {
          @Override
          public boolean enabled() {
            return true;
          }

          @Override
          public byte[] encrypt(String ownerId, String url) {
            return url.getBytes(java.nio.charset.StandardCharsets.UTF_8);
          }

          @Override
          public Optional<String> decrypt(String ownerId, byte[] stored) {
            return decrypted;
          }
        };
    AvailabilityQueries availability =
        owner ->
            zoneOfOwner == null
                ? Optional.empty()
                : Optional.of(
                    new Availability(
                        UUID.randomUUID(), owner, zoneOfOwner, minutes(), 0, EARLIER, EARLIER));
    ZoneCatalog zones = () -> ZoneId.getAvailableZoneIds();
    return new SyncExternalCalendar(
        store,
        cipher,
        host -> verdict,
        feed,
        availability,
        zones,
        Clock.fixed(now, ZoneOffset.UTC),
        audit);
  }

  static java.util.Map<DayOfWeek, Integer> minutes() {
    var daily = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) daily.put(day, 60);
    return daily;
  }

  void subscribed() {
    store.create(
        OWNER,
        UUID.randomUUID(),
        ExternalCalendarInput.of("Trabajo", URL),
        URL.getBytes(java.nio.charset.StandardCharsets.UTF_8),
        EARLIER);
  }

  void withPreviousSnapshot() {
    subscribed();
    store.seedSnapshot(
        OWNER,
        List.of(
            new ExternalEvent(
                "viejo",
                "Anterior",
                Instant.parse("2030-01-07T08:00:00Z"),
                Instant.parse("2030-01-07T09:00:00Z"),
                false)));
    store.commitSuccess(
        OWNER,
        0,
        new com.apptolast.organization.domain.SyncSummary("Europe/Madrid", 5, 0, 0, 0, false),
        store.stored(OWNER),
        EARLIER);
  }

  @Test
  void s28_syncingWithoutASubscriptionIsRejectedWithoutDownloading() {
    assertThrows(ExternalCalendarNotConfiguredException.class, () -> sync().execute(OWNER, false));
    assertTrue(feed.requested.isEmpty());
  }

  @Test
  void s14_aSuccessfulSyncStoresTheEventAndCapturesTheClockOnce() {
    subscribed();
    var outcome = sync().execute(OWNER, false);
    assertTrue(outcome.performed());
    var subscription = outcome.subscription();
    assertEquals(SyncStatus.OK, subscription.lastStatus());
    assertNull(subscription.lastError());
    assertEquals(NOW, subscription.lastSyncAt());
    assertEquals(subscription.lastSyncAt(), subscription.lastAttemptAt());
    assertEquals(1, subscription.imported());
    assertEquals(0, subscription.skippedRecurring());
    assertEquals(0, subscription.skippedCancelled());
    assertEquals(0, subscription.skippedInvalid());
    assertEquals("Europe/Madrid", subscription.snapshotZoneId());
    var stored = store.stored(OWNER);
    assertEquals(1, stored.size());
    assertEquals("u1@example", stored.getFirst().uid());
    assertEquals("Reunión", stored.getFirst().summary());
    assertEquals(Instant.parse("2030-01-08T09:00:00Z"), stored.getFirst().startAt());
    assertEquals(Instant.parse("2030-01-08T10:00:00Z"), stored.getFirst().endAt());
  }

  @Test
  void s13_theDownloadedUrlIsTheDecryptedOne() {
    subscribed();
    sync().execute(OWNER, false);
    assertEquals(List.of(URL), feed.requested);
  }

  @Test
  void s11_aRejectedHostFailsWithoutAnyHttpRequest() {
    withPreviousSnapshot();
    verdict = OutboundHostGuard.Verdict.BLOCKED;
    var outcome = sync().execute(OWNER, false);
    assertTrue(outcome.performed());
    assertEquals(SyncStatus.FAILED, outcome.subscription().lastStatus());
    assertEquals(FeedError.FEED_REJECTED, outcome.subscription().lastError());
    assertEquals(NOW, outcome.subscription().lastAttemptAt());
    assertEquals(EARLIER, outcome.subscription().lastSyncAt());
    assertEquals(5, outcome.subscription().imported());
    assertEquals(1, store.stored(OWNER).size());
    assertTrue(feed.requested.isEmpty());
  }

  @Test
  void s11_aHostThatNoLongerResolvesIsAlsoRejected() {
    subscribed();
    verdict = OutboundHostGuard.Verdict.UNRESOLVABLE;
    assertEquals(FeedError.FEED_REJECTED, sync().execute(OWNER, false).subscription().lastError());
    assertTrue(feed.requested.isEmpty());
  }

  @Test
  void s29_anUnreadableSecretFailsWithoutAnyHttpRequest() {
    withPreviousSnapshot();
    decrypted = Optional.empty();
    var outcome = sync().execute(OWNER, false);
    assertTrue(outcome.performed());
    assertEquals(FeedError.SECRET_UNREADABLE, outcome.subscription().lastError());
    assertEquals(EARLIER, outcome.subscription().lastSyncAt());
    assertEquals(1, store.stored(OWNER).size());
    assertTrue(feed.requested.isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"FEED_HTTP_ERROR", "FEED_UNREACHABLE", "FEED_TOO_LARGE", "FEED_UNSUPPORTED_TYPE"})
  void s12_aDownloadFailureKeepsThePreviousSnapshot(FeedError code) {
    withPreviousSnapshot();
    feed.answer = FeedFetch.failed(code);
    var outcome = sync().execute(OWNER, false);
    assertTrue(outcome.performed());
    assertEquals(code, outcome.subscription().lastError());
    assertEquals(SyncStatus.FAILED, outcome.subscription().lastStatus());
    assertEquals(EARLIER, outcome.subscription().lastSyncAt());
    assertEquals(5, outcome.subscription().imported());
    assertEquals(1, store.stored(OWNER).size());
  }

  @ParameterizedTest
  @CsvSource({"'<html>Vaya</html>'", "'BEGIN:VEVENT\\nEND:VEVENT'", "''"})
  void s12_aBodyThatIsNotACalendarIsMalformed(String body) {
    withPreviousSnapshot();
    feed.answer = FeedFetch.downloaded(body.replace("\\n", "\r\n"));
    var outcome = sync().execute(OWNER, false);
    assertEquals(FeedError.FEED_MALFORMED, outcome.subscription().lastError());
    assertEquals(1, store.stored(OWNER).size());
  }

  @Test
  void s12_theAuditRecordsHostStatusAndDurationWithoutThePath() {
    withPreviousSnapshot();
    feed.answer = FeedFetch.failed(FeedError.FEED_HTTP_ERROR);
    sync().execute(OWNER, false);
    assertEquals(List.of("feed.example.test FAILED FEED_HTTP_ERROR true"), audit.lines);
    assertTrue(audit.lines.stream().noneMatch(line -> line.contains("abc123")));
  }

  @Test
  void s14_theAuditAlsoRecordsASuccess() {
    subscribed();
    sync().execute(OWNER, false);
    assertEquals(List.of("feed.example.test OK null true"), audit.lines);
  }

  @ParameterizedTest
  @CsvSource({"Europe/Madrid, Europe/Madrid", "UTC, UTC", "Legacy/Retired, UTC", ", UTC"})
  void s16_theSnapshotZoneFallsBackToUtc(String availability, String expected) {
    subscribed();
    zoneOfOwner = availability == null || availability.isBlank() ? null : availability;
    assertEquals(expected, sync().execute(OWNER, false).subscription().snapshotZoneId());
  }

  @Test
  void s16_floatingTimesAreResolvedInTheSnapshotZone() {
    subscribed();
    feed.answer =
        FeedFetch.downloaded(
            "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:f1\r\nDTSTART:20300108T100000\r\n"
                + "DTEND:20300108T110000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n");
    sync().execute(OWNER, false);
    assertEquals(Instant.parse("2030-01-08T09:00:00Z"), store.stored(OWNER).getFirst().startAt());
  }

  @Test
  void s24_countersDescribeTheWholeFeedWhileTheSnapshotIsTruncated() {
    subscribed();
    feed.answer = FeedFetch.downloaded(manyEvents(600, 50));
    var outcome = sync().execute(OWNER, false);
    assertEquals(650, outcome.subscription().imported());
    assertTrue(outcome.subscription().truncated());
    assertEquals(500, store.stored(OWNER).size());
  }

  @Test
  void s23_eventsOutsideTheWindowCountAsImportedButAreNotStored() {
    subscribed();
    feed.answer = FeedFetch.downloaded(manyEvents(0, 3));
    var outcome = sync().execute(OWNER, false);
    assertEquals(3, outcome.subscription().imported());
    assertTrue(store.stored(OWNER).isEmpty());
    assertEquals(false, outcome.subscription().truncated());
  }

  @Test
  void s26_aSyncThatLosesTheVersionRaceReportsTheCurrentSubscription() {
    subscribed();
    feed.whileDownloading = () -> store.relabel(OWNER, "Ganadora", new byte[] {1, 2, 3}, NOW);
    var outcome = sync().execute(OWNER, false);
    assertEquals(false, outcome.performed());
    assertEquals("Ganadora", outcome.subscription().label());
    assertNull(outcome.subscription().lastStatus());
    assertTrue(store.stored(OWNER).isEmpty(), "la instantánea perdedora no se escribe");
    assertEquals(1, feed.requested.size(), "una sola descarga, sin reintento automático");
  }

  @Test
  void s26_aFailedSyncThatLosesTheRaceIsAlsoDiscarded() {
    withPreviousSnapshot();
    feed.answer = FeedFetch.failed(FeedError.FEED_HTTP_ERROR);
    feed.whileDownloading = () -> store.relabel(OWNER, "Ganadora", new byte[] {1, 2, 3}, NOW);
    var outcome = sync().execute(OWNER, false);
    assertEquals(false, outcome.performed());
    assertEquals("Ganadora", outcome.subscription().label());
    assertEquals(SyncStatus.OK, outcome.subscription().lastStatus());
    assertNull(outcome.subscription().lastError());
  }

  @ParameterizedTest
  @CsvSource({
    "null, true, true, 1",
    "2030-01-07T11:44:59Z, true, true, 1",
    "2030-01-07T11:45:00Z, true, false, 0",
    "2030-01-07T11:59:00Z, true, false, 0",
    "2030-01-07T11:59:00Z, false, true, 1"
  })
  void s27_freshnessIsDecidedFromLastAttemptAt(
      String attempt, boolean onlyIfStale, boolean performed, int downloads) {
    subscribed();
    if (!"null".equals(attempt)) {
      store.commitFailure(OWNER, 0, FeedError.FEED_HTTP_ERROR, Instant.parse(attempt));
    }
    var outcome = sync().execute(OWNER, onlyIfStale);
    assertEquals(performed, outcome.performed());
    assertEquals(downloads, feed.requested.size());
  }

  @Test
  void s27_aStaleAttemptIsMeasuredEvenWhenTheLastSyncIsMuchOlder() {
    subscribed();
    store.commitFailure(OWNER, 0, FeedError.FEED_HTTP_ERROR, Instant.parse("2030-01-07T11:50:00Z"));
    var outcome = sync().execute(OWNER, true);
    assertEquals(false, outcome.performed());
    assertEquals(FeedError.FEED_HTTP_ERROR, outcome.subscription().lastError());
    assertTrue(feed.requested.isEmpty());
  }

  @Test
  void s27_skippingBecauseItIsFreshReturnsTheStoredSubscriptionUntouched() {
    subscribed();
    var before = store.find(OWNER).orElseThrow();
    store.commitFailure(OWNER, 0, FeedError.FEED_HTTP_ERROR, NOW);
    var outcome = sync().execute(OWNER, true);
    assertEquals(false, outcome.performed());
    assertSame(store.find(OWNER).orElseThrow().subscription(), outcome.subscription());
    assertEquals(before.subscription().id(), outcome.subscription().id());
  }

  static String manyEvents(int inside, int outside) {
    var ics = new StringBuilder("BEGIN:VCALENDAR\r\n");
    for (int i = 0; i < inside; i++) {
      var start = Instant.parse("2030-01-08T00:00:00Z").plusSeconds(60L * i);
      ics.append("BEGIN:VEVENT\r\nUID:in-")
          .append(String.format("%04d", i))
          .append("\r\nDTSTART:")
          .append(compact(start))
          .append("\r\nDTEND:")
          .append(compact(start.plusSeconds(60)))
          .append("\r\nEND:VEVENT\r\n");
    }
    for (int i = 0; i < outside; i++) {
      var start = Instant.parse("2031-01-08T00:00:00Z").plusSeconds(60L * i);
      ics.append("BEGIN:VEVENT\r\nUID:out-")
          .append(i)
          .append("\r\nDTSTART:")
          .append(compact(start))
          .append("\r\nDTEND:")
          .append(compact(start.plusSeconds(60)))
          .append("\r\nEND:VEVENT\r\n");
    }
    return ics.append("END:VCALENDAR\r\n").toString();
  }

  static String compact(Instant instant) {
    return instant.toString().replace("-", "").replace(":", "");
  }
}
