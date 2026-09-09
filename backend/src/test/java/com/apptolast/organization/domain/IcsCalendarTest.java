package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class IcsCalendarTest {
  private static final String ORIGIN = "https://organizacion.apptolast.com";

  @Test
  void s13_emptyCalendarIsSevenCrlfLinesOf158Octets() {
    var document = IcsCalendar.render(ORIGIN, new CalendarSnapshot(Optional.empty(), List.of()));
    assertEquals(
        "BEGIN:VCALENDAR\r\n"
            + "VERSION:2.0\r\n"
            + "PRODID:-//apptolast//OrganizationWeb//ES\r\n"
            + "CALSCALE:GREGORIAN\r\n"
            + "METHOD:PUBLISH\r\n"
            + "X-WR-CALNAME:Bloques planificados\r\n"
            + "END:VCALENDAR\r\n",
        document);
    assertEquals(158, document.getBytes(StandardCharsets.UTF_8).length);
  }

  @Test
  void s14_timezoneLineFollowsCalendarNameOnlyWhenAvailabilityHasZone() {
    var document =
        IcsCalendar.render(ORIGIN, new CalendarSnapshot(Optional.of("America/Bogota"), List.of()));
    var lines = document.split("\r\n", -1);
    assertEquals("X-WR-CALNAME:Bloques planificados", lines[5]);
    assertEquals("X-WR-TIMEZONE:America/Bogota", lines[6]);
    assertEquals("END:VCALENDAR", lines[7]);
  }

  static final CalendarEntry BLOCK_B =
      new CalendarEntry(
          UUID.fromString("3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f"),
          UUID.fromString("0f2b3a1c-9d8e-4f70-a1b2-c3d4e5f60718"),
          UUID.fromString("7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e"),
          "Revisión; plan, fase 2",
          "Cerrar conclusiones y anotar las dudas pendientes de esa sección\n"
              + "Segundo paso: enviar la versión final a revisión\\externa; sin adjuntos, sólo texto",
          Instant.parse("2026-10-25T08:00:00Z"),
          Instant.parse("2026-10-25T09:30:00Z"),
          1,
          Instant.parse("2026-09-01T09:15:30.123456Z"));

  static final String DOCUMENT_B =
      "BEGIN:VCALENDAR\r\n"
          + "VERSION:2.0\r\n"
          + "PRODID:-//apptolast//OrganizationWeb//ES\r\n"
          + "CALSCALE:GREGORIAN\r\n"
          + "METHOD:PUBLISH\r\n"
          + "X-WR-CALNAME:Bloques planificados\r\n"
          + "X-WR-TIMEZONE:Europe/Madrid\r\n"
          + "BEGIN:VEVENT\r\n"
          + "UID:3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f@organizacion.apptolast.com\r\n"
          + "DTSTAMP:20260901T091530Z\r\n"
          + "DTSTART:20261025T080000Z\r\n"
          + "DTEND:20261025T093000Z\r\n"
          + "SUMMARY:Revisión\\; plan\\, fase 2\r\n"
          + "DESCRIPTION:Cerrar conclusiones y anotar las dudas pendientes de esa secci\r\n"
          + " ón\\nSegundo paso: enviar la versión final a revisión\\\\externa\\; sin adj\r\n"
          + " untos\\, sólo texto\r\n"
          + "SEQUENCE:1\r\n"
          + "URL:https://organizacion.apptolast.com/proyectos/0f2b3a1c-9d8e-4f70-a1b2-c3\r\n"
          + " d4e5f60718/tareas/7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e\r\n"
          + "END:VEVENT\r\n"
          + "END:VCALENDAR\r\n";

  @Test
  void s12_singleBlockDocumentMatchesRfc5545ByteForByte() {
    var document =
        IcsCalendar.render(
            ORIGIN, new CalendarSnapshot(Optional.of("Europe/Madrid"), List.of(BLOCK_B)));
    assertEquals(DOCUMENT_B, document);
    var bytes = document.getBytes(StandardCharsets.UTF_8);
    assertEquals(714, bytes.length);
    assertEquals(21, document.split("\r\n", -1).length - 1);
    var physical = document.split("\r\n", -1);
    assertEquals(74, physical[13].getBytes(StandardCharsets.UTF_8).length);
    assertEquals(75, physical[14].getBytes(StandardCharsets.UTF_8).length);
    assertEquals(20, physical[15].getBytes(StandardCharsets.UTF_8).length);
    assertEquals(75, physical[17].getBytes(StandardCharsets.UTF_8).length);
    assertEquals(55, physical[18].getBytes(StandardCharsets.UTF_8).length);
    assertFalse(document.contains("TZID"));
    assertFalse(document.contains("VTIMEZONE"));
    assertFalse(document.contains("STATUS"));
    assertFalse(document.contains("CREATED"));
  }

  static CalendarEntry withObjective(String objective) {
    return new CalendarEntry(
        BLOCK_B.blockId(),
        BLOCK_B.projectId(),
        BLOCK_B.taskId(),
        BLOCK_B.title(),
        objective,
        BLOCK_B.startAt(),
        BLOCK_B.endAt(),
        BLOCK_B.version(),
        BLOCK_B.stampedAt());
  }

  static String description(String document) {
    var unfolded = document.replace("\r\n ", "");
    var start = unfolded.indexOf("DESCRIPTION:") + "DESCRIPTION:".length();
    return unfolded.substring(start, unfolded.indexOf("\r\n", start));
  }

  /** Reader-side unescape per RFC 5545 §3.3.11: a backslash pair becomes one backslash. */
  static String unescape(String value) {
    var out = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c != '\\') {
        out.append(c);
        continue;
      }
      char next = value.charAt(++i);
      out.append(next == 'n' || next == 'N' ? '\n' : next);
    }
    return out.toString();
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "a{BS}b | a{BS}{BS}b | a{BS}b",
        "a;b | a{BS};b | a;b",
        "a,b | a{BS},b | a,b",
        "a{LF}b | a{BS}nb | a{LF}b",
        "a{CR}{LF}b | a{BS}nb | a{LF}b",
        "a{CR}b | ab | ab",
        "a:b \"c\" d | a:b \"c\" d | a:b \"c\" d"
      })
  void s23_textValuesEscapeAndRoundTrip(String persisted, String serialized, String recovered) {
    var document =
        IcsCalendar.render(
            ORIGIN,
            new CalendarSnapshot(Optional.empty(), List.of(withObjective(unquote(persisted)))));
    assertEquals(unquote(serialized), description(document));
    assertEquals(unquote(recovered), unescape(description(document)));
  }

  private static String unquote(String cell) {
    return cell.replace("{LF}", "\n").replace("{CR}", "\r").replace("{BS}", "\\");
  }

  @Test
  void s23_fiveHundredCodePointsWithMultibyteSurviveFoldingAndUnescaping() {
    var objective = "ñ😀,".repeat(166) + "ñ;";
    assertEquals(500, objective.codePointCount(0, objective.length()));
    var document =
        IcsCalendar.render(
            ORIGIN, new CalendarSnapshot(Optional.empty(), List.of(withObjective(objective))));
    assertEquals(objective, unescape(description(document)));
    for (var physical : document.split("\r\n"))
      assertTrue(physical.getBytes(StandardCharsets.UTF_8).length <= 75, physical);
  }

  static List<String> physicalDescriptionLines(String document) {
    var lines = List.of(document.split("\r\n"));
    int start = -1;
    for (int i = 0; i < lines.size(); i++) if (lines.get(i).startsWith("DESCRIPTION:")) start = i;
    int end = start + 1;
    while (lines.get(end).startsWith(" ")) end++;
    return lines.subList(start, end);
  }

  @Test
  void s24_lineOfExactly75OctetsIsNotFolded() {
    var document =
        IcsCalendar.render(
            ORIGIN, new CalendarSnapshot(Optional.empty(), List.of(withObjective("a".repeat(63)))));
    var lines = physicalDescriptionLines(document);
    assertEquals(1, lines.size());
    assertEquals(75, lines.get(0).getBytes(StandardCharsets.UTF_8).length);
  }

  @Test
  void s24_lineOf76OctetsFoldsIntoSeventyFivePlusSpaceAndOneCharacter() {
    var document =
        IcsCalendar.render(
            ORIGIN,
            new CalendarSnapshot(Optional.empty(), List.of(withObjective("a".repeat(63) + "b"))));
    var lines = physicalDescriptionLines(document);
    assertEquals(List.of("DESCRIPTION:" + "a".repeat(63), " b"), lines);
  }

  @Test
  void s24_twoOctetCharacterAtOctet75MovesWholeToContinuation() {
    var document =
        IcsCalendar.render(
            ORIGIN,
            new CalendarSnapshot(Optional.empty(), List.of(withObjective("a".repeat(62) + "óz"))));
    var lines = physicalDescriptionLines(document);
    assertEquals(74, lines.get(0).getBytes(StandardCharsets.UTF_8).length);
    assertEquals(" óz", lines.get(1));
  }

  @Test
  void s24_fourOctetCharacterAtOctets73To76MovesWholeToContinuation() {
    var document =
        IcsCalendar.render(
            ORIGIN,
            new CalendarSnapshot(Optional.empty(), List.of(withObjective("a".repeat(60) + "😀z"))));
    var lines = physicalDescriptionLines(document);
    assertEquals(72, lines.get(0).getBytes(StandardCharsets.UTF_8).length);
    assertEquals(" 😀z", lines.get(1));
  }

  @Test
  void s24_escapeStartingAtOctet75IsKeptWhole() {
    var document =
        IcsCalendar.render(
            ORIGIN,
            new CalendarSnapshot(Optional.empty(), List.of(withObjective("a".repeat(62) + ",z"))));
    var lines = physicalDescriptionLines(document);
    assertEquals(74, lines.get(0).getBytes(StandardCharsets.UTF_8).length);
    assertEquals(" \\,z", lines.get(1));
  }

  static CalendarEntry startingAt(String start, String blockId) {
    return new CalendarEntry(
        UUID.fromString(blockId),
        BLOCK_B.projectId(),
        BLOCK_B.taskId(),
        "T",
        "O",
        Instant.parse(start),
        Instant.parse(start).plusSeconds(3600),
        1,
        BLOCK_B.stampedAt());
  }

  @Test
  void s25_eventsAreOrderedByStartThenUidRegardlessOfInput() {
    var e1 = startingAt("2026-10-20T08:00:00Z", "00000000-0000-4000-8000-000000000002");
    var e2 = startingAt("2026-10-20T08:00:00Z", "00000000-0000-4000-8000-000000000001");
    var e3 = startingAt("2026-10-19T08:00:00Z", "00000000-0000-4000-8000-000000000003");
    var snapshot = new CalendarSnapshot(Optional.empty(), List.of(e1, e2, e3));
    var document = IcsCalendar.render(ORIGIN, snapshot);
    var uids =
        java.util.Arrays.stream(document.split("\r\n"))
            .filter(line -> line.startsWith("UID:"))
            .toList();
    assertEquals(
        List.of(
            "UID:" + e3.blockId() + "@organizacion.apptolast.com",
            "UID:" + e2.blockId() + "@organizacion.apptolast.com",
            "UID:" + e1.blockId() + "@organizacion.apptolast.com"),
        uids);
    assertEquals(document, IcsCalendar.render(ORIGIN, snapshot));
  }

  @Test
  void s22_sameLocalTimeAcrossDstChangeRendersDistinctUtcInstants() {
    var d1 = startingAt("2026-10-24T07:00:00Z", "00000000-0000-4000-8000-0000000000d1");
    var d2 = startingAt("2026-10-25T08:00:00Z", "00000000-0000-4000-8000-0000000000d2");
    var document =
        IcsCalendar.render(ORIGIN, new CalendarSnapshot(Optional.empty(), List.of(d1, d2)));
    assertTrue(document.contains("DTSTART:20261024T070000Z\r\nDTEND:20261024T080000Z"));
    assertTrue(document.contains("DTSTART:20261025T080000Z\r\nDTEND:20261025T090000Z"));
    assertFalse(document.contains("TZID"));
    for (var line : document.split("\r\n"))
      if (line.startsWith("DT")) assertTrue(line.endsWith("Z"), line);
  }
}
