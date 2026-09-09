package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.CalendarEntry;
import com.apptolast.organization.domain.CalendarFeedSecret;
import com.apptolast.organization.domain.CalendarSnapshot;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CalendarFeedUseCasesTest {
  private static final String ORIGIN = "https://organizacion.apptolast.com";
  private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
  private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

  /** In-memory stand-in for the single-row-per-owner table, with the same uniqueness rules. */
  static final class FakeTokens implements CalendarFeedTokens {
    record Row(byte[] fingerprint, Instant createdAt) {}

    final Map<String, Row> rows = new HashMap<>();
    final List<String> writes = new ArrayList<>();
    RuntimeException failure;

    @Override
    public Optional<Instant> createdAt(String owner) {
      if (failure != null) throw failure;
      return Optional.ofNullable(rows.get(owner)).map(Row::createdAt);
    }

    @Override
    public void replace(String owner, byte[] fingerprint, Instant createdAt) {
      writes.add("replace:" + owner);
      if (failure != null) throw failure;
      rows.put(owner, new Row(fingerprint, createdAt));
    }

    @Override
    public void revoke(String owner) {
      writes.add("revoke:" + owner);
      if (failure != null) throw failure;
      rows.remove(owner);
    }

    @Override
    public Optional<String> ownerOf(byte[] fingerprint) {
      if (failure != null) throw failure;
      return rows.entrySet().stream()
          .filter(entry -> java.util.Arrays.equals(entry.getValue().fingerprint(), fingerprint))
          .map(Map.Entry::getKey)
          .findFirst();
    }
  }

  static final class FakeCalendar implements CalendarQueries {
    final List<String> windows = new ArrayList<>();
    CalendarSnapshot snapshot = new CalendarSnapshot(Optional.empty(), List.of());
    RuntimeException failure;

    @Override
    public CalendarSnapshot read(String owner, Instant from, Instant to) {
      windows.add(owner + "|" + from + "|" + to);
      if (failure != null) throw failure;
      return snapshot;
    }
  }

  static ManageCalendarFeed manager(FakeTokens tokens) {
    return new ManageCalendarFeed(tokens, ORIGIN, FIXED, new SecureRandom());
  }

  @Test
  void s1_generateReturnsTheUrlOnceAndStoresOnlyTheFingerprint() {
    var tokens = new FakeTokens();
    var link = manager(tokens).generate("persona-a");
    var token =
        link.url().substring(ORIGIN.length() + "/calendar/".length(), link.url().length() - 4);
    assertEquals(ORIGIN + "/calendar/" + token + ".ics", link.url());
    assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
    assertEquals(NOW, link.createdAt());
    assertEquals(1, tokens.rows.size());
    assertArrayEquals(
        CalendarFeedSecret.fingerprintOf(token), tokens.rows.get("persona-a").fingerprint());
    assertEquals(32, tokens.rows.get("persona-a").fingerprint().length);
  }

  @Test
  void s2_statusWithoutTokenIsInactiveAndWritesNothing() {
    var tokens = new FakeTokens();
    assertEquals(new CalendarFeedStatus(false, null), manager(tokens).status("persona-a"));
    assertEquals(List.of(), tokens.writes);
  }

  @Test
  void s3_statusWithTokenExposesOnlyTheCreationInstant() {
    var tokens = new FakeTokens();
    var link = manager(tokens).generate("persona-a");
    var status = manager(tokens).status("persona-a");
    assertEquals(new CalendarFeedStatus(true, NOW), status);
    assertFalse(status.toString().contains(link.url()));
  }

  @Test
  void s4_regeneratingKeepsOneRowAndInvalidatesThePreviousToken() {
    var tokens = new FakeTokens();
    var first = manager(tokens).generate("persona-a");
    var later =
        new ManageCalendarFeed(
            tokens, ORIGIN, Clock.fixed(NOW.plusSeconds(300), ZoneOffset.UTC), new SecureRandom());
    var second = later.generate("persona-a");
    assertNotEquals(first.url(), second.url());
    assertEquals(NOW.plusSeconds(300), second.createdAt());
    assertEquals(1, tokens.rows.size());
    assertTrue(tokens.ownerOf(CalendarFeedSecret.fingerprintOf(tokenOf(first.url()))).isEmpty());
    assertEquals(
        Optional.of("persona-a"),
        tokens.ownerOf(CalendarFeedSecret.fingerprintOf(tokenOf(second.url()))));
  }

  static String tokenOf(String url) {
    return url.substring(url.lastIndexOf('/') + 1, url.length() - 4);
  }

  @Test
  void s5_revokeIsIdempotentAndLeavesNoRow() {
    var tokens = new FakeTokens();
    manager(tokens).generate("persona-a");
    manager(tokens).revoke("persona-a");
    manager(tokens).revoke("persona-a");
    assertEquals(Map.of(), tokens.rows);
    assertEquals(new CalendarFeedStatus(false, null), manager(tokens).status("persona-a"));
  }

  @Test
  void s10_aFailedWriteIsAttemptedOnceAndNeverRetried() {
    var tokens = new FakeTokens();
    manager(tokens).generate("persona-a");
    tokens.writes.clear();
    tokens.failure = new StorageUnavailableException(new IllegalStateException("unique"));
    assertThrows(StorageUnavailableException.class, () -> manager(tokens).generate("persona-a"));
    assertEquals(List.of("replace:persona-a"), tokens.writes);
    assertEquals(1, tokens.rows.size());
  }

  static RenderCalendar renderer(FakeTokens tokens, FakeCalendar calendar) {
    return new RenderCalendar(tokens, calendar, ORIGIN, FIXED);
  }

  @Test
  void s18_theWindowIsThirtyDaysBackAndThreeHundredSixtyFiveDaysForward() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    var link = manager(tokens).generate("persona-a");
    renderer(tokens, calendar).forToken(tokenOf(link.url()));
    assertEquals(List.of("persona-a|2026-08-09T12:00:00Z|2027-09-08T12:00:00Z"), calendar.windows);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "corto", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
  void s15_anUnknownCandidateIsNotFoundAndNeverReachesTheCalendar(String candidate) {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    manager(tokens).generate("persona-a");
    assertThrows(
        CalendarNotFoundException.class, () -> renderer(tokens, calendar).forToken(candidate));
    assertEquals(List.of(), calendar.windows);
  }

  @Test
  void s27_atokenOnlyResolvesTheFeedOfItsOwner() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    var a = manager(tokens).generate("persona-a");
    var b = manager(tokens).generate("persona-b");
    renderer(tokens, calendar).forToken(tokenOf(a.url()));
    renderer(tokens, calendar).forToken(tokenOf(b.url()));
    assertEquals(
        List.of(
            "persona-a|2026-08-09T12:00:00Z|2027-09-08T12:00:00Z",
            "persona-b|2026-08-09T12:00:00Z|2027-09-08T12:00:00Z"),
        calendar.windows);
  }

  @Test
  void s17_theDownloadRendersTheOwnerFeedWithoutTouchingAnyToken() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    var document = renderer(tokens, calendar).forOwner("persona-a");
    assertTrue(document.startsWith("BEGIN:VCALENDAR\r\n"));
    assertTrue(document.endsWith("END:VCALENDAR\r\n"));
    assertEquals(List.of(), tokens.writes);
    assertEquals(Map.of(), tokens.rows);
  }

  static CalendarEntry entry(int index) {
    var start = Instant.parse("2026-10-20T08:00:00Z").plusSeconds(index * 3600L);
    return new CalendarEntry(
        new UUID(0, index),
        new UUID(1, 1),
        new UUID(2, 2),
        "T",
        "O",
        start,
        start.plusSeconds(1800),
        1,
        NOW);
  }

  static CalendarSnapshot snapshotOf(int events) {
    var entries = new ArrayList<CalendarEntry>();
    for (int index = 0; index < events; index++) entries.add(entry(index));
    return new CalendarSnapshot(Optional.empty(), entries);
  }

  @Test
  void s26_twoThousandEventsStillRender() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    calendar.snapshot = snapshotOf(2000);
    var link = manager(tokens).generate("persona-a");
    var document = renderer(tokens, calendar).forToken(tokenOf(link.url()));
    assertEquals(2000, document.split("BEGIN:VEVENT", -1).length - 1);
  }

  @Test
  void s26_moreThanTwoThousandEventsAreRefusedBeforeAnyDocumentExists() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    calendar.snapshot = snapshotOf(2001);
    var link = manager(tokens).generate("persona-a");
    var renderer = renderer(tokens, calendar);
    var token = tokenOf(link.url());
    var error = assertThrows(CalendarTooLargeException.class, () -> renderer.forToken(token));
    assertFalse(error.getMessage().contains(token));
    assertThrows(CalendarTooLargeException.class, () -> renderer.forOwner("persona-a"));
  }

  @Test
  void s30_anUnavailableStoreNeverProducesAPartialDocument() {
    var tokens = new FakeTokens();
    var calendar = new FakeCalendar();
    var link = manager(tokens).generate("persona-a");
    calendar.failure = new StorageUnavailableException(new IllegalStateException("down"));
    var renderer = renderer(tokens, calendar);
    var token = tokenOf(link.url());
    assertThrows(StorageUnavailableException.class, () -> renderer.forToken(token));
    assertThrows(StorageUnavailableException.class, () -> renderer.forOwner("persona-a"));
  }
}
