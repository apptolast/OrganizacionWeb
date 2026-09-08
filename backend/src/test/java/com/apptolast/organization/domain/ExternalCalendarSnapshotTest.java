package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalCalendarSnapshotTest {
  static final Instant SYNC_AT = Instant.parse("2030-01-07T12:00:00Z");

  static ExternalEvent event(String uid, String start, String end) {
    return new ExternalEvent(uid, uid, Instant.parse(start), Instant.parse(end), false);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2030-01-06T11:00:00Z,2030-01-06T12:00:00Z,0",
    "2030-01-06T11:30:00Z,2030-01-06T12:30:00Z,1",
    "2030-01-06T12:00:00Z,2030-01-06T13:00:00Z,1",
    "2030-01-21T11:30:00Z,2030-01-21T12:30:00Z,1",
    "2030-01-21T12:00:00Z,2030-01-21T13:00:00Z,0",
    "2029-12-01T00:00:00Z,2030-02-01T00:00:00Z,1"
  })
  void s23_keepsOnlyEventsIntersectingTheHalfOpenWindow(String start, String end, int stored) {
    var selection = ExternalCalendarSnapshot.select(List.of(event("u", start, end)), SYNC_AT);
    assertEquals(stored, selection.events().size());
    assertFalse(selection.truncated());
  }

  @Test
  void s24_truncatesTo500OrderedByStartThenUid() {
    var events = new ArrayList<ExternalEvent>();
    for (int i = 0; i < 600; i++) {
      var start = SYNC_AT.plus(Duration.ofMinutes(i));
      events.add(new ExternalEvent("e%03d".formatted(i), "", start, start.plusSeconds(60), false));
    }
    var shared = SYNC_AT.plus(Duration.ofMinutes(5));
    events.set(5, new ExternalEvent("b", "", shared, shared.plusSeconds(60), false));
    events.set(6, new ExternalEvent("a", "", shared, shared.plusSeconds(60), false));
    for (int i = 0; i < 50; i++) {
      var start = SYNC_AT.plus(Duration.ofDays(20)).plus(Duration.ofMinutes(i));
      events.add(
          new ExternalEvent("far%02d".formatted(i), "", start, start.plusSeconds(60), false));
    }
    java.util.Collections.reverse(events);
    var selection = ExternalCalendarSnapshot.select(events, SYNC_AT);
    assertTrue(selection.truncated());
    assertEquals(500, selection.events().size());
    assertEquals("e000", selection.events().getFirst().uid());
    assertEquals("a", selection.events().get(5).uid());
    assertEquals("b", selection.events().get(6).uid());
    assertEquals("e499", selection.events().getLast().uid());
    var exact = ExternalCalendarSnapshot.select(events.subList(50, 550), SYNC_AT);
    assertFalse(exact.truncated());
    assertEquals(500, exact.events().size());
  }
}
