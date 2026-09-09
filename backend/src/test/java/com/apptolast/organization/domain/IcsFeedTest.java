package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IcsFeedTest {
  static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
  static final Set<String> ZONES = Set.of("UTC", "Europe/Madrid");

  static String calendar(String... components) {
    return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Test//EN\r\n"
        + String.join("", components)
        + "END:VCALENDAR\r\n";
  }

  static String event(String... lines) {
    return "BEGIN:VEVENT\r\n" + String.join("\r\n", lines) + "\r\nEND:VEVENT\r\n";
  }

  @Test
  void s14_utcEventKeepsExactInstants() {
    var parsed =
        IcsFeed.parse(
            calendar(
                event(
                    "UID:u1@example",
                    "DTSTART:20300108T090000Z",
                    "DTEND:20300108T100000Z",
                    "SUMMARY:Reunión")),
            MADRID,
            ZONES);
    assertEquals(
        List.of(
            new ExternalEvent(
                "u1@example",
                "Reunión",
                Instant.parse("2030-01-08T09:00:00Z"),
                Instant.parse("2030-01-08T10:00:00Z"),
                false)),
        parsed.events());
    assertEquals(0, parsed.skippedRecurring());
    assertEquals(0, parsed.skippedCancelled());
    assertEquals(0, parsed.skippedInvalid());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "20260115T100000,20260115T110000,2026-01-15T09:00:00Z,2026-01-15T10:00:00Z",
    "20260715T100000,20260715T110000,2026-07-15T08:00:00Z,2026-07-15T09:00:00Z",
    "20260329T013000,20260329T033000,2026-03-29T00:30:00Z,2026-03-29T01:30:00Z",
    "20260329T100000,20260329T110000,2026-03-29T08:00:00Z,2026-03-29T09:00:00Z",
    "20261025T013000,20261025T040000,2026-10-24T23:30:00Z,2026-10-25T03:00:00Z",
    "20261025T100000,20261025T110000,2026-10-25T09:00:00Z,2026-10-25T10:00:00Z"
  })
  void s15_tzidMadridUsesTheRealOffsetOfEachDate(
      String start, String end, String startAt, String endAt) {
    var parsed =
        IcsFeed.parse(
            calendar(
                event(
                    "UID:tz",
                    "DTSTART;TZID=Europe/Madrid:" + start,
                    "DTEND;TZID=Europe/Madrid:" + end)),
            ZoneId.of("UTC"),
            ZONES);
    var stored = parsed.events().getFirst();
    assertEquals(Instant.parse(startAt), stored.startAt());
    assertEquals(Instant.parse(endAt), stored.endAt());
    assertFalse(stored.allDay());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "Europe/Madrid,DTSTART;VALUE=DATE:20260329,,2026-03-28T23:00:00Z,2026-03-29T22:00:00Z,true",
    "Europe/Madrid,DTSTART;VALUE=DATE:20260105,DTEND;VALUE=DATE:20260107,2026-01-04T23:00:00Z,2026-01-06T23:00:00Z,true",
    "UTC,DTSTART;VALUE=DATE:20260105,,2026-01-05T00:00:00Z,2026-01-06T00:00:00Z,true",
    "Europe/Madrid,DTSTART:20260115T100000,DTEND:20260115T110000,2026-01-15T09:00:00Z,2026-01-15T10:00:00Z,false",
    "UTC,DTSTART:20260115T100000,DTEND:20260115T110000,2026-01-15T10:00:00Z,2026-01-15T11:00:00Z,false"
  })
  void s16_allDayAndFloatingResolveInTheSnapshotZone(
      String zone, String start, String end, String startAt, String endAt, boolean allDay) {
    var parsed =
        IcsFeed.parse(
            calendar(event("UID:day", start, end == null ? "SUMMARY:x" : end)),
            ZoneId.of(zone),
            ZONES);
    var stored = parsed.events().getFirst();
    assertEquals(Instant.parse(startAt), stored.startAt());
    assertEquals(Instant.parse(endAt), stored.endAt());
    assertEquals(allDay, stored.allDay());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "DURATION:PT1H30M,1,0,2030-01-08T10:30:00Z",
    "DURATION:P1D,1,0,2030-01-09T09:00:00Z",
    "SUMMARY:sin fin,0,1,",
    "DTEND:20300108T090000Z,0,1,",
    "DTEND:20300108T085900Z,0,1,",
    "DURATION:PT0S,0,1,"
  })
  void s17_durationComputesTheEndAndNonPositiveEndsAreInvalid(
      String rest, int imported, int invalid, String endAt) {
    var parsed =
        IcsFeed.parse(calendar(event("UID:d1", "DTSTART:20300108T090000Z", rest)), MADRID, ZONES);
    assertEquals(imported, parsed.events().size());
    assertEquals(invalid, parsed.skippedInvalid());
    if (endAt != null) assertEquals(Instant.parse(endAt), parsed.events().getFirst().endAt());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "STATUS:CANCELLED,cancelled",
    "RRULE:FREQ=WEEKLY;COUNT=10,recurring",
    "RDATE:20300109T090000Z,recurring",
    "RECURRENCE-ID:20300108T090000Z,recurring",
    "STATUS:CANCELLED|RRULE:FREQ=DAILY,recurring"
  })
  void s18_cancelledAndRecurringEventsAreCountedNotStored(String property, String counter) {
    var parsed =
        IcsFeed.parse(
            calendar(
                event("UID:ok1", "DTSTART:20300108T090000Z", "DTEND:20300108T100000Z"),
                event("UID:ok2", "DTSTART:20300108T110000Z", "DTEND:20300108T120000Z"),
                event(
                    ("UID:x1|DTSTART:20300108T130000Z|DTEND:20300108T140000Z|" + property)
                        .split("[|]"))),
            MADRID,
            ZONES);
    assertEquals(List.of("ok1", "ok2"), parsed.events().stream().map(ExternalEvent::uid).toList());
    assertEquals(counter.equals("recurring") ? 1 : 0, parsed.skippedRecurring());
    assertEquals(counter.equals("cancelled") ? 1 : 0, parsed.skippedCancelled());
    assertEquals(0, parsed.skippedInvalid());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "DTSTART:20260115T100000Z|DTEND:20260115T110000Z",
        "UID:bad|DTEND:20260115T110000Z",
        "UID:bad|DTSTART:20260230T100000Z|DTEND:20260302T100000Z",
        "UID:bad|DTSTART:20260115T256000Z|DTEND:20260116T110000Z",
        "UID:bad|DTSTART;TZID=Romance Standard Time:20260115T100000|DTEND;TZID=Romance Standard Time:20260115T110000",
        "UID:bad|DTSTART;TZID=Marte/Base:20260115T100000|DTEND;TZID=Marte/Base:20260115T110000",
        "UID:bad|DTSTART:20260115|DTEND:20260116",
        "UID:bad|DTSTART;VALUE=DATE:20260115|DTEND;VALUE=DATE:20260115",
        "UID:bad|DTSTART:20260115T100000Z|DURATION:P1W",
        "UID: |DTSTART:20260115T100000Z|DTEND:20260115T110000Z"
      })
  void s21_uninterpretableEventsCountAsInvalid(String body) {
    var parsed =
        IcsFeed.parse(
            calendar(
                event("UID:ok", "DTSTART:20260115T100000Z", "DTEND:20260115T110000Z"),
                event(body.split("[|]"))),
            MADRID,
            ZONES);
    assertEquals(List.of("ok"), parsed.events().stream().map(ExternalEvent::uid).toList());
    assertEquals(1, parsed.skippedInvalid());
    assertEquals(0, parsed.skippedRecurring());
    assertEquals(0, parsed.skippedCancelled());
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> summaries() {
    return java.util.stream.Stream.of(
        org.junit.jupiter.params.provider.Arguments.of(
            "SUMMARY:Reunión con\\, el equipo\\; sala 2\\\\n", "Reunión con, el equipo; sala 2\\n"),
        org.junit.jupiter.params.provider.Arguments.of(
            "SUMMARY:Reu\r\n nión larga", "Reunión larga"),
        org.junit.jupiter.params.provider.Arguments.of("SUMMARY:Reu\n\tnión", "Reunión"),
        org.junit.jupiter.params.provider.Arguments.of("SUMMARY:  con espacios  ", "con espacios"),
        org.junit.jupiter.params.provider.Arguments.of("X-NOTHING:1", ""),
        org.junit.jupiter.params.provider.Arguments.of(
            "SUMMARY:" + "é".repeat(501), "é".repeat(500)),
        org.junit.jupiter.params.provider.Arguments.of(
            "SUMMARY:" + "😀".repeat(501), "😀".repeat(500)),
        org.junit.jupiter.params.provider.Arguments.of("SUMMARY:a\\\\\\\\b", "a\\\\b"),
        org.junit.jupiter.params.provider.Arguments.of("SUMMARY:línea\\Notra", "línea\notra"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("summaries")
  void s19_unfoldsLinesAndUnescapesSummary(String raw, String summary) {
    var parsed =
        IcsFeed.parse(
            calendar(event("UID:s", "DTSTART:20300108T090000Z", "DTEND:20300108T100000Z", raw)),
            MADRID,
            ZONES);
    assertEquals(summary, parsed.events().getFirst().summary());
  }

  @Test
  void s20_firstRepeatedUidWinsAndTheRestCountAsInvalid() {
    var parsed =
        IcsFeed.parse(
            calendar(
                event(
                    "UID:dup",
                    "DTSTART:20300108T090000Z",
                    "DTEND:20300108T100000Z",
                    "SUMMARY:Primero"),
                event(
                    "UID:dup",
                    "DTSTART:20300108T110000Z",
                    "DTEND:20300108T120000Z",
                    "SUMMARY:Segundo"),
                event(
                    "UID:dup",
                    "DTSTART:20300108T130000Z",
                    "DTEND:20300108T140000Z",
                    "SUMMARY:Tercero")),
            MADRID,
            ZONES);
    assertEquals(1, parsed.events().size());
    assertEquals("Primero", parsed.events().getFirst().summary());
    assertEquals(2, parsed.skippedInvalid());
  }

  @Test
  void s22_ignoresComponentsOtherThanVevent() {
    var parsed =
        IcsFeed.parse(
            calendar(
                "BEGIN:VTIMEZONE\r\nTZID:Europe/Madrid\r\nBEGIN:STANDARD\r\nDTSTART:19701025T030000\r\nTZOFFSETFROM:+0200\r\nTZOFFSETTO:+0100\r\nEND:STANDARD\r\nEND:VTIMEZONE\r\n",
                "BEGIN:VTODO\r\nUID:todo\r\nDTSTART:20300108T090000Z\r\nDTEND:20300108T100000Z\r\nEND:VTODO\r\n",
                "BEGIN:VEVENT\r\nUID:ok\r\nDTSTART:20300108T090000Z\r\nDTEND:20300108T100000Z\r\nBEGIN:VALARM\r\nACTION:DISPLAY\r\nTRIGGER:-PT10M\r\nUID:alarm\r\nEND:VALARM\r\nEND:VEVENT\r\n",
                "BEGIN:X-DESCONOCIDO\r\nUID:x\r\nDTSTART:20300108T090000Z\r\nEND:X-DESCONOCIDO\r\n"),
            MADRID,
            ZONES);
    assertEquals(List.of("ok"), parsed.events().stream().map(ExternalEvent::uid).toList());
    assertEquals(0, parsed.skippedInvalid());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "<html><body>login</body></html>",
        "BEGIN:VEVENT\r\nUID:x\r\nEND:VEVENT",
        "",
        "   "
      })
  void s12_feedsWithoutVcalendarAreMalformed(String text) {
    assertThrows(IcsFeedMalformedException.class, () -> IcsFeed.parse(text, MADRID, ZONES));
  }

  static String fixture(String name) throws java.io.IOException {
    try (var stream = IcsFeedTest.class.getResourceAsStream("/ics/" + name)) {
      return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  @Test
  void googleCalendarExportImportsUtcTzidAllDayAndCountsTheRest() throws Exception {
    var parsed = IcsFeed.parse(fixture("google-calendar.ics"), MADRID, ZONES);
    assertEquals(
        List.of("utc-1@google.com", "madrid-2@google.com", "allday-3@google.com"),
        parsed.events().stream().map(ExternalEvent::uid).toList());
    assertEquals("Revisión semanal, con Ana; sala 2", parsed.events().getFirst().summary());
    assertEquals(Instant.parse("2030-01-08T08:00:00Z"), parsed.events().get(1).startAt());
    assertTrue(parsed.events().get(2).allDay());
    assertEquals(2, parsed.skippedRecurring());
    assertEquals(1, parsed.skippedCancelled());
    assertEquals(0, parsed.skippedInvalid());
  }

  @Test
  void outlookExportRejectsWindowsZoneNamesAsInvalid() throws Exception {
    var parsed = IcsFeed.parse(fixture("outlook.ics"), MADRID, ZONES);
    assertEquals(
        List.of("utc-ok@outlook.com"), parsed.events().stream().map(ExternalEvent::uid).toList());
    assertEquals(1, parsed.skippedInvalid());
  }
}
