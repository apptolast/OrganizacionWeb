package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CreateApiCredentialUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ApiCredentialController {
  private final CreateApiCredentialUseCase create;
  private final ObjectMapper json;

  public ApiCredentialController(CreateApiCredentialUseCase create, ObjectMapper json) {
    this.create = create;
    this.json = json;
  }

  @PutMapping("/api/v1/me/api-credentials/{id}")
  public ResponseEntity<?> create(
      Principal principal, @PathVariable String id, HttpServletRequest http) throws IOException {
    if (http.getQueryString() != null && !http.getQueryString().isEmpty()) {
      return ResponseEntity.badRequest()
          .header("Cache-Control", "no-store")
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(ApiErrors.problem(400, "API_CREDENTIAL_INVALID", "La consulta no está permitida."));
    }
    UUID parsedId;
    try {
      parsedId = UUID.fromString(id);
      if (!parsedId.toString().equals(id)) throw new IllegalArgumentException();
    } catch (IllegalArgumentException error) {
      var problem = ApiErrors.problem(400, "API_CREDENTIAL_INVALID", "La identidad no es válida.");
      problem.put(
          "errors",
          List.of(
              new com.apptolast.organization.domain.FieldError(
                  "id", "INVALID_VALUE", "Usa una identidad UUID canónica minúscula.")));
      return ResponseEntity.badRequest()
          .header("Cache-Control", "no-store")
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem);
    }
    byte[] body;
    try (var input = http.getInputStream()) {
      body = input.readNBytes(4097);
    }
    if (body.length > 4096) {
      return ResponseEntity.status(413)
          .header("Cache-Control", "no-store")
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(ApiErrors.problem(413, "API_CREDENTIAL_TOO_LARGE", "El cuerpo supera 4096 bytes."));
    }
    CreationRequest request;
    try {
      com.fasterxml.jackson.databind.JsonNode node =
          json.reader()
              .with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
              .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .with(
                  com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
              .readTree(body);
      if (node == null
          || !node.isObject()
          || node.size() != 3
          || !node.hasNonNull("name")
          || !node.get("name").isTextual()
          || !node.hasNonNull("scopes")
          || !node.get("scopes").isArray()
          || !node.hasNonNull("expiresInDays")
          || !node.get("expiresInDays").isNumber()) {
        throw new IllegalArgumentException();
      }
      var scopes = new java.util.ArrayList<String>();
      for (var scope : node.get("scopes")) {
        if (!scope.isTextual()) throw new IllegalArgumentException();
        scopes.add(scope.textValue());
      }
      request =
          new CreationRequest(
              node.get("name").textValue(),
              scopes,
              node.get("expiresInDays").decimalValue().intValueExact());
    } catch (com.fasterxml.jackson.core.JsonProcessingException
        | IllegalArgumentException
        | ArithmeticException error) {
      return ResponseEntity.badRequest()
          .header("Cache-Control", "no-store")
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(ApiErrors.problem(400, "API_CREDENTIAL_INVALID", "El cuerpo no es válido."));
    }
    var result =
        create.create(
            principal.getName(),
            parsedId,
            request.name(),
            request.scopes(),
            request.expiresInDays());
    return (result.secret() == null
            ? ResponseEntity.ok()
            : ResponseEntity.created(URI.create("/api/v1/me/api-credentials/" + id)))
        .header("Cache-Control", "no-store")
        .body(result);
  }

  public record CreationRequest(String name, List<String> scopes, int expiresInDays) {}

  @org.springframework.web.bind.annotation.ExceptionHandler({
    com.apptolast.organization.domain.ApiCredentialInvalidException.class,
    com.apptolast.organization.application.ApiCredentialConflictException.class,
    com.apptolast.organization.application.ApiCredentialLimitException.class,
    com.apptolast.organization.application.StorageUnavailableException.class
  })
  ResponseEntity<?> applicationFailure(RuntimeException error) {
    int status;
    String code;
    if (error instanceof com.apptolast.organization.domain.ApiCredentialInvalidException) {
      status = 400;
      code = "API_CREDENTIAL_INVALID";
    } else if (error
        instanceof com.apptolast.organization.application.ApiCredentialConflictException) {
      status = 409;
      code = "API_CREDENTIAL_CONFLICT";
    } else if (error
        instanceof com.apptolast.organization.application.ApiCredentialLimitException) {
      status = 409;
      code = "API_CREDENTIAL_LIMIT";
    } else {
      status = 503;
      code = "STORAGE_UNAVAILABLE";
    }
    var problem =
        ApiErrors.problem(status, code, "No se pudo confirmar la operación de credencial.");
    if (error instanceof com.apptolast.organization.domain.ApiCredentialInvalidException invalid) {
      problem.put("errors", invalid.errors());
    }
    return ResponseEntity.status(status)
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
