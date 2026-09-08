package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.adapter.persistence.ExportJsonWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
import org.testcontainers.junit.jupiter.Container;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "app.auth.username=import-scale-owner", "app.auth.password=test-only-secret",
      "app.public-origin=http://127.0.0.1", "app.publisher.enabled=false"
    })
@org.testcontainers.junit.jupiter.Testcontainers
class ImportScaleTest {
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

  private static org.testcontainers.images.builder.ImageFromDockerfile proxyImage;

  @Test
  void s16_exact32MiBAnd100000RecordsPreviewAndApplyThroughRealProxy() throws Exception {
    exerciseScale(false);
  }

  @Test
  void s16_deep60000TaskChainPreviewAndApplyThroughRealProxy() throws Exception {
    exerciseScale(true);
  }

  private void exerciseScale(boolean deepTasks) throws Exception {
    var beforeProjects =
        jdbc.queryForObject(
            "SELECT count(*) FROM projects WHERE owner_id='import-scale-owner'", Long.class);
    var beforeTasks =
        jdbc.queryForObject(
            "SELECT count(*) FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id='import-scale-owner'",
            Long.class);
    int projectCount = deepTasks ? 1 : 100000;
    int taskCount = deepTasks ? 60000 : 0;
    long projectNamespace = java.util.UUID.randomUUID().getMostSignificantBits();
    long taskNamespace = java.util.UUID.randomUUID().getMostSignificantBits();
    var projectId = new java.util.UUID(projectNamespace, 1);
    var key = java.util.UUID.randomUUID();
    var projectPrefix = new java.util.UUID(projectNamespace, 0).toString().substring(0, 18) + "%";
    var taskPrefix = new java.util.UUID(taskNamespace, 0).toString().substring(0, 18) + "%";
    var file = Files.createTempFile("import-scale-", ".json");
    try {
      var empty = new java.io.ByteArrayOutputStream();
      new ExportJsonWriter()
          .empty("import-scale-owner", Instant.parse("2026-09-08T00:00:00Z"))
          .writeTo(empty);
      var json = new ObjectMapper();
      var envelope = json.readTree(empty.toByteArray());
      ((com.fasterxml.jackson.databind.node.ObjectNode) envelope.path("counts"))
          .put("projects", projectCount);
      ((com.fasterxml.jackson.databind.node.ObjectNode) envelope.path("counts"))
          .put("tasks", taskCount);
      try (var out = Files.newOutputStream(file);
          var generator = json.getFactory().createGenerator(out)) {
        generator.writeStartObject();
        var names = envelope.fieldNames();
        while (names.hasNext()) {
          var name = names.next();
          generator.writeFieldName(name);
          if (!name.equals("data")) {
            generator.writeTree(envelope.get(name));
            continue;
          }
          generator.writeStartObject();
          var collections = envelope.path("data").fieldNames();
          while (collections.hasNext()) {
            var collection = collections.next();
            generator.writeArrayFieldStart(collection);
            if (collection.equals("projects"))
              for (int i = 0; i < projectCount; i++) {
                generator.writeStartObject();
                generator.writeStringField(
                    "id",
                    (deepTasks ? projectId : new java.util.UUID(projectNamespace, i + 1))
                        .toString());
                generator.writeStringField("name", "Proyecto " + i);
                generator.writeStringField("description", "");
                generator.writeStringField("status", "idea");
                generator.writeStringField("version", "0");
                generator.writeStringField("createdAt", "2026-09-08T00:00:00.000000Z");
                generator.writeStringField("updatedAt", "2026-09-08T00:00:00.000000Z");
                generator.writeEndObject();
              }
            if (collection.equals("tasks"))
              for (int i = 0; i < taskCount; i++) {
                generator.writeStartObject();
                generator.writeStringField(
                    "id", new java.util.UUID(taskNamespace, i + 1).toString());
                generator.writeStringField("projectId", projectId.toString());
                if (i == 0) generator.writeNullField("parentId");
                else
                  generator.writeStringField(
                      "parentId", new java.util.UUID(taskNamespace, i).toString());
                generator.writeStringField("title", "Tarea " + i);
                generator.writeStringField("completionCriterion", "Hecha");
                generator.writeNullField("estimatedMinutes");
                generator.writeStringField("status", "pending");
                generator.writeStringField("version", "0");
                generator.writeNullField("completedAt");
                generator.writeStringField("createdAt", "2026-09-08T00:00:00.000000Z");
                generator.writeStringField("updatedAt", "2026-09-08T00:00:00.000000Z");
                generator.writeEndObject();
              }
            generator.writeEndArray();
          }
          generator.writeEndObject();
        }
        generator.writeEndObject();
      }
      long remaining = 33554432 - Files.size(file);
      assertThat(remaining).isPositive();
      if (deepTasks) remaining = 0;
      byte[] padding = new byte[8192];
      Arrays.fill(padding, (byte) ' ');
      try (var out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
        while (remaining > 0) {
          int count = (int) Math.min(remaining, padding.length);
          out.write(padding, 0, count);
          remaining -= count;
        }
      }
      long byteLength = Files.size(file);
      if (deepTasks) assertThat(byteLength).isLessThan(33554432L);
      else assertThat(byteLength).isEqualTo(33554432L);
      var digest = java.security.MessageDigest.getInstance("SHA-256");
      try (var input = new java.security.DigestInputStream(Files.newInputStream(file), digest)) {
        input.transferTo(java.io.OutputStream.nullOutputStream());
      }
      var sha = java.util.HexFormat.of().formatHex(digest.digest());
      try (var proxy = proxy();
          var client =
              HttpClient.newBuilder()
                  .cookieHandler(new CookieManager())
                  .version(HttpClient.Version.HTTP_1_1)
                  .build()) {
        proxy.start();
        var base = "http://" + proxy.getHost() + ":" + proxy.getMappedPort(8080);
        var session =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/session")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        var token = json.readTree(session.body()).path("csrfToken").asText();
        assertThat(
                client
                    .send(
                        HttpRequest.newBuilder(URI.create(base + "/api/session"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .header("X-CSRF-TOKEN", token)
                            .POST(
                                HttpRequest.BodyPublishers.ofString(
                                    "username=import-scale-owner&password=test-only-secret"))
                            .build(),
                        HttpResponse.BodyHandlers.ofString())
                    .statusCode())
            .isEqualTo(204);
        session =
            client.send(
                HttpRequest.newBuilder(URI.create(base + "/api/session")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        token = json.readTree(session.body()).path("csrfToken").asText();
        for (var route : java.util.List.of("/api/v1/me/import/preview", "/api/v1/me/import")) {
          boolean apply = route.equals("/api/v1/me/import");
          var publisher =
              apply
                  ? HttpRequest.BodyPublishers.ofInputStream(
                      () -> {
                        try {
                          return Files.newInputStream(file);
                        } catch (java.io.IOException error) {
                          throw new java.io.UncheckedIOException(error);
                        }
                      })
                  : HttpRequest.BodyPublishers.ofFile(file);
          assertThat(publisher.contentLength()).isEqualTo(apply ? -1 : byteLength);
          var request =
              HttpRequest.newBuilder(URI.create(base + route))
                  .timeout(Duration.ofSeconds(45))
                  .expectContinue(true)
                  .header("Content-Type", "application/json")
                  .header("X-CSRF-TOKEN", token)
                  .POST(publisher);
          if (apply)
            request
                .header("Idempotency-Key", key.toString())
                .header("X-Import-Content-SHA256", sha);
          long start = System.nanoTime();
          var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
          System.out.println(
              "import-scale route="
                  + route
                  + " status="
                  + response.statusCode()
                  + " elapsedMs="
                  + (System.nanoTime() - start) / 1000000
                  + " bytes="
                  + Files.size(file)
                  + " projects="
                  + projectCount
                  + " tasks="
                  + taskCount);
          assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
          var result = json.readTree(response.body());
          assertThat(result.path("byteLength").longValue()).isEqualTo(byteLength);
          assertThat(result.path("fileSha256").asText()).isEqualTo(sha);
          assertThat(result.path(apply ? "insertedCounts" : "insertCounts"))
              .isEqualTo(envelope.path("counts"));
          assertThat(result.path("identicalCounts").size()).isEqualTo(14);
          for (var collection : (Iterable<String>) () -> envelope.path("counts").fieldNames()) {
            assertThat(result.path("identicalCounts").path(collection).isIntegralNumber()).isTrue();
            assertThat(result.path("identicalCounts").path(collection).longValue()).isZero();
          }
          if (apply) {
            assertThat(result.path("requestKey").asText()).isEqualTo(key.toString());
            assertThat(result.path("outcome").asText()).isEqualTo("IMPORTED");
          } else {
            assertThat(result.path("owner").asText()).isEqualTo("import-scale-owner");
            assertThat(result.path("counts")).isEqualTo(envelope.path("counts"));
          }
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM projects WHERE owner_id='import-scale-owner'",
                      Long.class))
              .isEqualTo(beforeProjects + (apply ? projectCount : 0L));
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id='import-scale-owner'",
                      Long.class))
              .isEqualTo(beforeTasks + (apply ? taskCount : 0L));
        }
        assertThat(
                jdbc.queryForObject(
                    "SELECT count(*) FROM import_receipts WHERE owner_id='import-scale-owner' AND request_key=?",
                    Long.class,
                    key))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
        if (deepTasks) {
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM tasks WHERE project_id=? AND parent_id IS NULL",
                      Long.class,
                      projectId))
              .isEqualTo(1L);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM tasks child JOIN tasks parent ON parent.id=child.parent_id WHERE child.project_id=? AND parent.project_id=?",
                      Long.class,
                      projectId,
                      projectId))
              .isEqualTo(59999L);
          assertThat(
                  jdbc.queryForObject(
                      "SELECT parent_id FROM tasks WHERE id=?",
                      java.util.UUID.class,
                      new java.util.UUID(taskNamespace, 60000)))
              .isEqualTo(new java.util.UUID(taskNamespace, 59999));
        }
      }
    } finally {
      try {
        jdbc.update(
            "DELETE FROM import_receipts WHERE owner_id='import-scale-owner' AND request_key=?",
            key);
        jdbc.update(
            "DELETE FROM tasks WHERE id::text LIKE ? AND project_id IN (SELECT id FROM projects WHERE owner_id='import-scale-owner' AND id::text LIKE ?)",
            taskPrefix,
            projectPrefix);
        jdbc.update(
            "DELETE FROM projects WHERE owner_id='import-scale-owner' AND id::text LIKE ?",
            projectPrefix);
      } finally {
        Files.deleteIfExists(file);
      }
    }
  }

  private GenericContainer<?> proxy() throws Exception {
    Testcontainers.exposeHostPorts(port);
    var config =
        Files.readString(Path.of("..", "deploy", "nginx.conf"))
            .replace(
                "server backend:8080 resolve;",
                "server host.testcontainers.internal:" + port + ";");
    if (proxyImage == null)
      proxyImage =
          new org.testcontainers.images.builder.ImageFromDockerfile(
                  "import-scale-proxy-" + java.util.UUID.randomUUID(), true)
              .withFileFromString("default.conf", config)
              .withDockerfileFromBuilder(
                  builder ->
                      builder
                          .from(
                              "ocholoko888/organizationweb-web@sha256:2568a6bf4c2347171df4433f50d5127ac1e4537385ad4b067c595c889eaf8d92")
                          .copy("default.conf", "/etc/nginx/conf.d/default.conf")
                          .build());
    return new GenericContainer<>(proxyImage)
        .withCreateContainerCmdModifier(
            command -> {
              command.withUser("101:101");
              command
                  .getHostConfig()
                  .withReadonlyRootfs(true)
                  .withCapDrop(com.github.dockerjava.api.model.Capability.ALL);
            })
        .withTmpFs(
            Map.of(
                "/run",
                "rw,noexec,nosuid,size=16777216",
                "/var/cache/nginx",
                "rw,noexec,nosuid,size=16777216"))
        .withExposedPorts(8080)
        .waitingFor(Wait.forHttp("/healthz").forStatusCode(204));
  }
}
