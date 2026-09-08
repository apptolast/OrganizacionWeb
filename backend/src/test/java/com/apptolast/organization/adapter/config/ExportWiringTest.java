package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ExportDataUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
@Testcontainers
class ExportWiringTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ExportDataUseCase export;
  @Autowired JdbcTemplate jdbc;

  @Test
  void s1_s2_realBeanPreparesTheOwnedDatabaseWithTheHttpControllerPresent() throws Exception {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES (?,'export-wiring-owner','Propio','','idea',0,'2026-09-01Z','2026-09-01Z')",
        id);
    var file = export.prepare("export-wiring-owner");
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var json = new ObjectMapper().readTree(output.toByteArray());
    assertThat(json.path("owner").asText()).isEqualTo("export-wiring-owner");
    assertThat(json.path("data").size()).isEqualTo(14);
    assertThat(json.path("counts").path("projects").intValue()).isEqualTo(1);
    assertThat(json.at("/data/projects/0/id").asText()).isEqualTo(id.toString());
    assertThat(file.contentLength()).isEqualTo(output.size());
    assertThat(file.filename()).matches("organizationweb-export-v1-[0-9]{8}T[0-9]{12}Z\\.json");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id='export-wiring-owner'",
                Integer.class))
        .isZero();
  }
}
