package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ManageCalendarFeedUseCase;
import com.apptolast.organization.application.RenderCalendarUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Managing the subscription and downloading the same document from an authenticated session. */
@RestController
public final class CalendarFeedController {
  private static final String ATTACHMENT = "attachment; filename=\"organizationweb-bloques.ics\"";

  private final ManageCalendarFeedUseCase feeds;
  private final RenderCalendarUseCase calendars;
  private final CalendarProblems problems;

  public CalendarFeedController(
      ManageCalendarFeedUseCase feeds,
      RenderCalendarUseCase calendars,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.feeds = feeds;
    this.calendars = calendars;
    this.problems = new CalendarProblems(json);
  }

  @GetMapping(CalendarPaths.FEED)
  public Map<String, Object> status(Principal principal, HttpServletResponse response) {
    CalendarDocuments.harden(response);
    var status = feeds.status(principal.getName());
    var body = new LinkedHashMap<String, Object>();
    body.put("active", status.active());
    body.put("createdAt", CalendarDocuments.instant(status.createdAt()));
    return body;
  }

  @PostMapping(CalendarPaths.FEED)
  public ResponseEntity<Map<String, Object>> generate(
      Principal principal, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    CalendarDocuments.harden(response);
    if (request.getInputStream().read() != -1) throw new NonEmptyBody();
    var link = feeds.generate(principal.getName());
    var body = new LinkedHashMap<String, Object>();
    body.put("url", link.url());
    body.put("createdAt", CalendarDocuments.instant(link.createdAt()));
    return ResponseEntity.status(201).body(body);
  }

  @DeleteMapping(CalendarPaths.FEED)
  public ResponseEntity<Void> revoke(Principal principal, HttpServletResponse response) {
    CalendarDocuments.harden(response);
    feeds.revoke(principal.getName());
    return ResponseEntity.noContent().build();
  }

  /** The same bytes as the public feed, offered as a file and without reading any token. */
  @GetMapping(CalendarPaths.DOWNLOAD)
  public void download(
      Principal principal, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    CalendarDocuments.harden(response);
    var document = calendars.forOwner(principal.getName());
    response.setHeader("Content-Disposition", ATTACHMENT);
    CalendarDocuments.write(document, request, response);
  }

  /** Written straight to the response so the {@code .ics} download carries no extra header. */
  @org.springframework.web.bind.annotation.ExceptionHandler({
    com.apptolast.organization.application.CalendarTooLargeException.class,
    com.apptolast.organization.application.StorageUnavailableException.class
  })
  void failure(RuntimeException error, HttpServletResponse response) throws IOException {
    problems.write(response, error);
  }

  private static final class NonEmptyBody extends RuntimeException {}

  @org.springframework.web.bind.annotation.ExceptionHandler(NonEmptyBody.class)
  ResponseEntity<Map<String, Object>> nonEmptyBody() {
    var body =
        ApiErrors.problem(400, "VALIDATION_ERROR", "La generación del enlace no admite cuerpo.");
    body.put(
        "errors",
        List.of(
            new com.apptolast.organization.domain.FieldError(
                "body", "MUST_BE_EMPTY", "Envía la solicitud sin cuerpo.")));
    return ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }
}
