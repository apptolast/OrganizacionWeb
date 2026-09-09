package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.ExternalEventsRange;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.SyncSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** @s1, @s31, @s33. */
class ReadExternalCalendarEventsTest {
  static final String A = "persona-a";
  static final String B = "persona-b";
  static final Instant SYNCED = Instant.parse("2030-01-07T11:00:00Z");
  static final ExternalEventsRange DAY =
      ExternalEventsRange.of("2030-01-07T00:00:00Z", "2030-01-08T00:00:00Z");

  InMemoryExternalCalendarStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryExternalCalendarStore();
  }

  static ExternalEvent event(String uid, String summary, String startAt, String endAt) {
    return new ExternalEvent(uid, summary, Instant.parse(startAt), Instant.parse(endAt), false);
  }

  void subscribe(String owner, List<ExternalEvent> events) {
    store.create(
        owner,
        UUID.randomUUID(),
        ExternalCalendarInput.of("Trabajo", "https://feed.example.test/a.ics"),
        new byte[] {1, 2, 3},
        SYNCED);
    store.commitSuccess(
        owner, 0, new SyncSummary("Europe/Madrid", events.size(), 0, 0, 0, false), events, SYNCED);
  }

  @Test
  void s1_withoutASubscriptionEverythingIsEmptyAndNothingIsConfigured() {
    var view = new ReadExternalCalendarEvents(store).execute(A, DAY);
    assertFalse(view.configured());
    assertNull(view.lastSyncAt());
    assertNull(view.lastStatus());
    assertTrue(view.items().isEmpty());
  }

  @Test
  void s31_onlyTheEventsThatIntersectTheHalfOpenRangeAreReturnedInOrder() {
    subscribe(
        A,
        List.of(
            event("b", "A", "2030-01-07T08:00:00Z", "2030-01-07T09:00:00Z"),
            event("a", "E", "2030-01-07T08:00:00Z", "2030-01-07T09:00:00Z"),
            event("c", "B", "2030-01-07T23:30:00Z", "2030-01-08T00:30:00Z"),
            event("d", "C", "2030-01-08T00:00:00Z", "2030-01-08T01:00:00Z"),
            event("e", "D", "2030-01-06T23:00:00Z", "2030-01-07T00:00:00Z")));
    var view = new ReadExternalCalendarEvents(store).execute(A, DAY);
    assertTrue(view.configured());
    assertEquals(SYNCED, view.lastSyncAt());
    assertEquals(SyncStatus.OK, view.lastStatus());
    assertEquals(List.of("E", "A", "B"), view.items().stream().map(ExternalEvent::summary).toList());
  }

  @Test
  void s33_eachOwnerOnlySeesItsOwnEvents() {
    subscribe(A, List.of(event("u1", "De A", "2030-01-07T08:00:00Z", "2030-01-07T09:00:00Z")));
    subscribe(B, List.of(event("u1", "De B", "2030-01-07T10:00:00Z", "2030-01-07T11:00:00Z")));
    var read = new ReadExternalCalendarEvents(store);
    assertEquals(List.of("De A"), read.execute(A, DAY).items().stream().map(ExternalEvent::summary).toList());
    assertEquals(List.of("De B"), read.execute(B, DAY).items().stream().map(ExternalEvent::summary).toList());
  }

  @Test
  void s12_aFailedSyncStillPublishesThePreviousListWithItsStatus() {
    subscribe(A, List.of(event("u1", "Anterior", "2030-01-07T08:00:00Z", "2030-01-07T09:00:00Z")));
    store.commitFailure(A, 1, com.apptolast.organization.domain.FeedError.FEED_HTTP_ERROR, Instant.parse("2030-01-07T12:00:00Z"));
    var view = new ReadExternalCalendarEvents(store).execute(A, DAY);
    assertEquals(SyncStatus.FAILED, view.lastStatus());
    assertEquals(SYNCED, view.lastSyncAt());
    assertEquals(1, view.items().size());
  }

  @Test
  void s1_aSubscriptionWithoutAnySyncIsConfiguredWithAnEmptyList() {
    store.create(
        A,
        UUID.randomUUID(),
        ExternalCalendarInput.of("Trabajo", "https://feed.example.test/a.ics"),
        new byte[] {1, 2, 3},
        SYNCED);
    var view = new ReadExternalCalendarEvents(store).execute(A, DAY);
    assertTrue(view.configured());
    assertNull(view.lastSyncAt());
    assertNull(view.lastStatus());
    assertTrue(view.items().isEmpty());
  }
}
