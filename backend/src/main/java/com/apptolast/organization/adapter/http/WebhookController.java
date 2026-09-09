package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.CreateWebhookUseCase;
import com.apptolast.organization.application.ManageWebhookUseCase;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session-only HTTP surface for the owner's outgoing webhooks. */
@RestController
public final class WebhookController {
  private static final String BASE = "/api/v1/me/webhooks";
  private static final int MAX_BODY_BYTES = 4096;

  private final CreateWebhookUseCase create;
  private final ManageWebhookUseCase manage;
  private final ObjectMapper json;

  public WebhookController(
      CreateWebhookUseCase create, ManageWebhookUseCase manage, ObjectMapper json) {
    this.create = create;
    this.manage = manage;
    this.json = json;
  }

  @PostMapping(value = BASE, consumes = "application/json")
  public ResponseEntity<?> create(Principal principal, HttpServletRequest http) throws IOException {
    noQuery(http);
    var request = readCreation(http);
    var created =
        create.create(
            principal.getName(), request.url(), request.description(), request.eventTypes());
    return ResponseEntity.created(URI.create(BASE + "/" + created.endpoint().id()))
        .header("Cache-Control", "no-store")
        .body(
            new CreationView(WebhookEndpointView.of(created.endpoint()), created.secret()));
  }

  /** Any query string on this route is a rejected field, never a silent filter. */
  private static void noQuery(HttpServletRequest request) {
    if (request.getQueryString() != null && !request.getQueryString().isEmpty())
      throw new com.apptolast.organization.domain.WebhookInvalidException(List.of("query"));
  }

  private CreationRequest readCreation(HttpServletRequest http) throws IOException {
    byte[] body;
    try (var input = http.getInputStream()) {
      body = input.readNBytes(MAX_BODY_BYTES + 1);
    }
    if (body.length > MAX_BODY_BYTES) throw new BodyTooLarge();
    JsonNode node;
    try {
      node =
          json.reader()
              .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(body);
    } catch (JsonProcessingException error) {
      throw new MalformedBody();
    }
    if (node == null || !node.isObject()) throw new MalformedBody();
    for (var field : (Iterable<String>) node::fieldNames) {
      if (!List.of("url", "description", "eventTypes").contains(field)) throw new MalformedBody();
    }
    return new CreationRequest(text(node, "url"), text(node, "description"), types(node));
  }

  private static String text(JsonNode node, String field) {
    var value = node.get(field);
    if (value == null || value.isNull()) return null;
    if (!value.isTextual()) throw new MalformedBody();
    return value.textValue();
  }

  private static List<String> types(JsonNode node) {
    var value = node.get("eventTypes");
    if (value == null || value.isNull()) return List.of();
    if (!value.isArray()) throw new MalformedBody();
    var types = new ArrayList<String>();
    for (var type : value) {
      if (!type.isTextual()) throw new MalformedBody();
      types.add(type.textValue());
    }
    return types;
  }

  @org.springframework.web.bind.annotation.GetMapping(BASE)
  public ResponseEntity<?> list(Principal principal) {
    return ok(new EndpointList(manage.list(principal.getName()).stream()
        .map(WebhookEndpointView::of)
        .toList()));
  }

  @org.springframework.web.bind.annotation.GetMapping(BASE + "/{id}")
  public ResponseEntity<?> find(Principal principal, @PathVariable String id) {
    return ok(WebhookEndpointView.of(manage.find(principal.getName(), identifier(id))));
  }

  @org.springframework.web.bind.annotation.PutMapping(BASE + "/{id}/status")
  public ResponseEntity<?> changeStatus(
      Principal principal, @PathVariable String id, HttpServletRequest http) throws IOException {
    var target = readStatus(http);
    return ok(
        WebhookEndpointView.of(
            manage.changeStatus(principal.getName(), identifier(id), target)));
  }

  @org.springframework.web.bind.annotation.DeleteMapping(BASE + "/{id}")
  public ResponseEntity<?> delete(Principal principal, @PathVariable String id) {
    manage.delete(principal.getName(), identifier(id));
    return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
  }

  @PostMapping(BASE + "/{id}/ping")
  public ResponseEntity<?> ping(Principal principal, @PathVariable String id) {
    return accepted(manage.ping(principal.getName(), identifier(id)));
  }

  @org.springframework.web.bind.annotation.GetMapping(BASE + "/{id}/deliveries")
  public ResponseEntity<?> deliveries(Principal principal, @PathVariable String id) {
    return ok(new DeliveryList(manage.deliveries(principal.getName(), identifier(id)).stream()
        .map(WebhookDeliveryView::of)
        .toList()));
  }

  @PostMapping(BASE + "/{id}/deliveries/{deliveryId}/redeliver")
  public ResponseEntity<?> redeliver(
      Principal principal, @PathVariable String id, @PathVariable String deliveryId) {
    return accepted(
        manage.redeliver(principal.getName(), identifier(id), identifier(deliveryId)));
  }

  private static ResponseEntity<?> ok(Object body) {
    return ResponseEntity.ok().header("Cache-Control", "no-store").body(body);
  }

  private static ResponseEntity<?> accepted(
      com.apptolast.organization.domain.WebhookDelivery delivery) {
    return ResponseEntity.accepted()
        .header("Cache-Control", "no-store")
        .body(new DeliveryView(WebhookDeliveryView.of(delivery)));
  }

  private String readStatus(HttpServletRequest http) throws IOException {
    byte[] body;
    try (var input = http.getInputStream()) {
      body = input.readNBytes(MAX_BODY_BYTES + 1);
    }
    JsonNode node;
    try {
      node =
          json.reader()
              .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
              .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(body);
    } catch (JsonProcessingException error) {
      throw new MalformedBody();
    }
    if (node == null || !node.isObject()) throw new MalformedBody();
    for (var field : (Iterable<String>) node::fieldNames) {
      if (!"status".equals(field)) throw new MalformedBody();
    }
    return text(node, "status");
  }

  /** A path that is not a canonical UUID is indistinguishable from a webhook that never existed. */
  private static java.util.UUID identifier(String raw) {
    try {
      var id = java.util.UUID.fromString(raw);
      if (!id.toString().equals(raw)) throw new IllegalArgumentException(raw);
      return id;
    } catch (IllegalArgumentException error) {
      throw new com.apptolast.organization.application.WebhookOperationException(
          com.apptolast.organization.application.WebhookOperationException.Code.NOT_FOUND);
    }
  }

  record EndpointList(List<WebhookEndpointView> items) {}

  record DeliveryList(List<WebhookDeliveryView> items) {}

  record DeliveryView(WebhookDeliveryView delivery) {}

  record CreationRequest(String url, String description, List<String> eventTypes) {}

  record CreationView(WebhookEndpointView endpoint, String secret) {
    @Override
    public String toString() {
      return "CreationView[endpoint=" + endpoint + ", secret=REDACTED]";
    }
  }

  static final class MalformedBody extends RuntimeException {}

  static final class BodyTooLarge extends RuntimeException {}

  @org.springframework.web.bind.annotation.ExceptionHandler(BodyTooLarge.class)
  ResponseEntity<?> tooLarge() {
    return ResponseEntity.status(413)
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                413, "WEBHOOK_TOO_LARGE", "El cuerpo supera los 4096 bytes permitidos."));
  }

  /**
   * The dispatcher raises its own 405 before resolving a handler, where a controller-local handler
   * never runs and the generic advice would turn it into a 500. Mapping the unsupported methods
   * explicitly keeps the answer inside the webhook contract.
   */
  @org.springframework.web.bind.annotation.RequestMapping(
      value = {BASE, BASE + "/{id}", BASE + "/{id}/status"},
      method = org.springframework.web.bind.annotation.RequestMethod.PATCH)
  ResponseEntity<?> methodNotAllowed(HttpServletRequest request) {
    return ResponseEntity.status(405)
        .header("Cache-Control", "no-store")
        .header("Allow", allowedOn(request.getRequestURI()))
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(405, "METHOD_NOT_ALLOWED", "El método no está permitido."));
  }

  private static String allowedOn(String path) {
    if (path.endsWith("/status")) return "PUT";
    return path.endsWith("/webhooks") ? "GET, POST" : "GET, DELETE";
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(MalformedBody.class)
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      com.apptolast.organization.domain.WebhookInvalidException.class)
  ResponseEntity<?> invalid(com.apptolast.organization.domain.WebhookInvalidException error) {
    var problem = ApiErrors.problem(400, "WEBHOOK_INVALID", "Revisa los campos indicados.");
    problem.put("errors", error.errors());
    return ResponseEntity.badRequest()
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      com.apptolast.organization.application.WebhookOperationException.class)
  ResponseEntity<?> operation(
      com.apptolast.organization.application.WebhookOperationException error) {
    var failure = Failure.of(error.code());
    return ResponseEntity.status(failure.status())
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(failure.status(), failure.code(), failure.message()));
  }

  /** The stable, leak-free rendering of every non-field rejection. */
  private record Failure(int status, String code, String message) {
    static Failure of(com.apptolast.organization.application.WebhookOperationException.Code code) {
      return switch (code) {
        case NOT_FOUND -> new Failure(404, "WEBHOOK_NOT_FOUND", "No se encuentra el webhook.");
        case DISABLED ->
            new Failure(409, "WEBHOOK_DISABLED", "Activa el webhook antes de esta operación.");
        case DELIVERY_PENDING ->
            new Failure(
                409, "WEBHOOK_DELIVERY_PENDING", "Ya hay una entrega pendiente para este webhook.");
        case CONNECTORS_DISABLED ->
            new Failure(
                503, "CONNECTORS_DISABLED", "Falta configuración del servidor para conectores.");
        case LIMIT ->
            new Failure(409, "WEBHOOK_LIMIT", "Has alcanzado el límite de cinco webhooks.");
        case URL_BLOCKED ->
            new Failure(400, "WEBHOOK_URL_BLOCKED", "La dirección de destino no está permitida.");
        case URL_UNRESOLVABLE ->
            new Failure(400, "WEBHOOK_URL_UNRESOLVABLE", "No se resuelve el host de destino.");
      };
    }
  }
}
