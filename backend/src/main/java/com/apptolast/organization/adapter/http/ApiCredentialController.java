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
  private final com.apptolast.organization.application.ReadApiCredentialsUseCase read;
  private final com.apptolast.organization.application.RevokeApiCredentialUseCase revoke;

  public ApiCredentialController(
      CreateApiCredentialUseCase create,
      ObjectMapper json,
      com.apptolast.organization.application.ReadApiCredentialsUseCase read,
      com.apptolast.organization.application.RevokeApiCredentialUseCase revoke) {
    this.create = create;
    this.json = json;
    this.read = read;
    this.revoke = revoke;
  }

  @PutMapping("/api/v1/me/api-credentials/{id}/revocation")
  public ResponseEntity<?> revoke(
      Principal principal, @PathVariable String id, HttpServletRequest http) throws IOException {
    acceptable(http);
    noQuery(http);
    emptyBody(http);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(
            revoke
                .revoke(principal.getName(), identifier(id))
                .orElseThrow(CredentialNotFound::new));
  }

  @org.springframework.web.bind.annotation.GetMapping("/api/v1/me/api-credentials/{id}")
  public ResponseEntity<?> find(
      Principal principal, @PathVariable String id, HttpServletRequest http) throws IOException {
    acceptable(http);
    noQuery(http);
    emptyBody(http);
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(read.find(principal.getName(), identifier(id)).orElseThrow(CredentialNotFound::new));
  }

  @org.springframework.web.bind.annotation.GetMapping("/api/v1/me/api-credentials")
  public ResponseEntity<?> list(
      Principal principal,
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> parameters,
      HttpServletRequest http)
      throws IOException {
    acceptable(http);
    emptyBody(http);
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor"))
        || parameters.containsKey("cursor") && parameters.get("cursor").size() != 1) {
      throw new com.apptolast.organization.domain.ApiCredentialInvalidException("query");
    }
    return ResponseEntity.ok()
        .header("Cache-Control", "no-store")
        .body(read.list(principal.getName(), parameters.getFirst("cursor")));
  }

  private static void acceptable(HttpServletRequest request) {
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
        throw new UnacceptableResponse();
    } catch (org.springframework.web.HttpMediaTypeNotAcceptableException error) {
      throw new UnacceptableResponse();
    }
  }

  private static final class UnacceptableResponse extends RuntimeException {}

  @org.springframework.web.bind.annotation.ExceptionHandler(UnacceptableResponse.class)
  ResponseEntity<?> unacceptable() {
    return ResponseEntity.status(406)
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(406, "NOT_ACCEPTABLE", "Esta operación devuelve JSON."));
  }

  private static void emptyBody(HttpServletRequest request) throws IOException {
    try (var input = request.getInputStream()) {
      if (input.read() != -1)
        throw new com.apptolast.organization.domain.ApiCredentialInvalidException("body");
    }
  }

  private static void noQuery(HttpServletRequest request) {
    if (request.getQueryString() != null && !request.getQueryString().isEmpty())
      throw new com.apptolast.organization.domain.ApiCredentialInvalidException("query");
  }

  private static UUID identifier(String raw) {
    try {
      UUID id = UUID.fromString(raw);
      if (!id.toString().equals(raw)) throw new IllegalArgumentException();
      return id;
    } catch (IllegalArgumentException error) {
      throw new com.apptolast.organization.domain.ApiCredentialInvalidException("id");
    }
  }

  private static final class CredentialNotFound extends RuntimeException {}

  @PutMapping("/api/v1/me/api-credentials")
  ResponseEntity<?> unsupportedListPut(HttpServletRequest request) {
    return unsupported(request);
  }

  @org.springframework.web.bind.annotation.GetMapping("/api/v1/me/api-credentials/{id}/revocation")
  ResponseEntity<?> unsupportedRevocationRead(HttpServletRequest request) {
    return unsupported(request);
  }

  @org.springframework.web.bind.annotation.RequestMapping(
      value = {
        "/api/v1/me/api-credentials",
        "/api/v1/me/api-credentials/{id}",
        "/api/v1/me/api-credentials/{id}/revocation"
      },
      method = {
        org.springframework.web.bind.annotation.RequestMethod.POST,
        org.springframework.web.bind.annotation.RequestMethod.PATCH,
        org.springframework.web.bind.annotation.RequestMethod.DELETE
      })
  ResponseEntity<?> unsupported(HttpServletRequest request) {
    String allow =
        request.getRequestURI().endsWith("/revocation")
            ? "PUT"
            : request.getRequestURI().endsWith("/api-credentials") ? "GET" : "GET, PUT";
    return ResponseEntity.status(405)
        .header("Cache-Control", "no-store")
        .header("Allow", allow)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(405, "METHOD_NOT_ALLOWED", "El método no está permitido."));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(CredentialNotFound.class)
  ResponseEntity<?> notFound() {
    return ResponseEntity.status(404)
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(404, "API_CREDENTIAL_NOT_FOUND", "No se encuentra la credencial."));
  }

  @PutMapping("/api/v1/me/api-credentials/{id}")
  public ResponseEntity<?> create(
      Principal principal, @PathVariable String id, HttpServletRequest http) throws IOException {
    acceptable(http);
    noQuery(http);
    UUID parsedId = identifier(id);
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
      var problem = ApiErrors.problem(400, "API_CREDENTIAL_INVALID", "El cuerpo no es válido.");
      problem.put(
          "errors",
          List.of(
              new com.apptolast.organization.domain.FieldError(
                  "body", "INVALID_VALUE", "Indica un cuerpo válido.")));
      return ResponseEntity.badRequest()
          .header("Cache-Control", "no-store")
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(problem);
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
