package com.apptolast.organization.adapter.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Writes an iCalendar body with the exact headers of the contract, and nothing else. */
final class CalendarDocuments {
  static final String MEDIA_TYPE = "text/calendar; charset=utf-8";
  static final String CACHE_CONTROL = "private, no-store";
  private static final DateTimeFormatter MICROSECONDS =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);

  private CalendarDocuments() {}

  /** Applied before anything is resolved, so success and failure carry the same protection. */
  static void harden(HttpServletResponse response) {
    response.setHeader("Cache-Control", CACHE_CONTROL);
    response.setHeader("X-Content-Type-Options", "nosniff");
  }

  static void write(String document, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    var body = document.getBytes(StandardCharsets.UTF_8);
    response.setContentType(MEDIA_TYPE);
    response.setContentLength(body.length);
    if (!"HEAD".equalsIgnoreCase(request.getMethod())) response.getOutputStream().write(body);
  }

  /** The contract states creation instants in UTC with microseconds, zeros included. */
  static String instant(Instant value) {
    return value == null ? null : MICROSECONDS.format(value);
  }
}
