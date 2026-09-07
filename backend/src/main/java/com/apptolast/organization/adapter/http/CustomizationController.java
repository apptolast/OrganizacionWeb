package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class CustomizationController {
  private final ReadCustomizationUseCase read;
  private final SaveCustomizationViewUseCase save;
  private final CreateCustomFieldUseCase create;
  private final UpdateCustomFieldUseCase update;
  private final ObjectMapper json;

  public CustomizationController(
      ReadCustomizationUseCase read,
      SaveCustomizationViewUseCase save,
      CreateCustomFieldUseCase create,
      UpdateCustomFieldUseCase update,
      ObjectMapper json) {
    this.read = read;
    this.save = save;
    this.create = create;
    this.update = update;
    this.json = json;
  }

  @PutMapping(
      value = "/api/v1/me/customization/{scope}/fields/{fieldId}",
      consumes = "application/json")
  public ResponseEntity<CustomizationResponse> update(
      Principal principal,
      @PathVariable String scope,
      @PathVariable String fieldId,
      @RequestBody(required = false) String raw,
      @RequestHeader HttpHeaders headers,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    query(parameters);
    var parsed = scope(scope);
    if (!fieldId.matches(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
      throw invalid("fieldId", "INVALID_FORMAT");
    var id = UUID.fromString(fieldId);
    var expected = precondition(headers, parsed);
    var body = body(raw, Set.of("label", "active"));
    var label = new CustomFieldLabel(text(body, "label")).value();
    if (!body.hasNonNull("active")) throw invalid("active", "REQUIRED");
    if (!body.get("active").isBoolean()) throw invalid("active", "INVALID_TYPE");
    var active = body.get("active").booleanValue();
    return response(update.update(principal.getName(), parsed, id, expected, label, active));
  }

  @PostMapping(value = "/api/v1/me/customization/{scope}/fields", consumes = "application/json")
  public ResponseEntity<CustomizationResponse> create(
      Principal principal,
      @PathVariable String scope,
      @RequestBody(required = false) String raw,
      @RequestHeader HttpHeaders headers,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    query(parameters);
    var parsed = scope(scope);
    var expected = precondition(headers, parsed);
    var body = body(raw, Set.of("label", "type"));
    var label = new CustomFieldLabel(text(body, "label")).value();
    var rawType = text(body, "type");
    CustomFieldType type;
    try {
      type = CustomFieldType.valueOf(rawType);
    } catch (IllegalArgumentException error) {
      throw invalid("type", "INVALID_VALUE");
    }
    return response(create.create(principal.getName(), parsed, expected, label, type));
  }

  @PutMapping(value = "/api/v1/me/customization/{scope}", consumes = "application/json")
  public ResponseEntity<CustomizationResponse> put(
      Principal principal,
      @PathVariable String scope,
      @RequestBody(required = false) String raw,
      @RequestHeader HttpHeaders headers,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    query(parameters);
    var parsed = scope(scope);
    var expected = precondition(headers, parsed);
    var body = body(raw, Set.of("visibleFields"));
    var visible = body.get("visibleFields");
    if (visible == null || visible.isNull()) throw invalid("visibleFields", "REQUIRED");
    if (!visible.isArray()) throw invalid("visibleFields", "INVALID_TYPE");
    var fields = new ArrayList<String>();
    for (int index = 0; index < visible.size(); index++) {
      var value = visible.get(index);
      if (value.isNull()) throw invalid("visibleFields[" + index + "]", "REQUIRED");
      if (!value.isTextual()) throw invalid("visibleFields[" + index + "]", "INVALID_TYPE");
      fields.add(value.textValue());
      new CustomizationView(parsed, fields);
    }
    var view = new CustomizationView(parsed, fields);
    return response(save.save(principal.getName(), parsed, expected, view.visibleFields()));
  }

  private JsonNode body(String raw, Set<String> allowed) throws JsonProcessingException {
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    var body =
        json.reader().with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(raw);
    if (!body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!allowed.contains(field)) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    return body;
  }

  private static String text(JsonNode body, String field) {
    var value = body.get(field);
    if (value == null || value.isNull()) throw invalid(field, "REQUIRED");
    if (!value.isTextual()) throw invalid(field, "INVALID_TYPE");
    return value.textValue();
  }

  private static CustomizationRevision precondition(HttpHeaders headers, CustomizationScope scope) {
    var matches = headers.get("If-Match");
    if (matches == null) throw new MissingPrecondition();
    if (matches.size() != 1) throw invalid("If-Match", "INVALID_VALUE");
    var tag = matches.getFirst();
    if (!tag.equals("\"customization:" + scope + ":unconfigured\"")
        && !tag.matches(
            "\"customization:"
                + scope
                + ":[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match", "INVALID_VALUE");
    var parts = tag.substring(1, tag.length() - 1).split(":");
    try {
      return parts[2].equals("unconfigured")
          ? new CustomizationRevision(null, 0)
          : new CustomizationRevision(UUID.fromString(parts[2]), Long.parseLong(parts[3]));
    } catch (NumberFormatException error) {
      throw invalid("If-Match", "INVALID_VALUE");
    }
  }

  private static void query(MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }

  private static final class MissingPrecondition extends RuntimeException {}

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler(MissingPrecondition.class)
  ResponseEntity<?> missing() {
    return ResponseEntity.status(428)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                428, "PRECONDITION_REQUIRED", "Envía la revisión actual de personalización."));
  }

  @ExceptionHandler(MalformedBody.class)
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  @ExceptionHandler(CustomizationConflictException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(412)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
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
    query(parameters);
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
