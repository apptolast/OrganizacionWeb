package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.auth.username=export-socket-owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=http://127.0.0.1",
      "app.publisher.enabled=false"
    })
@org.testcontainers.junit.jupiter.Testcontainers
class ExportSocketTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"projects", "tasks", "workSessionIntervals", "plannedBlocks"})
  void s15_s20_largeValidExportTraversesTheUnchangedProxyTimeout(String collection)
      throws Exception {
    jdbc.update(
        "DELETE FROM work_session_intervals WHERE session_id IN (SELECT id FROM work_sessions WHERE owner_id='export-socket-owner')");
    jdbc.update("DELETE FROM work_sessions WHERE owner_id='export-socket-owner'");
    jdbc.update(
        "DELETE FROM planned_blocks WHERE project_id IN (SELECT id FROM projects WHERE owner_id='export-socket-owner')");
    jdbc.update(
        "DELETE FROM tasks WHERE project_id IN (SELECT id FROM projects WHERE owner_id='export-socket-owner')");
    jdbc.update("DELETE FROM projects WHERE owner_id='export-socket-owner'");
    int expectedRows =
        collection.equals("projects")
            ? 100000
            : collection.equals("tasks")
                ? 80000
                : collection.equals("plannedBlocks") ? 55000 : 99997;
    if (collection.equals("projects")) {
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) SELECT md5('socket-export-'||n)::uuid,'export-socket-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z' FROM generate_series(1,100000) n");
    } else if (collection.equals("tasks")) {
      var project = java.util.UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'export-socket-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z')",
          project);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) SELECT md5('socket-task-'||n)::uuid,?,'T','','pending',0,'2026-09-01Z','2026-09-01Z' FROM generate_series(1,80000) n",
          project);
    }
    if (collection.equals("workSessionIntervals")) {
      var project = java.util.UUID.randomUUID();
      var task = java.util.UUID.randomUUID();
      var session = java.util.UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'export-socket-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z')",
          project);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'T','','pending',0,'2026-09-01Z','2026-09-01Z')",
          task,
          project);
      jdbc.update(
          "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since) VALUES (?,'export-socket-owner',?,?,?,'2026-09-01Z',25,'2026-09-01T00:25:00Z','UTC','running',199995,'2026-09-01Z'::timestamptz+199994*interval '1 second',99997000000,'2026-09-01Z'::timestamptz+199994*interval '1 second')",
          session,
          project,
          task,
          session);
      jdbc.update(
          "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) SELECT ?,2*n,'2026-09-01Z'::timestamptz+(2*n-2)*interval '1 second','2026-09-01Z'::timestamptz+(2*n-1)*interval '1 second' FROM generate_series(1,99997) n",
          session);
    }
    if (collection.equals("plannedBlocks")) {
      var project = java.util.UUID.randomUUID();
      var task = java.util.UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'export-socket-owner','P','','idea',0,'2026-09-01Z','2026-09-01Z')",
          project);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,version,created_at,updated_at) VALUES (?,?,'T','','pending',0,'2026-09-01Z','2026-09-01Z')",
          task,
          project);
      jdbc.update(
          "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) SELECT md5('socket-block-'||n)::uuid,?,?,md5('socket-block-'||n)::uuid,'O','2026-09-01'::timestamp+(n-1)*interval '25 minutes','2026-09-01'::timestamp+n*interval '25 minutes','UTC','Z','Z',true,'2026-09-01Z'::timestamptz+(n-1)*interval '25 minutes','2026-09-01Z'::timestamptz+n*interval '25 minutes',25,'2026-09-01Z' FROM generate_series(1,55000) n",
          project,
          task);
    }
    Testcontainers.exposeHostPorts(port);
    // The only config substitution connects the real host API through Testcontainers' host bridge.
    var config =
        Files.readString(Path.of("..", "deploy", "nginx.conf"))
            .replace(
                "server backend:8080 resolve;",
                "server host.testcontainers.internal:" + port + ";");
    assertThat(config).contains("proxy_read_timeout 15s;");
    try (var proxy =
        new GenericContainer<>("nginx:1.30.4-alpine3.24")
            .withCopyToContainer(Transferable.of(config), "/etc/nginx/conf.d/default.conf")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/healthz").forStatusCode(204))) {
      proxy.start();
      var base = "http://" + proxy.getHost() + ":" + proxy.getMappedPort(8080);
      var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
      try (var client =
          HttpClient.newBuilder()
              .cookieHandler(cookies)
              .connectTimeout(Duration.ofSeconds(5))
              .build()) {
        var session =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/session")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(session.statusCode()).isEqualTo(200);
        var token = new ObjectMapper().readTree(session.body()).path("csrfToken").asText();
        var login =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/session"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("X-CSRF-TOKEN", token)
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            "username=export-socket-owner&password=test-only-secret"))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(204);
        var started = System.nanoTime();
        var response =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/v1/me/export"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Accept", "application/json")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray());
        System.out.println(
            "export-proxy: collection="
                + collection
                + " status="
                + response.statusCode()
                + " elapsedMillis="
                + (System.nanoTime() - started) / 1000000);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValueAsLong("Content-Length").orElseThrow())
            .isEqualTo(response.body().length);
        assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
        assertThat(response.headers().firstValue("Cache-Control").orElseThrow())
            .contains("no-store", "no-transform");
        var reader = new ObjectMapper();
        int rows = 0;
        try (var parser = reader.getFactory().createParser(response.body())) {
          while (parser.nextToken() != null) {
            if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.FIELD_NAME
                && parser.currentName().equals(collection)) {
              if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
                  parser.skipChildren();
                  rows++;
                }
              } else assertThat(parser.getIntValue()).isEqualTo(expectedRows);
            }
          }
        }
        assertThat(rows).isEqualTo(expectedRows);
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM outbox_events WHERE owner_id='export-socket-owner'",
                    Integer.class))
            .isZero();
      }
    }
  }
}
