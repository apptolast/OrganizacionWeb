package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.ImportDataUseCase;
import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.application.ImportTooLargeException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ImportDataController {
  private static final DateTimeFormatter INSTANT =
      new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
  private final ImportDataUseCase imports;

  public ImportDataController(ImportDataUseCase imports) {
    this.imports = imports;
  }

  @RequestMapping(value = "/api/v1/me/import/preview", method = RequestMethod.HEAD)
  public ResponseEntity<Void> head() {
    return ResponseEntity.status(405).header("Allow", "POST").build();
  }

  @RequestMapping(
      value = "/api/v1/me/import/preview",
      method = {
        RequestMethod.GET,
        RequestMethod.PUT,
        RequestMethod.PATCH,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
      })
  public ResponseEntity<?> unsupported() {
    return ResponseEntity.status(405)
        .header("Allow", "POST")
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                405, "METHOD_NOT_ALLOWED", "Esta ruta de importación sólo admite POST."));
  }

  private static final class InvalidRequest extends RuntimeException {}

  @ExceptionHandler(InvalidRequest.class)
  ResponseEntity<?> invalidRequest() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                400, "IMPORT_INVALID_REQUEST", "La petición de importación no es válida."));
  }

  @ExceptionHandler(ImportTooLargeException.class)
  ResponseEntity<?> tooLarge() {
    return ResponseEntity.status(413)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                413, "IMPORT_TOO_LARGE", "El archivo supera los límites de importación."));
  }

  @ExceptionHandler(ImportInvalidFileException.class)
  ResponseEntity<?> invalidFile() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                400, "IMPORT_INVALID_FILE", "El archivo de importación no es válido."));
  }

  @PostMapping(value = "/api/v1/me/import/preview", consumes = "application/json")
  public PreviewResponse preview(
      Principal principal,
      HttpServletRequest request,
      @RequestParam MultiValueMap<String, String> parameters)
      throws IOException, HttpMediaTypeNotSupportedException {
    var media = MediaType.parseMediaType(request.getContentType());
    if (media.getCharset() != null && !StandardCharsets.UTF_8.equals(media.getCharset()))
      throw new HttpMediaTypeNotSupportedException(media, List.of(MediaType.APPLICATION_JSON));
    if (Collections.list(request.getHeaders("Content-Encoding")).stream()
        .anyMatch(value -> !value.equalsIgnoreCase("identity")))
      throw new HttpMediaTypeNotSupportedException(media, List.of(MediaType.APPLICATION_JSON));
    if (!parameters.isEmpty()) throw new InvalidRequest();
    try (var body = request.getInputStream()) {
      var prepared = imports.preview(principal.getName(), body);
      return new PreviewResponse(
          "organizationweb-import-preview",
          1,
          prepared.fileSha256(),
          prepared.byteLength(),
          prepared.owner(),
          INSTANT.format(prepared.exportedAt()),
          prepared.counts(),
          prepared.insertCounts(),
          prepared.identicalCounts(),
          prepared.runningSessions().stream()
              .map(
                  session ->
                      new RunningSessionResponse(
                          session.sessionId(),
                          session.runningSince() == null
                              ? null
                              : INSTANT.format(session.runningSince())))
              .toList());
    }
  }

  public record PreviewResponse(
      String format,
      int schemaVersion,
      String fileSha256,
      long byteLength,
      String owner,
      String exportedAt,
      ImportCounts counts,
      ImportCounts insertCounts,
      ImportCounts identicalCounts,
      List<RunningSessionResponse> runningSessions) {}

  public record RunningSessionResponse(UUID sessionId, String runningSince) {}
}
