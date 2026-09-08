package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ExportDataUseCase;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ExportDataController {
  private final ExportDataUseCase exports;

  @org.springframework.web.bind.annotation.RequestMapping(
      value = "/api/v1/me/export",
      method = {
        org.springframework.web.bind.annotation.RequestMethod.POST,
        org.springframework.web.bind.annotation.RequestMethod.PUT,
        org.springframework.web.bind.annotation.RequestMethod.PATCH,
        org.springframework.web.bind.annotation.RequestMethod.DELETE,
        org.springframework.web.bind.annotation.RequestMethod.OPTIONS
      })
  public org.springframework.http.ResponseEntity<?> unsupportedMethod() {
    return org.springframework.http.ResponseEntity.status(405)
        .header("Allow", "GET")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(405, "METHOD_NOT_ALLOWED", "La exportación sólo admite GET."));
  }

  @org.springframework.web.bind.annotation.RequestMapping(
      value = "/api/v1/me/export",
      method = org.springframework.web.bind.annotation.RequestMethod.HEAD)
  public org.springframework.http.ResponseEntity<Void> head() {
    return org.springframework.http.ResponseEntity.status(405).header("Allow", "GET").build();
  }

  public ExportDataController(ExportDataUseCase exports) {
    this.exports = exports;
  }

  @GetMapping("/api/v1/me/export")
  public void get(
      Principal principal,
      jakarta.servlet.http.HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam MultiValueMap<String, String> parameters)
      throws IOException {
    if (!parameters.isEmpty()) throw new InvalidQuery();
    if (request.getInputStream().read() != -1) throw new InvalidRequest();
    acceptable(request);
    identityEncoding(request);
    var prepared = exports.prepare(principal.getName());
    response.setContentType("application/json; charset=utf-8");
    response.setContentLengthLong(prepared.contentLength());
    response.setHeader(
        "Content-Disposition", "attachment; filename=\"" + prepared.filename() + "\"");
    response.setHeader("Cache-Control", "no-store, private, no-transform");
    response.setHeader("X-Content-Type-Options", "nosniff");
    prepared.writeTo(response.getOutputStream());
  }

  private static final class InvalidQuery extends RuntimeException {}

  @org.springframework.web.bind.annotation.ExceptionHandler(
      com.apptolast.organization.application.ExportTooLargeException.class)
  org.springframework.http.ResponseEntity<?> tooLarge() {
    return org.springframework.http.ResponseEntity.status(413)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                413,
                "EXPORT_TOO_LARGE",
                "La exportación supera el límite de 100000 registros o 32 MiB."));
  }

  private static final class InvalidRequest extends RuntimeException {}

  private static final class UnacceptableFormat extends RuntimeException {}

  private static void identityEncoding(jakarta.servlet.http.HttpServletRequest request) {
    var values = java.util.Collections.list(request.getHeaders("Accept-Encoding"));
    try {
      // Reuse Spring's token/quality validation while retaining explicit q=0.
      var encodings =
          values.stream()
              .flatMap(value -> java.util.Arrays.stream(value.split(",")))
              .map(String::trim)
              .filter(value -> !value.isEmpty())
              .map(
                  value ->
                      org.springframework.http.MediaType.parseMediaType("application/" + value))
              .toList();
      if (encodings.stream()
          .anyMatch(value -> !java.util.Set.of("q").containsAll(value.getParameters().keySet()))) {
        throw new InvalidRequest();
      }
      var identity =
          encodings.stream()
              .filter(value -> value.getSubtype().equalsIgnoreCase("identity"))
              .findFirst();
      var selected =
          identity.or(
              () -> encodings.stream().filter(value -> value.getSubtype().equals("*")).findFirst());
      if (selected.isPresent() && selected.get().getQualityValue() == 0)
        throw new UnacceptableFormat();
    } catch (org.springframework.http.InvalidMediaTypeException error) {
      throw new InvalidRequest();
    }
  }

  private static void acceptable(jakarta.servlet.http.HttpServletRequest request) {
    try {
      var accepted =
          new org.springframework.web.accept.HeaderContentNegotiationStrategy()
              .resolveMediaTypes(
                  new org.springframework.web.context.request.ServletWebRequest(request));
      var selected =
          accepted.stream()
              .filter(
                  type ->
                      type.isCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
              .sorted(
                  (left, right) ->
                      org.springframework.http.MediaType.SPECIFICITY_COMPARATOR.compare(
                          left.removeQualityValue(), right.removeQualityValue()))
              .findFirst();
      if (selected.isEmpty() || selected.get().getQualityValue() == 0)
        throw new UnacceptableFormat();
    } catch (org.springframework.web.HttpMediaTypeNotAcceptableException error) {
      throw new InvalidRequest();
    }
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(UnacceptableFormat.class)
  org.springframework.http.ResponseEntity<?> unacceptable() {
    return org.springframework.http.ResponseEntity.status(406)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                406,
                "EXPORT_FORMAT_NOT_ACCEPTABLE",
                "Solicita JSON sin compresión para exportar."));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(InvalidRequest.class)
  org.springframework.http.ResponseEntity<?> invalidRequest() {
    return org.springframework.http.ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                400, "INVALID_EXPORT_REQUEST", "La solicitud de exportación no es válida."));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(InvalidQuery.class)
  org.springframework.http.ResponseEntity<?> invalidQuery() {
    return org.springframework.http.ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(400, "INVALID_EXPORT_QUERY", "La exportación no admite parámetros."));
  }
}
