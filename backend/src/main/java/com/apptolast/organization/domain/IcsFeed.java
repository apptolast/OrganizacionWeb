package com.apptolast.organization.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Subconjunto RFC 5545: solo VEVENT, sin expansión de recurrencias. */
public record IcsFeed(
    List<ExternalEvent> events, int skippedRecurring, int skippedCancelled, int skippedInvalid) {
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss").withResolverStyle(ResolverStyle.STRICT);
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

  public static IcsFeed parse(String text, ZoneId snapshotZone, Set<String> zones) {
    var lines = unfold(text);
    if (lines.isEmpty() || !lines.getFirst().equalsIgnoreCase("BEGIN:VCALENDAR"))
      throw new IcsFeedMalformedException();
    var events = new ArrayList<ExternalEvent>();
    var seen = new java.util.HashSet<String>();
    int recurring = 0;
    int cancelled = 0;
    int invalid = 0;
    List<Property> current = null;
    int nested = 0;
    for (var line : lines) {
      var property = Property.of(line);
      if (current == null) {
        if (property.is("BEGIN", "VEVENT")) current = new ArrayList<>();
      } else if (property.name().equals("BEGIN")) {
        nested++;
      } else if (nested > 0) {
        if (property.name().equals("END")) nested--;
      } else if (property.is("END", "VEVENT")) {
        if (has(current, "RRULE", "RDATE", "RECURRENCE-ID")) recurring++;
        else if (has(current, "STATUS") && value(current, "STATUS").equalsIgnoreCase("CANCELLED"))
          cancelled++;
        else {
          try {
            var event = event(current, snapshotZone, zones);
            if (!seen.add(event.uid())) throw new InvalidEvent();
            events.add(event);
          } catch (InvalidEvent | java.time.DateTimeException | ArithmeticException error) {
            invalid++;
          }
        }
        current = null;
      } else {
        current.add(property);
      }
    }
    return new IcsFeed(List.copyOf(events), recurring, cancelled, invalid);
  }

  private static boolean has(List<Property> properties, String... names) {
    return properties.stream().anyMatch(property -> Set.of(names).contains(property.name()));
  }

  private static String value(List<Property> properties, String name) {
    return properties.stream()
        .filter(property -> property.name().equals(name))
        .map(Property::value)
        .findFirst()
        .orElseThrow();
  }

  private static ExternalEvent event(List<Property> properties, ZoneId zone, Set<String> zones) {
    String uid = null;
    String summary = "";
    Property start = null;
    Property end = null;
    Property duration = null;
    for (var property : properties) {
      switch (property.name()) {
        case "UID" -> uid = property.value();
        case "SUMMARY" -> summary = summary(property.value());
        case "DTSTART" -> start = property;
        case "DTEND" -> end = property;
        case "DURATION" -> duration = property;
        default -> {}
      }
    }
    if (uid == null || uid.isBlank() || start == null) throw new InvalidEvent();
    if (start.isDate()) {
      if (end != null && !end.isDate()) throw new InvalidEvent();
      var first = LocalDate.parse(start.value(), DATE);
      var last = end == null ? first.plusDays(1) : LocalDate.parse(end.value(), DATE);
      if (!last.isAfter(first)) throw new InvalidEvent();
      return new ExternalEvent(
          uid,
          summary,
          first.atStartOfDay(zone).toInstant(),
          last.atStartOfDay(zone).toInstant(),
          true);
    }
    var startAt = start.instant(zone, zones);
    Instant endAt;
    if (end != null) endAt = end.instant(zone, zones);
    else if (duration != null) endAt = startAt.plus(java.time.Duration.parse(duration.value()));
    else throw new InvalidEvent();
    if (!endAt.isAfter(startAt)) throw new InvalidEvent();
    return new ExternalEvent(uid, summary, startAt, endAt, false);
  }

  static final int SUMMARY_LIMIT = 500;

  private static String summary(String raw) {
    var text = new StringBuilder();
    for (int i = 0; i < raw.length(); i++) {
      char c = raw.charAt(i);
      if (c != '\\' || i + 1 == raw.length()) {
        text.append(c);
        continue;
      }
      char next = raw.charAt(++i);
      text.append(next == 'n' || next == 'N' ? '\n' : next);
    }
    var stripped = text.toString().replaceAll("^\\p{IsWhite_Space}+|\\p{IsWhite_Space}+$", "");
    int codePoints = stripped.codePointCount(0, stripped.length());
    return stripped.substring(
        0, stripped.offsetByCodePoints(0, Math.min(SUMMARY_LIMIT, codePoints)));
  }

  private static final class InvalidEvent extends RuntimeException {}

  private static List<String> unfold(String text) {
    var lines = new ArrayList<String>();
    for (var line : text.replaceAll("\r?\n[ \t]", "").split("\r?\n")) {
      if (!line.isEmpty()) lines.add(line);
    }
    return lines;
  }

  private record Property(String name, Map<String, String> parameters, String value) {
    static Property of(String line) {
      int separator = -1;
      boolean quoted = false;
      for (int i = 0; i < line.length() && separator < 0; i++) {
        char c = line.charAt(i);
        if (c == '"') quoted = !quoted;
        else if (c == ':' && !quoted) separator = i;
      }
      if (separator < 0) throw new IcsFeedMalformedException();
      var head = line.substring(0, separator).split(";");
      var parameters = new LinkedHashMap<String, String>();
      for (int i = 1; i < head.length; i++) {
        int equals = head[i].indexOf('=');
        if (equals < 0) continue;
        parameters.put(
            head[i].substring(0, equals).toUpperCase(Locale.ROOT),
            head[i].substring(equals + 1).replace("\"", ""));
      }
      return new Property(
          head[0].toUpperCase(Locale.ROOT), parameters, line.substring(separator + 1));
    }

    boolean is(String expectedName, String expectedValue) {
      return name.equals(expectedName) && value.equalsIgnoreCase(expectedValue);
    }

    boolean isDate() {
      return "DATE".equalsIgnoreCase(parameters.get("VALUE"));
    }

    Instant instant(ZoneId snapshotZone, Set<String> zones) {
      if (value.endsWith("Z"))
        return LocalDateTime.parse(value.substring(0, value.length() - 1), DATE_TIME)
            .toInstant(ZoneOffset.UTC);
      var local = LocalDateTime.parse(value, DATE_TIME);
      var tzid = parameters.get("TZID");
      if (tzid == null) return local.atZone(snapshotZone).toInstant();
      if (!zones.contains(tzid)) throw new InvalidEvent();
      return local.atZone(ZoneId.of(tzid)).toInstant();
    }
  }
}
