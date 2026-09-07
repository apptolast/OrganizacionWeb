package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.AppearanceConflictException;
import com.apptolast.organization.application.ReadAppearanceUseCase;
import com.apptolast.organization.application.SaveAppearanceUseCase;
import com.apptolast.organization.domain.Appearance;
import com.apptolast.organization.domain.AppearanceRevision;
import com.apptolast.organization.domain.AppearanceValues;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class AppearanceController {
  private final ReadAppearanceUseCase read;
  private final SaveAppearanceUseCase save;
  private final ObjectMapper json;

  public AppearanceController(
      ReadAppearanceUseCase read, SaveAppearanceUseCase save, ObjectMapper json) {
    this.read = read;
    this.save = save;
    this.json = json;
  }

  public record AppearanceResponse(
      boolean configured, String theme, String accentLight, String accentDark, Instant updatedAt) {}

  @GetMapping("/api/v1/me/appearance")
  public ResponseEntity<AppearanceResponse> get(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    query(parameters);
    return read.get(principal.getName())
        .map(AppearanceController::response)
        .orElseGet(
            () ->
                ResponseEntity.ok()
                    .eTag("\"appearance:unconfigured\"")
                    .body(new AppearanceResponse(false, "SYSTEM", "#244C3C", "#B7E4C7", null)));
  }

  private static ResponseEntity<AppearanceResponse> response(Appearance value) {
    return ResponseEntity.ok()
        .eTag("\"appearance:" + value.id() + ":" + value.version() + "\"")
        .body(
            new AppearanceResponse(
                true, value.theme(), value.accentLight(), value.accentDark(), value.updatedAt()));
  }

  @PutMapping(value = "/api/v1/me/appearance", consumes = "application/json")
  public ResponseEntity<?> put(
      @RequestBody(required = false) String raw,
      Principal principal,
      @RequestHeader HttpHeaders headers,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    query(parameters);
    var matches = headers.get("If-Match");
    if (matches == null)
      return ResponseEntity.status(428)
          .contentType(MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la versión actual de apariencia."));
    var expected = precondition(matches);
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    var body =
        json.reader()
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (!body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!Set.of("theme", "accentLight", "accentDark").contains(field)) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    var theme = text(body, "theme");
    AppearanceValues.theme(theme);
    var light = text(body, "accentLight");
    AppearanceValues.accentLight(light);
    var dark = text(body, "accentDark");
    AppearanceValues.accentDark(dark);
    return response(save.execute(principal.getName(), expected, theme, light, dark));
  }

  private static String text(JsonNode body, String field) {
    var value = body.get(field);
    if (value == null || value.isNull()) throw invalid(field, "REQUIRED");
    if (!value.isTextual()) throw invalid(field, "INVALID_TYPE");
    return value.textValue();
  }

  private static AppearanceRevision precondition(List<String> matches) {
    if (matches.size() != 1) throw invalid("If-Match", "INVALID_VALUE");
    var raw = matches.getFirst();
    if (raw.equals("\"appearance:unconfigured\"")) return new AppearanceRevision(null, 0);
    if (!raw.matches(
        "\"appearance:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match", "INVALID_VALUE");
    var parts = raw.substring(1, raw.length() - 1).split(":");
    try {
      return new AppearanceRevision(UUID.fromString(parts[1]), Long.parseLong(parts[2]));
    } catch (NumberFormatException error) {
      throw invalid("If-Match", "INVALID_VALUE");
    }
  }

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler(AppearanceConflictException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                412, "APPEARANCE_CONFLICT", "La apariencia tiene una versión más reciente."));
  }

  @ExceptionHandler(MalformedBody.class)
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  private static void query(MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
