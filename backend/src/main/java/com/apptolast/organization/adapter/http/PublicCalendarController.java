package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CalendarNotFoundException;
import com.apptolast.organization.application.RenderCalendarUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * The subscription address. It takes no credentials, writes nothing and never logs the path,
 * because the path is the secret.
 */
@RestController
public final class PublicCalendarController {
  private static final String PATTERN = CalendarPaths.PUBLIC_PREFIX + "**";
  private static final String ALLOWED = "GET, HEAD";

  private final RenderCalendarUseCase calendars;
  private final CalendarProblems problems;

  public PublicCalendarController(
      RenderCalendarUseCase calendars, com.fasterxml.jackson.databind.ObjectMapper json) {
    this.calendars = calendars;
    this.problems = new CalendarProblems(json);
  }

  @RequestMapping(
      value = PATTERN,
      method = {RequestMethod.GET, RequestMethod.HEAD})
  public void feed(HttpServletRequest request, HttpServletResponse response) throws IOException {
    CalendarDocuments.harden(response);
    CalendarDocuments.write(calendars.forToken(candidateOf(request)), request, response);
  }

  /** Refused before the candidate is even read, so a known and an unknown token look alike. */
  @RequestMapping(
      value = PATTERN,
      method = {
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.DELETE,
        RequestMethod.PATCH,
        RequestMethod.OPTIONS
      })
  public void unsupportedMethod(HttpServletResponse response) throws IOException {
    response.setHeader("Allow", ALLOWED);
    problems.problem(
        response, 405, "METHOD_NOT_ALLOWED", "El calendario sólo se puede leer con GET o HEAD.");
  }

  @org.springframework.web.bind.annotation.ExceptionHandler({
    CalendarNotFoundException.class,
    com.apptolast.organization.application.CalendarTooLargeException.class,
    com.apptolast.organization.application.StorageUnavailableException.class
  })
  void failure(RuntimeException error, HttpServletResponse response) throws IOException {
    problems.write(response, error);
  }

  /** Any address that is not exactly one {@code .ics} segment is unresolvable, like a bad token. */
  private static String candidateOf(HttpServletRequest request) {
    if (request.getQueryString() != null || !request.getParameterMap().isEmpty())
      throw new CalendarNotFoundException();
    var segment = CalendarPaths.of(request).substring(CalendarPaths.PUBLIC_PREFIX.length());
    if (segment.indexOf('/') >= 0 || !segment.endsWith(CalendarPaths.SUFFIX))
      throw new CalendarNotFoundException();
    return segment.substring(0, segment.length() - CalendarPaths.SUFFIX.length());
  }
}
