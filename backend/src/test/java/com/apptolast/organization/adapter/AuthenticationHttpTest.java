package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=http://127.0.0.1"
    })
@Testcontainers
class AuthenticationHttpTest {
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
  CookieManager cookies;
  HttpClient client;

  @BeforeEach
  void browser() {
    jdbc.execute("TRUNCATE spring_session CASCADE");
    jdbc.execute("TRUNCATE outbox_events,projects");
    cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    client =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
  }

  HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void s1_anonymousSessionIsPublicExactAndPersisted() throws Exception {
    var response = get("/api/session");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    var body = json.readTree(response.body());
    assertThat(body.size()).isEqualTo(4);
    assertThat(body.get("authenticated").asBoolean()).isFalse();
    assertThat(body.get("username").isNull()).isTrue();
    assertThat(body.get("csrfToken").asText()).isNotBlank();
    assertThat(body.get("csrfHeaderName").asText()).isEqualTo("X-CSRF-TOKEN");
    assertThat(response.headers().allValues("Set-Cookie"))
        .singleElement()
        .asString()
        .contains("SESSION=", "Path=/api", "HttpOnly", "SameSite=Lax");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session", Long.class)).isEqualTo(1);
  }

  @AfterEach
  void closeBrowser() {
    client.close();
  }

  HttpResponse<String> post(String path, String body, String contentType, String csrf)
      throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", contentType);
    if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
    return client.send(
        request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  String cookie() {
    return cookies.getCookieStore().getCookies().stream()
        .filter(value -> value.getName().equals("SESSION"))
        .findFirst()
        .orElseThrow()
        .getValue();
  }

  @Test
  void s2_loginRotatesPersistedSessionAndConfirmsOwner() throws Exception {
    var anonymous = json.readTree(get("/api/session").body());
    var oldCookie = cookie();
    var response =
        post(
            "/api/session",
            "username=persona-a&password=test-only-secret",
            "application/x-www-form-urlencoded",
            anonymous.get("csrfToken").asText());
    assertThat(response.statusCode()).isEqualTo(204);
    assertThat(cookie()).isNotEqualTo(oldCookie);
    var body = json.readTree(get("/api/session").body());
    assertThat(body.get("authenticated").asBoolean()).isTrue();
    assertThat(body.get("username").asText()).isEqualTo("persona-a");
    assertThat(body.get("csrfToken").asText()).isNotBlank();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM spring_session WHERE principal_name='persona-a'", Long.class))
        .isEqualTo(1);
    assertThat(get("/api/v1/projects").statusCode()).isEqualTo(200);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "username=unknown&password=test-only-secret",
        "username=persona-a&password=wrong",
        "username=&password="
      })
  void s3_credentialsFailWithSamePublicProblem(String credentials) throws Exception {
    var csrf = json.readTree(get("/api/session").body()).get("csrfToken").asText();
    var response = post("/api/session", credentials, "application/x-www-form-urlencoded", csrf);
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(json.readTree(response.body()))
        .isEqualTo(
            json.valueToTree(
                com.apptolast.organization.adapter.http.ApiErrors.problem(
                    401, "UNAUTHENTICATED", "Identifícate para continuar.")));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM spring_session WHERE principal_name IS NOT NULL", Long.class))
        .isZero();
  }

  @Test
  void s4_basicCannotAuthenticateOrTriggerBrowserChallenge() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/projects"))
            .timeout(Duration.ofSeconds(10))
            .header(
                "Authorization",
                "Basic "
                    + java.util.Base64.getEncoder()
                        .encodeToString(
                            "persona-a:test-only-secret"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .GET()
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
    assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("UNAUTHENTICATED");
  }

  String login() throws Exception {
    var token = json.readTree(get("/api/session").body()).get("csrfToken").asText();
    assertThat(
            post(
                    "/api/session",
                    "username=persona-a&password=test-only-secret",
                    "application/x-www-form-urlencoded",
                    token)
                .statusCode())
        .isEqualTo(204);
    return json.readTree(get("/api/session").body()).get("csrfToken").asText();
  }

  @Test
  void s7_logoutDeletesServerSessionAndExpiresSameCookie() throws Exception {
    var csrf = login();
    var oldCookie = cookie();
    var response = post("/api/session/logout", "", "application/x-www-form-urlencoded", csrf);
    assertThat(response.statusCode()).isEqualTo(204);
    assertThat(response.headers().allValues("Set-Cookie"))
        .singleElement()
        .asString()
        .contains("SESSION=", "Path=/api", "Max-Age=0");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session", Long.class)).isZero();
    try (var oldClient = HttpClient.newHttpClient()) {
      var old =
          oldClient.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/projects"))
                  .timeout(Duration.ofSeconds(10))
                  .header("Cookie", "SESSION=" + oldCookie)
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(old.statusCode()).isEqualTo(401);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"login", "logout", "create", "edit", "state"})
  void s9_csrfRejectsEveryUnsafeOperation(String operation) throws Exception {
    if (!operation.equals("login")) login();
    else get("/api/session");
    String path =
        switch (operation) {
          case "login" -> "/api/session";
          case "logout" -> "/api/session/logout";
          case "create" -> "/api/v1/projects";
          case "edit" -> "/api/v1/projects/00000000-0000-0000-0000-000000000000";
          default -> "/api/v1/projects/00000000-0000-0000-0000-000000000000/status";
        };
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(
                operation.equals("edit") || operation.equals("state") ? "PUT" : "POST",
                HttpRequest.BodyPublishers.ofString("{}"));
    var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("CSRF_INVALID");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
  }

  @Test
  void s6_s10_expiredSessionReturns401BeforeCsrfAndCannotWrite() throws Exception {
    login();
    jdbc.update("UPDATE spring_session SET last_access_time=0,expiry_time=0");
    var response =
        post("/api/v1/projects", "{\"name\":\"Must not exist\"}", "application/json", null);
    assertThat(response.statusCode()).isEqualTo(401);
    assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("UNAUTHENTICATED");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
    assertThat(get("/api/v1/projects").statusCode()).isEqualTo(401);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"login", "logout"})
  void s12_storageFailureNeverConfirmsSessionAndPreservesRetryCookie(String operation)
      throws Exception {
    String csrf =
        operation.equals("logout")
            ? login()
            : json.readTree(get("/api/session").body()).get("csrfToken").asText();
    var previousCookie = cookie();
    String event = operation.equals("logout") ? "DELETE" : "UPDATE";
    String condition = operation.equals("logout") ? "" : " WHEN (NEW.PRINCIPAL_NAME IS NOT NULL)";
    jdbc.execute(
        "CREATE FUNCTION reject_session() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'synthetic secret SQL error'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reject_session BEFORE "
            + event
            + " ON spring_session FOR EACH ROW"
            + condition
            + " EXECUTE FUNCTION reject_session()");
    try {
      var response =
          operation.equals("logout")
              ? post("/api/session/logout", "", "application/x-www-form-urlencoded", csrf)
              : post(
                  "/api/session",
                  "username=persona-a&password=test-only-secret",
                  "application/x-www-form-urlencoded",
                  csrf);
      assertThat(response.statusCode()).isEqualTo(503);
      assertThat(json.readTree(response.body()).get("code").asText())
          .isEqualTo("SESSION_UNAVAILABLE");
      assertThat(response.body()).doesNotContain("synthetic", "SQL", "secret");
      assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
      assertThat(cookie()).isEqualTo(previousCookie);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM spring_session WHERE principal_name='persona-a'",
                  Long.class))
          .isEqualTo(operation.equals("logout") ? 1 : 0);
    } finally {
      jdbc.execute("DROP TRIGGER reject_session ON spring_session");
      jdbc.execute("DROP FUNCTION reject_session()");
    }
    if (operation.equals("logout")) {
      assertThat(
              post("/api/session/logout", "", "application/x-www-form-urlencoded", csrf)
                  .statusCode())
          .isEqualTo(204);
    } else {
      login();
      assertThat(get("/api/v1/projects").statusCode()).isEqualTo(200);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s1_storageFailureCannotMasqueradeAsAnonymous(boolean existingCookie) throws Exception {
    if (existingCookie) get("/api/session");
    jdbc.execute("ALTER TABLE spring_session RENAME TO unavailable_session_fixture");
    try {
      var response = get("/api/session");
      assertThat(response.statusCode()).isEqualTo(503);
      var body = json.readTree(response.body());
      assertThat(body.get("code").asText()).isEqualTo("SESSION_UNAVAILABLE");
      assertThat(body.has("authenticated")).isFalse();
      assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
    } finally {
      jdbc.execute("ALTER TABLE unavailable_session_fixture RENAME TO spring_session");
    }
  }

  @Test
  void s8_getLogoutDoesNotInvalidateSession() throws Exception {
    login();
    get("/api/session/logout");
    assertThat(json.readTree(get("/api/session").body()).get("authenticated").asBoolean()).isTrue();
    assertThat(get("/api/v1/projects").statusCode()).isEqualTo(200);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"login", "logout", "project"})
  void s11_foreignOriginCannotBypassProtectionWithValidCsrf(String operation) throws Exception {
    var token =
        operation.equals("login")
            ? json.readTree(get("/api/session").body()).get("csrfToken").asText()
            : login();
    String path =
        operation.equals("login")
            ? "/api/session"
            : operation.equals("logout") ? "/api/session/logout" : "/api/v1/projects";
    var response =
        client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Origin", "https://foreign.example")
                .header("X-CSRF-TOKEN", token)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=persona-a&password=test-only-secret"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(json.readTree(response.body()).get("code").asText()).isEqualTo("UNTRUSTED_ORIGIN");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
    assertThat(json.readTree(get("/api/session").body()).get("authenticated").asBoolean())
        .isEqualTo(!operation.equals("login"));
  }

  @Test
  void s2_s18_loginRefreshesCsrfBeforeCreatingOwnProject() throws Exception {
    var old = json.readTree(get("/api/session").body()).get("csrfToken").asText();
    assertThat(
            post(
                    "/api/session",
                    "username=persona-a&password=test-only-secret",
                    "application/x-www-form-urlencoded",
                    old)
                .statusCode())
        .isEqualTo(204);
    assertThat(
            post("/api/v1/projects", "{\"name\":\"Own project\"}", "application/json", old)
                .statusCode())
        .isEqualTo(403);
    var fresh = json.readTree(get("/api/session").body()).get("csrfToken").asText();
    var created = post("/api/v1/projects", "{\"name\":\"Own project\"}", "application/json", fresh);
    assertThat(created.statusCode()).isEqualTo(201);
    assertThat(json.readTree(created.body()).get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isEqualTo(1);
    assertThat(json.readTree(get("/api/v1/projects").body()).get("items").size()).isEqualTo(1);
  }
}
