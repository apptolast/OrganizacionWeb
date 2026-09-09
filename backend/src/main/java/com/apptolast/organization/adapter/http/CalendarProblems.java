package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CalendarNotFoundException;
import com.apptolast.organization.application.CalendarTooLargeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Failures of the calendar routes are written straight to the response. Going through a message
 * converter would attach {@code Content-Disposition: inline;filename=f.txt} to every address ending
 * in {@code .ics}, which the contract forbids and which would make the answers distinguishable.
 */
final class CalendarProblems {
  private final ObjectMapper json;

  CalendarProblems(ObjectMapper json) {
    this.json = json;
  }

  void write(HttpServletResponse response, RuntimeException error) throws IOException {
    if (error instanceof CalendarNotFoundException)
      problem(response, 404, "CALENDAR_NOT_FOUND", "No existe ningún calendario en esa dirección.");
    else if (error instanceof CalendarTooLargeException)
      problem(
          response,
          413,
          "CALENDAR_TOO_LARGE",
          "El calendario supera los 2000 eventos de la ventana publicada.");
    else
      problem(
          response,
          503,
          "STORAGE_UNAVAILABLE",
          "El almacenamiento no está disponible. Inténtalo más tarde.");
  }

  void problem(HttpServletResponse response, int status, String code, String title)
      throws IOException {
    CalendarDocuments.harden(response);
    response.setStatus(status);
    response.setContentType("application/problem+json");
    var body = json.writeValueAsBytes(ApiErrors.problem(status, code, title));
    response.setContentLength(body.length);
    response.getOutputStream().write(body);
  }
}
