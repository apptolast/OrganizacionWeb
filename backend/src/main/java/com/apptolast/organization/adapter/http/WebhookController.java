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

  @PostMapping(BASE)
  public ResponseEntity<?> create(Principal principal, HttpServletRequest http) throws IOException {
    var request = readCreation(http);
    var created =
        create.create(
            principal.getName(), request.url(), request.description(), request.eventTypes());
    return ResponseEntity.created(URI.create(BASE + "/" + created.endpoint().id()))
        .header("Cache-Control", "no-store")
        .body(
            new CreationView(WebhookEndpointView.of(created.endpoint()), created.secret()));
  }

  private CreationRequest readCreation(HttpServletRequest http) throws IOException {
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

  record CreationRequest(String url, String description, List<String> eventTypes) {}

  record CreationView(WebhookEndpointView endpoint, String secret) {
    @Override
    public String toString() {
      return "CreationView[endpoint=" + endpoint + ", secret=REDACTED]";
    }
  }

  static final class MalformedBody extends RuntimeException {}

  @org.springframework.web.bind.annotation.ExceptionHandler(MalformedBody.class)
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest()
        .header("Cache-Control", "no-store")
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }
}
