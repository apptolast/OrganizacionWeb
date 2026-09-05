package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadProjectsUseCase;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.*;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class ProjectReadController {
  // PostgreSQL 17 timestamp.h: finite range, lower inclusive and upper exclusive.
  private static final Instant MIN_TIMESTAMP = Instant.parse("-4713-11-24T00:00:00Z");
  private static final Instant END_TIMESTAMP = Instant.parse("+294277-01-01T00:00:00Z");
  private final ReadProjectsUseCase read;
  private final ObjectMapper json;

  public ProjectReadController(ReadProjectsUseCase read, ObjectMapper json) {
    this.read = read;
    this.json = json;
  }

  public record PageResponse(List<ProjectSummary> items, String nextCursor) {}

  @GetMapping("/api/v1/projects")
  public PageResponse list(
      @RequestParam MultiValueMap<String, String> parameters, Principal principal) {
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor"))) throw invalid("query");
    ProjectPosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1) throw invalid("cursor");
      after = decode(parameters.getFirst("cursor"));
    }
    var page = read.list(principal.getName(), after);
    return new PageResponse(
        page.items(),
        page.next() == null
            ? null
            : Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    ("{\"createdAt\":\""
                            + page.next().createdAt()
                            + "\",\"id\":\""
                            + page.next().id()
                            + "\"}")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private ProjectPosition decode(String cursor) {
    try {
      if (cursor == null || !cursor.matches("[A-Za-z0-9_-]+")) throw invalid("cursor");
      byte[] bytes = Base64.getUrlDecoder().decode(cursor);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(cursor))
        throw invalid("cursor");
      JsonNode body =
          json.reader().with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(bytes);
      if (body == null
          || !body.isObject()
          || body.size() != 2
          || !body.has("createdAt")
          || !body.has("id")
          || !body.get("createdAt").isTextual()
          || !body.get("id").isTextual()) throw invalid("cursor");
      String timestamp = body.get("createdAt").textValue();
      Instant createdAt = Instant.parse(timestamp);
      if (!timestamp.endsWith("Z")
          || createdAt.getNano() % 1000 != 0
          || createdAt.isBefore(MIN_TIMESTAMP)
          || !createdAt.isBefore(END_TIMESTAMP)) throw invalid("cursor");
      String id = body.get("id").textValue();
      if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        throw invalid("cursor");
      return new ProjectPosition(createdAt, UUID.fromString(id));
    } catch (Exception error) {
      throw invalid("cursor");
    }
  }

  private static ValidationException invalid(String field) {
    return new ValidationException(
        List.of(new FieldError(field, "INVALID_VALUE", "El parámetro de consulta no es válido.")));
  }

  @GetMapping("/api/v1/projects/{id}")
  public org.springframework.http.ResponseEntity<Project> detail(
      @PathVariable String id, Principal principal) {
    if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid("id");
    var snapshot = read.detail(principal.getName(), UUID.fromString(id));
    return org.springframework.http.ResponseEntity.ok()
        .eTag("\"" + snapshot.project().id() + ":" + snapshot.version() + "\"")
        .body(snapshot.project());
  }
}
