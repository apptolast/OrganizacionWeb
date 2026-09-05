package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.EditProjectUseCase;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class ProjectEditController {
  private final EditProjectUseCase edit;
  private final com.apptolast.organization.application.ChangeProjectStatusUseCase states;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public ProjectEditController(
      EditProjectUseCase edit,
      com.fasterxml.jackson.databind.ObjectMapper json,
      com.apptolast.organization.application.ChangeProjectStatusUseCase states) {
    this.edit = edit;
    this.states = states;
    this.json = json;
  }

  @PutMapping(value = "/api/v1/projects/{id}", consumes = "application/json")
  public ResponseEntity<Project> edit(
      @PathVariable String id,
      @RequestHeader("If-Match") java.util.List<String> matches,
      @RequestBody String raw,
      Principal principal) {
    var expected = precondition(matches);
    var body = body(raw, "name", "description");
    if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid("id");
    var result =
        edit.execute(
            principal.getName(),
            UUID.fromString(id),
            expected,
            body.get("name").asText(),
            body.get("description").asText());
    return ResponseEntity.ok()
        .eTag("\"" + result.project().id() + ":" + result.version() + "\"")
        .body(result.project());
  }

  @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
  public ResponseEntity<java.util.Map<String, Object>> missingPrecondition() {
    return ResponseEntity.status(428)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                428, "PRECONDITION_REQUIRED", "Recarga el proyecto antes de guardar los cambios."));
  }

  private static ProjectRevision precondition(java.util.List<String> matches) {
    if (matches.size() != 1) throw invalid("If-Match");
    String match = matches.getFirst();
    if (!match.matches(
        "\"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match");
    var parts = match.substring(1, match.length() - 1).split(":");
    try {
      return new ProjectRevision(UUID.fromString(parts[0]), Long.parseLong(parts[1]));
    } catch (IllegalArgumentException error) {
      throw invalid("If-Match");
    }
  }

  private static ValidationException invalid(String field) {
    return new ValidationException(
        java.util.List.of(
            new FieldError(field, "INVALID_VALUE", "El valor enviado no es válido.")));
  }

  private JsonNode body(String raw, String... required) {
    JsonNode body;
    try {
      body =
          json.reader()
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature
                      .FAIL_ON_READING_DUP_TREE_KEY)
              .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(raw);
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw invalid("body");
    }
    if (body == null || !body.isObject()) throw invalid("body");
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!java.util.Arrays.asList(required).contains(field)) throw invalid("body");
            });
    for (String field : required) {
      if (!body.has(field) || !body.get(field).isTextual()) throw invalid(field);
    }
    return body;
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<java.util.Map<String, Object>> malformed() {
    var result = ApiErrors.problem(400, "VALIDATION_ERROR", "Revisa los datos enviados.");
    result.put(
        "errors",
        java.util.List.of(
            new FieldError("body", "INVALID_VALUE", "El JSON enviado no es válido.")));
    return ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(result);
  }

  @PutMapping(value = "/api/v1/projects/{id}/status", consumes = "application/json")
  public ResponseEntity<Project> status(
      @PathVariable String id,
      @RequestHeader("If-Match") java.util.List<String> matches,
      @RequestBody String raw,
      Principal principal) {
    var expected = precondition(matches);
    var body = body(raw, "status");
    if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid("id");
    var target = body.get("status").asText();
    if (!ProjectStates.valid(target)) throw invalid("status");
    var result = states.execute(principal.getName(), UUID.fromString(id), expected, target);
    return ResponseEntity.ok()
        .eTag("\"" + result.project().id() + ":" + result.version() + "\"")
        .body(result.project());
  }
}
