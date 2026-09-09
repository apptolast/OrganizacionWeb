package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** The half of the rule engine that only PostgreSQL can answer: atomicity, races and crashes. */
@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
@Testcontainers
class AutomationExecutionTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired ExecuteAutomationsUseCase execute;
  @Autowired AutomationWork work;
  @Autowired CreateAutomationUseCase createRule;

  @Test
  void s19_theRunTheTaskTheEventAndTheCursorLandInOneSingleConfirmation() {
    var owner = owner();
    var project = project(owner, "active");
    var rule = statusRule(owner, project);
    var event = statusChanged(owner, project);

    execute.runCycle();

    var task = jdbc.queryForMap(automatedTask(), project);
    assertThat(task.get("completion_criterion")).isEqualTo("");
    assertThat(task.get("estimated_minutes")).isNull();
    assertThat(task.get("parent_id")).isNull();
    var emitted =
        jdbc.queryForMap(
            "SELECT * FROM outbox_events WHERE owner_id = ? AND event_type = 'TaskCreated.v1'",
            owner);
    assertThat(emitted.get("aggregate_id")).isEqualTo(project);
    assertThat(String.valueOf(emitted.get("payload"))).contains(String.valueOf(task.get("id")));
    var run = jdbc.queryForMap("SELECT * FROM automation_runs WHERE owner_id = ?", owner);
    assertThat(run)
        .containsEntry("rule_id", rule)
        .containsEntry("event_id", event)
        .containsEntry("attempt", 1)
        .containsEntry("status", "succeeded")
        .containsEntry("created_task_id", task.get("id"))
        .containsEntry("delivery_id", null)
        .containsEntry("error_code", null);
    assertThat(cursorOf(owner)).isEqualTo(event);
  }

  @Test
  void s23_twoConcurrentWorkersRunTheRuleAtMostOnceForTheEvent() throws Exception {
    var owner = owner();
    var project = project(owner, "active");
    statusRule(owner, project);
    statusChanged(owner, project);

    var gate = new CyclicBarrier(2);
    var failures = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
    var workers =
        List.of(worker(gate, failures), worker(gate, failures)).stream().map(Thread::new).toList();
    workers.forEach(Thread::start);
    for (var worker : workers) worker.join(30_000);

    assertThat(failures).as("no worker ends with an uncontrolled error").isEmpty();
    assertThat(count("SELECT count(*) FROM automation_runs WHERE owner_id = ?", owner))
        .isEqualTo(1);
    assertThat(count(countAutomatedTasks(), project)).isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM outbox_events WHERE owner_id = ?"
                    + " AND event_type = 'TaskCreated.v1'",
                owner))
        .isEqualTo(1);
  }

  @Test
  void s25_aCrashBeforeConfirmingLeavesTheCursorBehindAndTheRereadDoesNotDuplicate() {
    var owner = owner();
    var project = project(owner, "active");
    var rule = statusRule(owner, project);
    var event = statusChanged(owner, project);
    var before = cursorOf(owner);

    assertThatThrownBy(() -> work.commit(crashingCommit(owner, rule, project, event)))
        .isInstanceOf(StorageUnavailableException.class);

    assertThat(cursorOf(owner)).as("the cursor stayed on the previous event").isEqualTo(before);
    assertThat(count(countAutomatedTasks(), project)).isZero();

    execute.runCycle();

    assertThat(
            jdbc.queryForList(
                "SELECT status FROM automation_runs WHERE owner_id = ?", String.class, owner))
        .containsExactly("succeeded");
    assertThat(count(countAutomatedTasks(), project)).isEqualTo(1);
    assertThat(
            count(
                "SELECT count(*) FROM outbox_events WHERE owner_id = ?"
                    + " AND event_type = 'TaskCreated.v1'",
                owner))
        .isEqualTo(1);
    assertThat(cursorOf(owner)).isEqualTo(event);
  }

  @Test
  void s26_notifyWebhookQueuesTheOriginalEventForAnEndpointNotEvenSubscribedToIt() {
    var owner = owner();
    var project = project(owner, "active");
    var endpoint = endpoint(owner);
    notifyRule(owner, endpoint);
    var event = projectStatusChanged(owner, project);

    execute.runCycle();

    var delivery = jdbc.queryForMap("SELECT * FROM webhook_deliveries WHERE owner_id = ?", owner);
    assertThat(delivery)
        .containsEntry("endpoint_id", endpoint)
        .containsEntry("event_id", event)
        .containsEntry("event_type", "ProjectStatusChanged.v1")
        .containsEntry("status", "pending");
    assertThat(delivery.get("body"))
        .as("the body is the payload of the outbox, untransformed")
        .isEqualTo(payloadOf(event));
    var run = jdbc.queryForMap("SELECT * FROM automation_runs WHERE owner_id = ?", owner);
    assertThat(run)
        .containsEntry("status", "succeeded")
        .containsEntry("delivery_id", delivery.get("id"))
        .containsEntry("created_task_id", null)
        .containsEntry("error_code", null);
    assertThat(count("SELECT count(*) FROM outbox_events WHERE owner_id = ?", owner))
        .as("no new event is emitted")
        .isEqualTo(1);
    assertThat(cursorOf(owner)).isEqualTo(event);
  }

  /** An endpoint of the owner subscribed only to TaskCreated.v1, which the rule ignores. */
  private UUID endpoint(String owner) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO webhook_endpoints(id,owner_id,url,description,event_types,status,"
            + "secret_ciphertext,cursor_occurred_at,cursor_event_id,created_at,updated_at)"
            + " VALUES (?,?,'https://example.com/h','',ARRAY['TaskCreated.v1']::text[],'active',"
            + "'\\x01'::bytea,now(),?,now(),now())",
        id,
        owner,
        new UUID(0L, 0L));
    return id;
  }

  private UUID notifyRule(String owner, UUID endpoint) {
    return createRule
        .create(
            owner,
            new AutomationDraft(
                "Aviso", true, "ProjectStatusChanged.v1", null, new NotifyWebhookAction(endpoint)))
        .id();
  }

  private UUID projectStatusChanged(String owner, UUID project) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,"
            + "occurred_at,payload) VALUES (?,?,?,'ProjectStatusChanged.v1',1,now(),?::jsonb)",
        id,
        project,
        owner,
        "{\"projectId\":\"" + project + "\",\"status\":\"completed\"}");
    return id;
  }

  private String payloadOf(UUID event) {
    return jdbc.queryForObject(
        "SELECT payload::text FROM outbox_events WHERE event_id = ?", String.class, event);
  }

  private static String automatedTask() {
    return "SELECT * FROM tasks WHERE project_id = ? AND title = 'Revisar de nuevo'";
  }

  private static String countAutomatedTasks() {
    return "SELECT count(*) FROM tasks WHERE project_id = ? AND title = 'Revisar de nuevo'";
  }

  /**
   * A confirmation that creates the task and its event and then dies before landing: the run row
   * carries a status the CHECK of automation_runs refuses, so the write blows up once the task and
   * the outbox row are already there. That rollback is the crash the scenario describes.
   */
  private AutomationCommit crashingCommit(String owner, UUID rule, UUID project, UUID event) {
    var occurredAt = occurredAtOf(event);
    var run =
        new AutomationRun(
            UUID.randomUUID(),
            rule,
            owner,
            event,
            "TaskStatusChanged.v1",
            occurredAt,
            1,
            "crashed",
            null,
            null,
            null,
            Instant.now());
    var effect = new AutomationEffect.CreateTask(project, "Revisar de nuevo", "", null);
    return new AutomationCommit(
        owner,
        new AutomationCursor(occurredAt, event),
        List.of(new AutomationOutcome(run, effect)));
  }

  private Runnable worker(CyclicBarrier gate, List<Throwable> failures) {
    return () -> {
      try {
        gate.await();
        execute.runCycle();
      } catch (Exception | AssertionError failure) {
        failures.add(failure);
      }
    };
  }

  private String owner() {
    var owner = "execution-automations-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO automation_cursors(owner_id, occurred_at, event_id)"
            + " VALUES (?, now() - interval '1 hour', ?)",
        owner,
        new UUID(0L, 0L));
    return owner;
  }

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

  private UUID statusRule(String owner, UUID project) {
    return createRule
        .create(
            owner,
            new AutomationDraft(
                "Seguimiento",
                true,
                "TaskStatusChanged.v1",
                null,
                new CreateTaskAction(project, "Revisar de nuevo", null, null)))
        .id();
  }

  /** One outbox row of the owner, already committed, that the rule matches. */
  private UUID statusChanged(String owner, UUID project) {
    var id = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,estimated_minutes,status,"
            + "created_at,updated_at) VALUES (?,?,'Redactar informe','',NULL,'pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,"
            + "occurred_at,payload) VALUES (?,?,?,'TaskStatusChanged.v1',1,now(),?::jsonb)",
        id,
        project,
        owner,
        "{\"taskId\":\"" + task + "\",\"status\":\"completed\"}");
    return id;
  }

  private Instant occurredAtOf(UUID event) {
    return jdbc.queryForObject(
            "SELECT occurred_at FROM outbox_events WHERE event_id = ?",
            java.sql.Timestamp.class,
            event)
        .toInstant();
  }

  private UUID cursorOf(String owner) {
    return jdbc.queryForObject(
        "SELECT event_id FROM automation_cursors WHERE owner_id = ?", UUID.class, owner);
  }

  private long count(String sql, Object argument) {
    return jdbc.queryForObject(sql, Long.class, argument);
  }
}
