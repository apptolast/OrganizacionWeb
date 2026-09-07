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
  void s10_realValuesReadBeanChecksOwnedEntityAndReturnsAnAbsentSnapshot() {
    var project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'wiring-values-owner','P','','active',now(),now())",
        project);
    var read =
        context.getBean(com.apptolast.organization.application.ReadCustomFieldValuesUseCase.class);
    var value = read.get("wiring-values-owner", CustomizationScope.PROJECT, project, project);
    assertThat(value.entityId()).isEqualTo(project);
    assertThat(value.schema())
        .isEqualTo(new com.apptolast.organization.domain.CustomizationRevision(null, 0));
    assertThat(value.revision())
        .isEqualTo(new com.apptolast.organization.domain.CustomizationRevision(null, 0));
    assertThat(value.values()).isEmpty();
    assertThat(value.updatedAt()).isNull();
  }

  @org.junit.jupiter.api.BeforeEach
  void clearOwnPreferences() {
    jdbc.update("DELETE FROM customization_preferences");
  }

  @Test
  void s2_s3_s7_realCommandBeansShareThePostgresConfiguration() {
    var save =
        context.getBean(com.apptolast.organization.application.SaveCustomizationViewUseCase.class);
    var first =
        save.save(
            "wiring-command-owner",
            CustomizationScope.TASK,
            new com.apptolast.organization.domain.CustomizationRevision(null, 0),
            java.util.List.of());
    var create =
        context.getBean(com.apptolast.organization.application.CreateCustomFieldUseCase.class);
    var withField =
        create.create(
            "wiring-command-owner",
            CustomizationScope.TASK,
            new com.apptolast.organization.domain.CustomizationRevision(first.id(), 0),
            "Dato",
            com.apptolast.organization.domain.CustomFieldType.TEXT);
    var update =
        context.getBean(com.apptolast.organization.application.UpdateCustomFieldUseCase.class);
    var result =
        update.update(
            "wiring-command-owner",
            CustomizationScope.TASK,
            withField.customFields().getFirst().id(),
            new com.apptolast.organization.domain.CustomizationRevision(first.id(), 1),
            "Renombrado",
            false);
    assertThat(
            context
                .getBean(ReadCustomizationUseCase.class)
                .get("wiring-command-owner", CustomizationScope.TASK))
        .contains(result);
    assertThat(result.version()).isEqualTo(2);
    assertThat(result.customFields().getFirst().active()).isFalse();
  }

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
