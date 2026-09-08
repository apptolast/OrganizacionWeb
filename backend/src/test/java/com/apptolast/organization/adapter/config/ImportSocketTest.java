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
      "app.auth.username=import-socket-owner", "app.auth.password=test-only-secret",
      "app.public-origin=http://127.0.0.1", "app.publisher.enabled=false"
    })
@org.testcontainers.junit.jupiter.Testcontainers
class ImportSocketTest {
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

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.apptolast.organization.application.ImportDataUseCase previews;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.apptolast.organization.application.ApplyImportDataUseCase imports;

  private static org.testcontainers.images.builder.ImageFromDockerfile proxyImage;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "/api/v1/me/import/preview,401", "/api/v1/me/import,401",
    "/api/v1/projects,413", "/api/v1/me/import/preview/extra,413",
    "/api/v1/me/imports/by-key/6322225a-3bf8-42fd-a2b6-756e1a72928c,413"
  })
  void s32_onlyExactImportRoutesDelegateOversizedAnonymousRequestsToSecurity(
      String path, int status) throws Exception {
    try (var proxy = proxy();
        var client = HttpClient.newHttpClient()) {
      proxy.start();
      var response =
          client.send(
              HttpRequest.newBuilder(
                      URI.create(
                          "http://" + proxy.getHost() + ":" + proxy.getMappedPort(8080) + path))
                  .timeout(Duration.ofSeconds(45))
                  .expectContinue(true)
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[1048577]))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertThat(response.statusCode()).isEqualTo(status);
      if (status == 401) {
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
            .contains("application/problem+json");
        assertThat(new ObjectMapper().readTree(response.body()).path("code").asText())
            .isEqualTo("UNAUTHENTICATED");
      }
      org.mockito.Mockito.verifyNoInteractions(previews, imports);
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
                  "import-proxy-" + java.util.UUID.randomUUID(), true)
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

  @Test
  void s31_exact32MiBPreviewReachesTheRealApiThroughNginx() throws Exception {
    upload("/api/v1/me/import/preview", 33554432, false, 200);
  }

  @Test
  void s31_exact32MiBConfirmationReachesTheRealApiThroughNginx() throws Exception {
    upload("/api/v1/me/import", 33554432, false, 200);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"/api/v1/me/import/preview", "/api/v1/me/import"})
  void s31_oneAdditionalByteGetsTheApiProblem413(String path) throws Exception {
    upload(path, 33554433, false, 413);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "/api/v1/me/import/preview,33554432,200", "/api/v1/me/import,33554432,200",
    "/api/v1/me/import/preview,33554433,413", "/api/v1/me/import,33554433,413"
  })
  void s31_chunkedBodiesKeepBothInclusiveBoundaries(String path, long size, int status)
      throws Exception {
    upload(path, size, true, status);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"/api/v1/me/import/preview", "/api/v1/me/import"})
  void s32_realApplicationStartsBeforeTheProducerReleasesTheTail(String path) throws Exception {
    upload(path, 16384, true, 200, true);
  }

  private void upload(String path, long size, boolean chunked, int expectedStatus)
      throws Exception {
    upload(path, size, chunked, expectedStatus, false);
  }

  private void upload(String path, long size, boolean chunked, int expectedStatus, boolean gated)
      throws Exception {
    var reached = new java.util.concurrent.CountDownLatch(1);
    var sent = new java.util.concurrent.atomic.AtomicLong();
    if (gated) {
      org.mockito.stubbing.Answer<Object> real =
          call -> {
            assertThat(sent.get()).isLessThan(size);
            reached.countDown();
            return call.callRealMethod();
          };
      if (path.endsWith("preview"))
        org.mockito.Mockito.doAnswer(real)
            .when(previews)
            .preview(
                org.mockito.ArgumentMatchers.eq("import-socket-owner"),
                org.mockito.ArgumentMatchers.any());
      else
        org.mockito.Mockito.doAnswer(real)
            .when(imports)
            .apply(
                org.mockito.ArgumentMatchers.eq("import-socket-owner"),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }
    var file = Files.createTempFile("import-socket-", ".json");
    try {
      try (var out = Files.newOutputStream(file)) {
        new ExportJsonWriter()
            .empty("import-socket-owner", Instant.parse("2026-09-08T00:00:00Z"))
            .writeTo(out);
      }
      var padding = new byte[8192];
      Arrays.fill(padding, (byte) ' ');
      long remaining = size - Files.size(file);
      try (var out = Files.newOutputStream(file, StandardOpenOption.APPEND)) {
        while (remaining > 0) {
          int length = (int) Math.min(remaining, padding.length);
          out.write(padding, 0, length);
          remaining -= length;
        }
      }
      assertThat(Files.size(file)).isEqualTo(size);
      try (var proxy = proxy()) {
        proxy.start();
        assertThat(proxy.getContainerInfo().getHostConfig().getReadonlyRootfs()).isTrue();
        assertThat(proxy.getContainerInfo().getConfig().getUser()).isEqualTo("101:101");
        assertThat(proxy.getContainerInfo().getHostConfig().getCapDrop())
            .contains(com.github.dockerjava.api.model.Capability.ALL);
        assertThat(proxy.getContainerInfo().getHostConfig().getTmpFs())
            .containsKeys("/run", "/var/cache/nginx");
        var base = "http://" + proxy.getHost() + ":" + proxy.getMappedPort(8080);
        System.out.println(
            "import-proxy ownContainer="
                + proxy.getContainerId()
                + " apiPort="
                + port
                + " proxyPort="
                + proxy.getMappedPort(8080));
        try (var client =
            HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .version(HttpClient.Version.HTTP_1_1)
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
                              "username=import-socket-owner&password=test-only-secret"))
                      .build(),
                  HttpResponse.BodyHandlers.ofString());
          assertThat(login.statusCode()).isEqualTo(204);
          var authenticated =
              client.send(
                  HttpRequest.newBuilder(URI.create(base + "/api/session")).GET().build(),
                  HttpResponse.BodyHandlers.ofString());
          token = new ObjectMapper().readTree(authenticated.body()).path("csrfToken").asText();
          var publisher =
              chunked
                  ? HttpRequest.BodyPublishers.ofInputStream(
                      () -> {
                        try {
                          return new java.io.FilterInputStream(Files.newInputStream(file)) {
                            @Override
                            public int read(byte[] bytes, int offset, int length)
                                throws java.io.IOException {
                              if (gated && sent.get() > 0) {
                                try {
                                  if (!reached.await(5, java.util.concurrent.TimeUnit.SECONDS))
                                    throw new java.io.IOException(
                                        "Real application did not start before upload tail.");
                                } catch (InterruptedException error) {
                                  Thread.currentThread().interrupt();
                                  throw new java.io.IOException("Interrupted upload probe.", error);
                                }
                              }
                              int count = super.read(bytes, offset, Math.min(length, 8192));
                              if (count > 0) sent.addAndGet(count);
                              return count;
                            }
                          };
                        } catch (java.io.IOException error) {
                          throw new java.io.UncheckedIOException(error);
                        }
                      })
                  : HttpRequest.BodyPublishers.ofFile(file);
          assertThat(publisher.contentLength()).isEqualTo(chunked ? -1 : size);
          var request =
              HttpRequest.newBuilder(URI.create(base + path))
                  .timeout(Duration.ofSeconds(45))
                  .expectContinue(true)
                  .header("Content-Type", "application/json")
                  .header("X-CSRF-TOKEN", token)
                  .POST(publisher);
          if (path.equals("/api/v1/me/import")) {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            try (var input =
                new java.security.DigestInputStream(Files.newInputStream(file), digest)) {
              input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            request
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .header(
                    "X-Import-Content-SHA256", java.util.HexFormat.of().formatHex(digest.digest()));
          }
          var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
          System.out.println(
              "import-proxy path="
                  + path
                  + " bytes="
                  + size
                  + " chunked="
                  + chunked
                  + " status="
                  + response.statusCode());
          assertThat(response.statusCode()).isEqualTo(expectedStatus);
          if (gated) {
            assertThat(reached.getCount()).isZero();
            assertThat(sent.get()).isEqualTo(size);
          }
          var preview = new ObjectMapper().readTree(response.body());
          if (expectedStatus == 413) {
            assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .contains("application/problem+json");
            assertThat(preview.path("code").asText()).isEqualTo("IMPORT_TOO_LARGE");
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM import_receipts WHERE owner_id='import-socket-owner'",
                        Integer.class))
                .isZero();
          } else if (path.endsWith("preview")) {
            assertThat(preview.path("byteLength").asLong()).isEqualTo(size);
            assertThat(preview.path("owner").asText()).isEqualTo("import-socket-owner");
            assertThat(preview.path("counts").size()).isEqualTo(14);
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM import_receipts WHERE owner_id='import-socket-owner'",
                        Integer.class))
                .isZero();
          } else {
            assertThat(preview.path("byteLength").asLong()).isEqualTo(size);
            assertThat(preview.path("outcome").asText()).isEqualTo("NO_CHANGE");
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM import_receipts WHERE owner_id='import-socket-owner'",
                        Integer.class))
                .isEqualTo(1);
          }
          assertThat(
                  jdbc.queryForObject(
                      "SELECT count(*) FROM projects WHERE owner_id='import-socket-owner'",
                      Integer.class))
              .isZero();
        }
      }
    } finally {
      Files.deleteIfExists(file);
      jdbc.update("DELETE FROM import_receipts WHERE owner_id='import-socket-owner'");
    }
  }
}
