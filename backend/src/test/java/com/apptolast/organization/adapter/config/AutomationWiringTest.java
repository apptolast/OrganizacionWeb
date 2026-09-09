package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class AutomationWiringTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired CreateAutomationUseCase create;
  @Autowired ReadAutomationsUseCase read;
  @Autowired ReplaceAutomationUseCase replace;
  @Autowired DeleteAutomationUseCase delete;
  @Autowired SimulateAutomationUseCase simulate;
  @Autowired ReadAutomationRunsUseCase runs;
  @Autowired WebhookEndpointLookup endpoints;

  private UUID project(String owner, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at)"
            + " VALUES (?,?,'Marketing','',?,now(),now())",
        id,
        owner,
        status);
    return id;
  }

  private static AutomationDraft draft(UUID project) {
    return new AutomationDraft(
        "Seguimiento",
        true,
        "TaskCreated.v1",
        null,
        new CreateTaskAction(project, "Revisar {{task.title}}", null, 30));
  }

  @Test
  void s1_s11_s12_s14_theRealBeansCreateReadReplaceAndDeleteAgainstPostgres() {
    var owner = "wiring-automations-" + UUID.randomUUID();
    var project = project(owner, "active");
    var created = create.create(owner, draft(project));
    assertThat(created.version()).isEqualTo(1);
    assertThat(read.list(owner)).containsExactly(created);
    assertThat(read.get(owner, created.id())).isEqualTo(created);
    var replaced = replace.replace(owner, created.id(), 1, draft(project));
    assertThat(replaced.version()).isEqualTo(2);
    assertThat(runs.read(owner, created.id(), null).items()).isEmpty();
    delete.delete(owner, created.id(), 2);
    assertThat(read.list(owner)).isEmpty();
  }

  @Test
  void s4_s33_theRealBeansRefuseAForeignProjectAndSimulateWithoutWriting() {
    var owner = "wiring-automations-" + UUID.randomUUID();
    var stranger = "wiring-automations-" + UUID.randomUUID();
    var mine = project(owner, "active");
    var theirs = project(stranger, "active");
    assertThatThrownBy(() -> create.create(owner, draft(theirs)))
        .isInstanceOf(AutomationTargetNotFoundException.class);
    var simulation = simulate.simulate(owner, draft(mine));
    assertThat(simulation.evaluatedEvents()).isZero();
    assertThat(simulation.matches()).isEmpty();
    assertThat(read.list(owner)).isEmpty();
  }

  /**
   * Phase two contract: until feature 25 ships its endpoints, the extension point must answer «not
   * mine» for every endpoint, so no rule can be saved pointing at something that does not exist.
   */
  @Test
  void s5_theWebhookExtensionPointRejectsEveryEndpointUntilFeature25Exists() {
    var owner = "wiring-automations-" + UUID.randomUUID();
    assertThat(endpoints.isActiveEndpointOf(owner, UUID.randomUUID())).isFalse();
    var webhook =
        new AutomationDraft(
            "Aviso",
            true,
            "ProjectStatusChanged.v1",
            null,
            new NotifyWebhookAction(UUID.randomUUID()));
    assertThatThrownBy(() -> create.create(owner, webhook))
        .isInstanceOf(WebhookEndpointNotFoundException.class);
    assertThat(read.list(owner)).isEmpty();
  }
}
