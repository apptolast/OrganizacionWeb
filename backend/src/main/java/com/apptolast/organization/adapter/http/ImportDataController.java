package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ApplyImportDataUseCase;
import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.ImportDataUseCase;
import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.application.ImportReceipt;
import com.apptolast.organization.application.ImportTooLargeException;
import com.apptolast.organization.application.ReadImportReceiptUseCase;
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
  private final ApplyImportDataUseCase applyImports;
  private final ReadImportReceiptUseCase receipts;

  public ImportDataController(
      ImportDataUseCase imports,
      ApplyImportDataUseCase applyImports,
      ReadImportReceiptUseCase receipts) {
    this.imports = imports;
    this.applyImports = applyImports;
    this.receipts = receipts;
  }

  @org.springframework.web.bind.annotation.GetMapping("/api/v1/me/imports/by-key/{requestKey}")
  public ReceiptResponse find(
      Principal principal,
      HttpServletRequest request,
      @org.springframework.web.bind.annotation.PathVariable String requestKey,
      @RequestParam MultiValueMap<String, String> parameters)
      throws IOException {
    if (!parameters.isEmpty()) throw new InvalidRequest();
    try (var body = request.getInputStream()) {
      if (body.read() != -1) throw new InvalidRequest();
    }
    return receipt(
        receipts
            .find(principal.getName(), identifier(requestKey))
            .orElseThrow(ReceiptNotFound::new));
  }

  private static final class ReceiptNotFound extends RuntimeException {}

  @ExceptionHandler(com.apptolast.organization.application.ImportConflictException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409, "IMPORT_CONFLICT", "El archivo no es compatible con los datos actuales."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ImportKeyReusedException.class)
  ResponseEntity<?> keyReused() {
    return ResponseEntity.status(409)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(409, "IMPORT_KEY_REUSED", "La clave ya corresponde a otro archivo."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ImportFileChangedException.class)
  ResponseEntity<?> fileChanged() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                412, "IMPORT_FILE_CHANGED", "El archivo no coincide con la vista previa."));
  }

  @ExceptionHandler(ReceiptNotFound.class)
  ResponseEntity<?> notFound() {
    return ResponseEntity.status(404)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(404, "IMPORT_NOT_FOUND", "No se ha encontrado un recibo accesible."));
  }

  @PostMapping(value = "/api/v1/me/import", consumes = "application/json")
  public ReceiptResponse apply(
      Principal principal,
      HttpServletRequest request,
      @RequestParam MultiValueMap<String, String> parameters)
      throws IOException, HttpMediaTypeNotSupportedException {
    media(request);
    if (!parameters.isEmpty()) throw new InvalidRequest();
    var requestKey = identifier(singleHeader(request, "Idempotency-Key"));
    var hash = singleHeader(request, "X-Import-Content-SHA256");
    if (!hash.matches("[0-9a-f]{64}")) throw new InvalidRequest();
    try (var body = request.getInputStream()) {
      return receipt(applyImports.apply(principal.getName(), requestKey, hash, body));
    }
  }

  private static void media(HttpServletRequest request) throws HttpMediaTypeNotSupportedException {
    var media = MediaType.parseMediaType(request.getContentType());
    if (media.getCharset() != null && !StandardCharsets.UTF_8.equals(media.getCharset()))
      throw new HttpMediaTypeNotSupportedException(media, List.of(MediaType.APPLICATION_JSON));
    if (Collections.list(request.getHeaders("Content-Encoding")).stream()
        .anyMatch(value -> !value.equalsIgnoreCase("identity")))
      throw new HttpMediaTypeNotSupportedException(media, List.of(MediaType.APPLICATION_JSON));
  }

  private static String singleHeader(HttpServletRequest request, String name) {
    var values = Collections.list(request.getHeaders(name));
    if (values.size() != 1) throw new InvalidRequest();
    return values.getFirst();
  }

  private static UUID identifier(String raw) {
    if (raw == null) throw new InvalidRequest();
    try {
      var value = UUID.fromString(raw);
      if (!value.toString().equals(raw)) throw new InvalidRequest();
      return value;
    } catch (IllegalArgumentException error) {
      throw new InvalidRequest();
    }
  }

  private static ReceiptResponse receipt(ImportReceipt value) {
    return new ReceiptResponse(
        value.requestKey(),
        value.fileSha256(),
        value.byteLength(),
        INSTANT.format(value.recordedAt()),
        value.outcome(),
        value.insertedCounts(),
        value.identicalCounts());
  }

  public record ReceiptResponse(
      UUID requestKey,
      String fileSha256,
      long byteLength,
      String recordedAt,
      String outcome,
      ImportCounts insertedCounts,
      ImportCounts identicalCounts) {}

  @RequestMapping(
      value = {"/api/v1/me/import/preview", "/api/v1/me/import"},
      method = RequestMethod.HEAD)
  public ResponseEntity<Void> head() {
    return ResponseEntity.status(405).header("Allow", "POST").build();
  }

  @RequestMapping(
      value = {"/api/v1/me/import/preview", "/api/v1/me/import"},
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

  @RequestMapping(
      value = "/api/v1/me/imports/by-key/{requestKey}",
      method = {
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.PATCH,
        RequestMethod.DELETE,
        RequestMethod.OPTIONS
      })
  public ResponseEntity<?> unsupportedReceiptMethod() {
    return ResponseEntity.status(405)
        .header("Allow", "GET")
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(405, "METHOD_NOT_ALLOWED", "Esta ruta de recibos sólo admite GET."));
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
    media(request);
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
