package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadCustomizationUseCase;
import com.apptolast.organization.application.SaveCustomizationViewUseCase;
import com.apptolast.organization.domain.CustomFieldDefinition;
import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationRevision;
import com.apptolast.organization.domain.CustomizationScope;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class CustomizationController {
  private final ReadCustomizationUseCase read;
  private final SaveCustomizationViewUseCase save;
  private final ObjectMapper json;

  public CustomizationController(
      ReadCustomizationUseCase read, SaveCustomizationViewUseCase save, ObjectMapper json) {
    this.read = read;
    this.save = save;
    this.json = json;
  }

  @PutMapping(value = "/api/v1/me/customization/{scope}", consumes = "application/json")
  public ResponseEntity<?> put(
      Principal principal,
      @PathVariable String scope,
      @RequestBody(required = false) String raw,
      @RequestHeader HttpHeaders headers,
      @RequestParam MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    var parsed = scope(scope);
    if (headers.get("If-Match") == null)
      return ResponseEntity.status(428)
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la revisión actual de personalización."));
    if (headers.get("If-Match").size() != 1) throw invalid("If-Match", "INVALID_VALUE");
    var tag = headers.getFirst("If-Match");
    if (!tag.equals("\"customization:" + scope + ":unconfigured\"")
        && !tag.matches(
            "\"customization:"
                + scope
                + ":[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match", "INVALID_VALUE");
    var parts = tag.substring(1, tag.length() - 1).split(":");
    CustomizationRevision expected;
    try {
      expected =
          parts[2].equals("unconfigured")
              ? new CustomizationRevision(null, 0)
              : new CustomizationRevision(
                  java.util.UUID.fromString(parts[2]), Long.parseLong(parts[3]));
    } catch (NumberFormatException error) {
      throw invalid("If-Match", "INVALID_VALUE");
    }
    if (raw == null || raw.isBlank())
      return ResponseEntity.badRequest()
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
    var fields = new java.util.ArrayList<String>();
    var body =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .readTree(raw);
    if (!body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new java.util.TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!field.equals("visibleFields")) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    var visible = body.get("visibleFields");
    if (visible == null || visible.isNull()) throw invalid("visibleFields", "REQUIRED");
    if (!visible.isArray()) throw invalid("visibleFields", "INVALID_TYPE");
    for (int index = 0; index < visible.size(); index++) {
      var value = visible.get(index);
      if (value.isNull()) throw invalid("visibleFields[" + index + "]", "REQUIRED");
      if (!value.isTextual()) throw invalid("visibleFields[" + index + "]", "INVALID_TYPE");
      fields.add(value.textValue());
    }
    var view = new com.apptolast.organization.domain.CustomizationView(parsed, fields);
    return response(save.save(principal.getName(), parsed, expected, view.visibleFields()));
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      com.apptolast.organization.application.CustomizationConflictException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(412)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                412,
                "CUSTOMIZATION_CONFLICT",
                "La personalización tiene una revisión más reciente."));
  }

  private static CustomizationScope scope(String value) {
    try {
      return CustomizationScope.valueOf(value);
    } catch (IllegalArgumentException error) {
      throw invalid("scope", "INVALID_VALUE");
    }
  }

  private static ResponseEntity<CustomizationResponse> response(Customization value) {
    return ResponseEntity.ok()
        .eTag("\"customization:" + value.scope() + ":" + value.id() + ":" + value.version() + "\"")
        .body(
            new CustomizationResponse(
                true, value.visibleFields(), value.customFields(), value.updatedAt()));
  }

  public record CustomizationResponse(
      boolean configured,
      List<String> visibleFields,
      List<CustomFieldDefinition> customFields,
      Instant updatedAt) {}

  @GetMapping("/api/v1/me/customization/{scope}")
  public ResponseEntity<CustomizationResponse> get(
      Principal principal,
      @PathVariable String scope,
      @RequestParam MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    var parsed = scope(scope);
    return read.get(principal.getName(), parsed)
        .map(CustomizationController::response)
        .orElseGet(
            () ->
                ResponseEntity.ok()
                    .eTag("\"customization:" + scope + ":unconfigured\"")
                    .body(
                        new CustomizationResponse(
                            false,
                            parsed == CustomizationScope.PROJECT
                                ? List.of("createdAt")
                                : List.of("completionCriterion", "estimatedMinutes"),
                            List.of(),
                            null)));
  }
}
