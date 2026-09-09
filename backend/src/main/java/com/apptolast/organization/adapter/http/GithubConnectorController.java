package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.IssueImportReceipt;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * Frontera HTTP del conector de GitHub. Todo lo que escribe pasa por sesión con CSRF; nada de lo
 * que devuelve tiene hueco para el token. Las respuestas llevan {@code Cache-Control: no-store}
 * porque describen una credencial de terceros y el trabajo importado con ella.
 */
@RestController
public final class GithubConnectorController {
  /** Nombre del caso de uso de importación cableado para GitHub; GitLab tiene el suyo. */
  public static final String GITHUB_IMPORTS = "githubImportIssues";

  private static final String CONNECTION = "/api/v1/me/connectors/github";
  private static final String IMPORTS = CONNECTION + "/imports";
  private static final String NO_STORE = "no-store, private";
  private static final Set<String> CONNECT_FIELDS = Set.of("repository", "token");
  private static final Set<String> IMPORT_FIELDS = Set.of("projectId");
  private static final Pattern CANONICAL_UUID =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

  private final ReadGithubConnectionUseCase read;
  private final ConnectGithubUseCase connect;
  private final DisconnectGithubUseCase disconnect;
  private final ImportIssuesUseCase importIssues;
  private final ReadIssueImportUseCase readImport;
  private final ObjectMapper json;

  public GithubConnectorController(
      ReadGithubConnectionUseCase read,
      ConnectGithubUseCase connect,
      DisconnectGithubUseCase disconnect,
      @Qualifier(GITHUB_IMPORTS) ImportIssuesUseCase importIssues,
      ReadIssueImportUseCase readImport,
      ObjectMapper json) {
    this.read = read;
    this.connect = connect;
    this.disconnect = disconnect;
    this.importIssues = importIssues;
    this.readImport = readImport;
    this.json = json;
  }

  // ------------------------------------------------------------------------------ conexión

  @GetMapping(CONNECTION)
  public ResponseEntity<ConnectionResponse> get(Principal principal) {
    return noStore().body(ConnectionResponse.of(read.execute(principal.getName())));
  }

  @PutMapping(value = CONNECTION, consumes = "application/json")
  public ResponseEntity<ConnectionResponse> put(
      Principal principal,
      @RequestBody(required = false) String raw,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    rejectQuery(parameters);
    var body = object(raw, CONNECT_FIELDS);
    var repository = text(body, "repository");
    var token = text(body, "token");
    return noStore()
        .body(ConnectionResponse.of(connect.execute(principal.getName(), repository, token)));
  }

  @DeleteMapping(CONNECTION)
  public ResponseEntity<Void> delete(
      Principal principal, @RequestParam MultiValueMap<String, String> parameters) {
    rejectQuery(parameters);
    disconnect.execute(principal.getName());
    return ResponseEntity.noContent().header("Cache-Control", NO_STORE).build();
  }

  // --------------------------------------------------------------------------- importación

  @PostMapping(value = IMPORTS, consumes = "application/json")
  public ResponseEntity<ImportResponse> startImport(
      Principal principal,
      @RequestBody(required = false) String raw,
      @RequestParam MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    rejectQuery(parameters);
    var body = object(raw, IMPORT_FIELDS);
    var receipt = importIssues.execute(principal.getName(), projectId(body));
    return ResponseEntity.created(java.net.URI.create(IMPORTS + "/" + receipt.id()))
        .header("Cache-Control", NO_STORE)
        .body(ImportResponse.of(receipt));
  }

  @GetMapping(IMPORTS + "/{id}")
  public ResponseEntity<ImportResponse> getImport(Principal principal, @PathVariable String id) {
    if (!CANONICAL_UUID.matcher(id).matches()) throw new IssueImportNotFoundException();
    return noStore()
        .body(ImportResponse.of(readImport.execute(principal.getName(), UUID.fromString(id))));
  }

  // --------------------------------------------------------------------------- lectura JSON

  private JsonNode object(String raw, Set<String> allowed) throws JsonProcessingException {
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    var body =
        json.reader()
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
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

  private static UUID projectId(JsonNode body) {
    var value = text(body, "projectId");
    if (!CANONICAL_UUID.matcher(value).matches()) throw invalid("projectId", "INVALID_FORMAT");
    return UUID.fromString(value);
  }

  private static void rejectQuery(MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw new ValidationException(List.of());
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }

  private static ResponseEntity.BodyBuilder noStore() {
    return ResponseEntity.ok().header("Cache-Control", NO_STORE);
  }

  // -------------------------------------------------------------------------------- errores

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler({MalformedBody.class, JsonProcessingException.class})
  ResponseEntity<Map<String, Object>> malformed() {
    return problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado.");
  }

  @ExceptionHandler(ConnectorsDisabledException.class)
  ResponseEntity<Map<String, Object>> disabled() {
    return problem(
        503, "CONNECTORS_DISABLED", "Los conectores no están configurados en este servidor.");
  }

  /**
   * El texto cifrado guardado no lo abre ninguna clave del llavero: la clave del servidor cambió
   * sin conservar la anterior. Es un fallo de configuración, no de quien llama, y quien lo lea
   * necesita saber que se arregla reconectando o restaurando la clave previa.
   */
  @ExceptionHandler(SecretUndecipherableException.class)
  ResponseEntity<Map<String, Object>> undecipherable() {
    return problem(
        503,
        "CONNECTOR_KEY_MISMATCH",
        "La clave de conectores del servidor no puede leer el token guardado. Vuelve a conectar.");
  }

  @ExceptionHandler(ConnectionNotFoundException.class)
  ResponseEntity<Map<String, Object>> noConnection() {
    return problem(404, "CONNECTION_NOT_FOUND", "No hay ninguna conexión de GitHub configurada.");
  }

  @ExceptionHandler(ConnectionInvalidException.class)
  ResponseEntity<Map<String, Object>> invalidConnection() {
    return problem(409, "CONNECTION_INVALID", "La conexión ya no es válida. Vuelve a conectarla.");
  }

  @ExceptionHandler(GithubTokenRejectedException.class)
  ResponseEntity<Map<String, Object>> tokenRejected() {
    return problem(409, "GITHUB_TOKEN_REJECTED", "GitHub rechazó el token.");
  }

  @ExceptionHandler(GithubRepositoryUnavailableException.class)
  ResponseEntity<Map<String, Object>> repositoryUnavailable() {
    return problem(
        409, "GITHUB_REPOSITORY_UNAVAILABLE", "El repositorio no está disponible con ese token.");
  }

  @ExceptionHandler(GithubUnavailableException.class)
  ResponseEntity<Map<String, Object>> githubUnavailable() {
    return problem(503, "GITHUB_UNAVAILABLE", "GitHub no responde. Inténtalo más tarde.");
  }

  @ExceptionHandler(IssueImportInProgressException.class)
  ResponseEntity<Map<String, Object>> importRunning() {
    return problem(409, "IMPORT_IN_PROGRESS", "Hay una importación en curso.");
  }

  @ExceptionHandler(IssueImportNotFoundException.class)
  ResponseEntity<Map<String, Object>> importNotFound() {
    return problem(404, "IMPORT_NOT_FOUND", "No se ha encontrado la importación.");
  }

  @ExceptionHandler(ConnectorRateLimitedException.class)
  ResponseEntity<Map<String, Object>> rateLimited(ConnectorRateLimitedException error) {
    return rateLimitedProblem(error.retryAfterSeconds(), null);
  }

  @ExceptionHandler(IssueImportFailedException.class)
  ResponseEntity<Map<String, Object>> importFailed(IssueImportFailedException error) {
    var receipt = error.receipt();
    if ("RATE_LIMITED".equals(receipt.errorCode()))
      return rateLimitedProblem(error.retryAfterSeconds(), receipt);
    var response =
        problem(statusOf(receipt.errorCode()), receipt.errorCode(), titleOf(receipt.errorCode()));
    return withReceipt(response, receipt);
  }

  private ResponseEntity<Map<String, Object>> rateLimitedProblem(
      int retryAfterSeconds, IssueImportReceipt receipt) {
    var body =
        ApiErrors.problem(
            503, "RATE_LIMITED", "GitHub limita las peticiones. Reintenta más tarde.");
    body.put("retryAfterSeconds", retryAfterSeconds);
    if (receipt != null) putReceipt(body, receipt);
    return ResponseEntity.status(503)
        .header("Cache-Control", NO_STORE)
        .header("Retry-After", String.valueOf(retryAfterSeconds))
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  private static ResponseEntity<Map<String, Object>> withReceipt(
      ResponseEntity<Map<String, Object>> response, IssueImportReceipt receipt) {
    putReceipt(response.getBody(), receipt);
    return response;
  }

  /** Los contadores parciales viajan con el problema para que la interfaz no vuelva a preguntar. */
  private static void putReceipt(Map<String, Object> body, IssueImportReceipt receipt) {
    body.put("importId", receipt.id());
    body.put("created", receipt.created());
    body.put("skipped", receipt.skipped());
    body.put("failed", receipt.failed());
  }

  private static int statusOf(String errorCode) {
    return switch (errorCode) {
      case "CONNECTION_INVALID", "GITHUB_REPOSITORY_UNAVAILABLE", "PROJECT_COMPLETED" -> 409;
      default -> 503;
    };
  }

  private static String titleOf(String errorCode) {
    return switch (errorCode) {
      case "CONNECTION_INVALID" -> "La conexión ya no es válida. Vuelve a conectarla.";
      case "GITHUB_REPOSITORY_UNAVAILABLE" -> "El repositorio no está disponible con ese token.";
      case "PROJECT_COMPLETED" -> "Reabre el proyecto en pausa para añadir tareas.";
      case "STORAGE_UNAVAILABLE" -> "El almacenamiento no está disponible. Inténtalo más tarde.";
      default -> "GitHub no responde. Inténtalo más tarde.";
    };
  }

  private static ResponseEntity<Map<String, Object>> problem(
      int status, String code, String title) {
    return ResponseEntity.status(status)
        .header("Cache-Control", NO_STORE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(status, code, title));
  }

  // -------------------------------------------------------------------------- respuestas

  /**
   * Cinco campos y ni uno más: no hay sitio donde esconder el token. Los nulos se serializan,
   * porque "sin importaciones todavía" es información que la interfaz necesita distinguir de "este
   * campo no ha venido".
   */
  public record ConnectionResponse(
      String repository,
      String login,
      String status,
      Instant connectedAt,
      ImportResponse lastImport) {
    static ConnectionResponse of(ConnectionView view) {
      return new ConnectionResponse(
          view.repository(),
          view.login(),
          view.status(),
          view.connectedAt(),
          ImportResponse.of(view.lastImport()));
    }
  }

  public record ImportResponse(
      UUID id,
      String source,
      UUID projectId,
      String projectPath,
      String status,
      int created,
      int skipped,
      int failed,
      boolean truncated,
      String errorCode,
      Instant startedAt,
      Instant finishedAt) {
    static ImportResponse of(IssueImportReceipt receipt) {
      return receipt == null
          ? null
          : new ImportResponse(
              receipt.id(),
              receipt.source(),
              receipt.projectId(),
              receipt.projectPath(),
              receipt.status(),
              receipt.created(),
              receipt.skipped(),
              receipt.failed(),
              receipt.truncated(),
              receipt.errorCode(),
              receipt.startedAt(),
              receipt.finishedAt());
    }
  }
}
