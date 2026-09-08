package com.apptolast.organization.domain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class IcsCalendar {
  private static final String CRLF = "\r\n";
  private static final int MAX_LINE_OCTETS = 75;
  private static final DateTimeFormatter UTC_STAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  private IcsCalendar() {}

  public static String render(String publicOrigin, CalendarSnapshot snapshot) {
    var document = new StringBuilder();
    line(document, "BEGIN:VCALENDAR");
    line(document, "VERSION:2.0");
    line(document, "PRODID:-//apptolast//OrganizationWeb//ES");
    line(document, "CALSCALE:GREGORIAN");
    line(document, "METHOD:PUBLISH");
    line(document, "X-WR-CALNAME:Bloques planificados");
    snapshot.zoneId().ifPresent(zone -> line(document, "X-WR-TIMEZONE:" + zone));
    var host = URI.create(publicOrigin).getHost();
    for (var entry : ordered(snapshot.entries())) event(document, publicOrigin, host, entry);
    line(document, "END:VCALENDAR");
    return document.toString();
  }

  /** RFC 5545 leaves order free; the contract fixes DTSTART and then UID so bytes are stable. */
  private static List<CalendarEntry> ordered(List<CalendarEntry> entries) {
    return entries.stream()
        .sorted(
            Comparator.comparing(CalendarEntry::startAt)
                .thenComparing(entry -> entry.blockId().toString()))
        .toList();
  }

  private static void event(StringBuilder document, String origin, String host, CalendarEntry e) {
    line(document, "BEGIN:VEVENT");
    line(document, "UID:" + e.blockId() + "@" + host);
    line(document, "DTSTAMP:" + utc(e.stampedAt()));
    line(document, "DTSTART:" + utc(e.startAt()));
    line(document, "DTEND:" + utc(e.endAt()));
    line(document, "SUMMARY:" + escapeText(e.title()));
    line(document, "DESCRIPTION:" + escapeText(e.objective()));
    line(document, "SEQUENCE:" + e.version());
    line(document, "URL:" + origin + "/proyectos/" + e.projectId() + "/tareas/" + e.taskId());
    line(document, "END:VEVENT");
  }

  private static String utc(Instant instant) {
    return UTC_STAMP.format(instant);
  }

  static String escapeText(String value) {
    var escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> escaped.append("\\\\");
        case ';' -> escaped.append("\\;");
        case ',' -> escaped.append("\\,");
        case '\n' -> escaped.append("\\n");
        case '\r' -> {}
        default -> escaped.append(c);
      }
    }
    return escaped.toString();
  }

  /** Folds at 75 octets keeping UTF-8 code points and backslash escapes whole. */
  private static void line(StringBuilder document, String content) {
    int octets = 0;
    int i = 0;
    while (i < content.length()) {
      int end = content.charAt(i) == '\\' ? i + 2 : content.offsetByCodePoints(i, 1);
      var atom = content.substring(i, end);
      int size = atom.getBytes(StandardCharsets.UTF_8).length;
      if (octets + size > MAX_LINE_OCTETS) {
        document.append(CRLF).append(' ');
        octets = 1;
      }
      document.append(atom);
      octets += size;
      i = end;
    }
    document.append(CRLF);
  }
}
