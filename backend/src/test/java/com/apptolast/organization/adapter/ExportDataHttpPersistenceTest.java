package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.auth.username=export-owner",
      "app.auth.password=export-test-only",
      "app.public-origin=http://localhost",
      "app.publisher.enabled=false"
    })
@Testcontainers
class ExportDataHttpPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"gzip;q=0", ", identity;q=1, "})
  void s18_socketKeepsIdentityWhenOnlyAnotherCodingIsForbiddenOrEmptyMembersOccur(String encoding)
      throws Exception {
    try (var client = authenticatedClient()) {
      var response =
          client.send(
              request("/api/v1/me/export").header("Accept-Encoding", encoding).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(200);
      assertPrivateHeaders(response);
      assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
      assertThat(response.headers().firstValueAsLong("Content-Length"))
          .hasValue(response.body().length);
      assertThat(json.readTree(response.body()).path("format").asText())
          .isEqualTo("organizationweb-export");
    }
  }

  @Test
  void s18_anonymousSocketAuthenticatesBeforeQueryOrNegotiation() throws Exception {
    try (var client = HttpClient.newHttpClient()) {
      var response =
          client.send(
              request("/api/v1/me/export?unexpected=true")
                  .header("Accept", "text/html")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(401);
      assertPrivateHeaders(response);
      assertThat(response.headers().firstValue("Content-Disposition")).isEmpty();
      assertThat(response.headers().firstValue("Content-Type").orElseThrow())
          .startsWith("application/problem+json");
      assertThat(json.readTree(response.body()).path("code").asText()).isEqualTo("UNAUTHENTICATED");
      assertThat(new String(response.body(), StandardCharsets.UTF_8)).doesNotContain("\"data\"");
    }
  }

  @Test
  void s17_lateCorruptCollectionReturnsOnlyProblemWithoutPartialSuccessBytes() throws Exception {
    var project = UUID.randomUUID();
    var configuration = UUID.randomUUID();
    insertProject(project, "export-owner", "PRIVATE-PREFIX-MUST-NOT-ESCAPE");
    jdbc.update(
        "INSERT INTO customization_preferences(id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,'export-owner','PROJECT','[]','[1]',0,now())",
        configuration);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text FROM customization_preferences c WHERE id=?",
            String.class,
            configuration);
    try (var client = authenticatedClient()) {
      var response =
          client.send(
              request("/api/v1/me/export").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(503);
      assertPrivateHeaders(response);
      assertThat(response.headers().firstValue("Content-Disposition")).isEmpty();
      assertThat(response.headers().firstValue("Content-Type").orElseThrow())
          .startsWith("application/problem+json");
      var problem = json.readTree(response.body());
      assertThat(problem.path("code").asText()).isEqualTo("STORAGE_UNAVAILABLE");
      assertThat(problem.path("status").intValue()).isEqualTo(503);
      assertThat(new String(response.body(), StandardCharsets.UTF_8))
          .doesNotContain("PRIVATE-PREFIX-MUST-NOT-ESCAPE", "\"schemaVersion\"", "\"data\"");
      assertThat(
              jdbc.queryForObject(
                  "SELECT row_to_json(c)::text FROM customization_preferences c WHERE id=?",
                  String.class,
                  configuration))
          .isEqualTo(before);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    } finally {
      jdbc.update("DELETE FROM customization_preferences WHERE id=?", configuration);
      jdbc.update("DELETE FROM projects WHERE id=?", project);
    }
  }

  @Test
  void s18_s19_authenticatedSocketReturnsExactUtf8BytesAndOnlyOwnedData() throws Exception {
    var project = UUID.randomUUID();
    var foreign = UUID.randomUUID();
    insertProject(project, "export-owner", "  Proyecto 🧭 雪  ");
    insertProject(foreign, "foreign-owner", "NO EXPORTAR");
    try (var client = authenticatedClient()) {
      var response =
          client.send(
              request("/api/v1/me/export").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(200);
      assertPrivateHeaders(response);
      var contentType =
          org.springframework.http.MediaType.parseMediaType(
              response.headers().firstValue("Content-Type").orElseThrow());
      assertThat(contentType.getType()).isEqualTo("application");
      assertThat(contentType.getSubtype()).isEqualTo("json");
      assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);
      assertThat(response.headers().firstValueAsLong("Content-Length"))
          .hasValue(response.body().length);
      assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
      assertThat(response.headers().firstValue("Content-Disposition").orElseThrow())
          .startsWith("attachment; filename=\"organizationweb-export-")
          .endsWith(".json\"");
      var body = json.readTree(response.body());
      assertThat(body.path("owner").asText()).isEqualTo("export-owner");
      assertThat(body.path("counts").path("projects").intValue()).isEqualTo(1);
      var row = body.path("data").path("projects").get(0);
      assertThat(row.path("id").asText()).isEqualTo(project.toString());
      assertThat(row.path("name").asText()).isEqualTo("  Proyecto 🧭 雪  ");
      assertThat(row.path("version").asText()).isEqualTo("9007199254740993");
      assertThat(new String(response.body(), StandardCharsets.UTF_8)).doesNotContain("NO EXPORTAR");
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    } finally {
      jdbc.update("DELETE FROM projects WHERE id IN (?,?)", project, foreign);
    }
  }

  private void insertProject(UUID id, String owner, String name) {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,?,?,'','idea',9007199254740993,now(),now())",
        id,
        owner,
        name);
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
  }

  private HttpClient authenticatedClient() throws Exception {
    var client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    var session =
        client.send(request("/api/session").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    assertThat(session.statusCode()).isEqualTo(200);
    var csrf = json.readTree(session.body());
    var login =
        client.send(
            request("/api/session")
                .header("Origin", "http://localhost")
                .header(csrf.path("csrfHeaderName").asText(), csrf.path("csrfToken").asText())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=export-owner&password=export-test-only"))
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());
    assertThat(login.statusCode()).isEqualTo(204);
    return client;
  }

  private void assertPrivateHeaders(HttpResponse<byte[]> response) {
    assertThat(response.headers().firstValue("Cache-Control"))
        .hasValue("no-store, private, no-transform");
    assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
  }
}
