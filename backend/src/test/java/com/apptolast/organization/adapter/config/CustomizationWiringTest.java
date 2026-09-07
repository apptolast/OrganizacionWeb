package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ReadCustomizationUseCase;
import com.apptolast.organization.domain.CustomizationScope;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
@Testcontainers
class CustomizationWiringTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ApplicationContext context;
  @Autowired JdbcTemplate jdbc;

  @Test
  void s1_s2_realApplicationReadBeanUsesMigratedPostgresWithScopeIsolation() {
    var id = UUID.fromString("6734a57b-4f2f-4dcb-8d29-0d430413ee12");
    var field = UUID.fromString("dc34a57b-4f2f-4dcb-8d29-0d430413ee12");
    jdbc.update(
        "INSERT INTO customization_preferences VALUES (?, 'owner', 'PROJECT', '[\"updatedAt\"]'::jsonb, ?::jsonb, 4, '0001-01-01T00:00:00.123456Z')",
        id,
        "[{\"id\":\"" + field + "\",\"label\":\"Dato\",\"type\":\"TEXT\",\"active\":false}]");
    var read = context.getBean(ReadCustomizationUseCase.class);
    var value = read.get("owner", CustomizationScope.PROJECT).orElseThrow();
    assertThat(value.id()).isEqualTo(id);
    assertThat(value.visibleFields()).containsExactly("updatedAt");
    assertThat(value.customFields().getFirst().id()).isEqualTo(field);
    assertThat(value.customFields().getFirst().active()).isFalse();
    assertThat(value.version()).isEqualTo(4);
    assertThat(value.updatedAt()).isEqualTo("0001-01-01T00:00:00.123456Z");
    assertThat(read.get("other-owner", CustomizationScope.PROJECT)).isEmpty();
    assertThat(read.get("owner", CustomizationScope.TASK)).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM customization_preferences", Integer.class))
        .isEqualTo(1);
  }
}
