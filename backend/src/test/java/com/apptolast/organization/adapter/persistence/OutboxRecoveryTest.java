package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.adapter.broker.RabbitBrokerPublisher;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class OutboxRecoveryTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Container
  static final GenericContainer<?> rabbit =
      new GenericContainer<>("rabbitmq:4.3.5-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "recovery-test")
          .withEnv("RABBITMQ_DEFAULT_PASS", "recovery-test-secret")
          .withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "+S 2:2")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1));

  static JdbcTemplate jdbc;
  static TransactionTemplate transaction;
  static ObjectMapper json;
  static final String QUEUE = "organization.project-created.v1";
  final List<String> errors = new ArrayList<>();
  final List<PublicationAttempt> auditEvents = new ArrayList<>();
  final PublicationAudit audit =
      new PublicationAudit() {
        public void event(PublicationAttempt attempt) {
          auditEvents.add(attempt);
        }

        public void workerError(String code) {
          errors.add(code);
        }
      };

  @BeforeAll
  static void prepare() throws Exception {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.exchangeDeclare("organization.events", "direct", true);
      channel.queueDeclare(QUEUE, true, false, false, Map.of("x-queue-type", "quorum"));
      channel.queueBind(QUEUE, "organization.events", "project.created.v1");
    }
  }

  @BeforeEach
  void clear() throws Exception {
    jdbc.execute("TRUNCATE block_changes,block_projections,planned_blocks, task_status_history, tasks, outbox_events, projects");
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queuePurge(QUEUE);
    }
  }

  static ConnectionFactory factory() {
    var factory = new ConnectionFactory();
    factory.setHost(rabbit.getHost());
    factory.setPort(rabbit.getMappedPort(5672));
    factory.setUsername("recovery-test");
    factory.setPassword("recovery-test-secret");
    return factory;
  }

  void seed() {
    var now = Instant.now().minusSeconds(1).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var project =
        Project.create(
            UUID.randomUUID(),
            "recovery-owner",
            "Recovery test project",
            "Private description",
            now);
    var event =
        new ProjectCreated(
            UUID.randomUUID(),
            project.id(),
            project.ownerId(),
            now,
            1,
            project.name(),
            "ProjectCreated.v1");
    new PostgresProjectCommit(jdbc, transaction, json).save(project, event);
  }

  void publish() {
    var broker =
        new RabbitBrokerPublisher(
            rabbit.getHost(),
            rabbit.getMappedPort(5672),
            "recovery-test",
            "recovery-test-secret",
            "/");
    new PublishOutbox(
            new PostgresOutboxWork(jdbc, transaction, json), broker, audit, Clock.systemUTC())
        .runCycle();
  }

  Map<String, Object> seedBlockPayload() throws Exception {
    seed();
    var row = jdbc.queryForMap("SELECT * FROM outbox_events");
    var body = new HashMap<String, Object>();
    body.put("eventId", row.get("event_id").toString());
    body.put("aggregateId", row.get("aggregate_id").toString());
    body.put("ownerId", row.get("owner_id"));
    body.put("occurredAt", ((java.sql.Timestamp) row.get("occurred_at")).toInstant().toString());
    body.put("schemaVersion", 1);
    body.put("type", "BlockPlanned.v1");
    body.put("blockId", UUID.randomUUID().toString());
    body.put("taskId", UUID.randomUUID().toString());
    body.put("startAt", "2030-01-07T10:00:00Z");
    body.put("endAt", "2030-01-07T11:00:00Z");
    body.put("zoneId", "Historical/Removed");
    body.put("durationMinutes", 60);
    jdbc.update(
        "UPDATE outbox_events SET event_type='BlockPlanned.v1',payload=?::jsonb",
        json.writeValueAsString(body));
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare(
          "organization.block-planned.v1", true, false, false, Map.of("x-queue-type", "quorum"));
      channel.queuePurge("organization.block-planned.v1");
    }
    return body;
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "extra",
        "type",
        "version",
        "id",
        "nonIncreasing",
        "mismatch",
        "outside",
        "missing:eventId",
        "missing:aggregateId",
        "missing:ownerId",
        "missing:occurredAt",
        "missing:schemaVersion",
        "missing:type",
        "missing:blockId",
        "missing:taskId",
        "missing:startAt",
        "missing:endAt",
        "missing:zoneId",
        "missing:durationMinutes"
      })
  void block_s37_persistsInvalidClassificationWithoutSendingToRealRabbit(String defect)
      throws Exception {
    var body = seedBlockPayload();
    if (defect.startsWith("missing:")) body.remove(defect.substring(8));
    else
      switch (defect) {
        case "extra" -> body.put("objective", "private");
        case "id" -> body.put("blockId", "1-1-1-1-1");
        case "nonIncreasing" -> body.put("endAt", body.get("startAt"));
        case "mismatch" -> body.put("durationMinutes", 59);
        case "outside" -> {
          body.put("durationMinutes", 1441);
          body.put("endAt", "2030-01-08T10:01:00Z");
        }
        default -> {}
      }
    jdbc.update(
        "UPDATE outbox_events SET payload=?::jsonb,event_type=?,schema_version=?",
        json.writeValueAsString(body),
        defect.equals("type") ? "BlockPlanned.v2" : "BlockPlanned.v1",
        defect.equals("version") ? 2 : 1);
    var before = jdbc.queryForMap("SELECT * FROM outbox_events");
    publish();
    var after = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(after.get("status")).isEqualTo("blocked");
    assertThat(after.get("last_error_code"))
        .isEqualTo(
            defect.equals("type") || defect.equals("version")
                ? "UNSUPPORTED_EVENT"
                : "INVALID_EVENT");
    assertThat(after.get("attempts")).isEqualTo(0L);
    assertThat(after.get("payload")).isEqualTo(before.get("payload"));
    assertThat(after.get("event_id")).isEqualTo(before.get("event_id"));
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      assertThat(channel.basicGet("organization.block-planned.v1", true)).isNull();
    }
    assertThat(errors).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"BROKER_NACK", "CONFIRM_TIMEOUT"})
  void block_s37_retriesOriginalHistoricalEventAfterUnconfirmedRealDelivery(String outcome)
      throws Exception {
    seedBlockPayload();
    var original = jdbc.queryForMap("SELECT * FROM outbox_events");
    var connection = factory().newConnection();
    var channel = org.mockito.Mockito.spy(connection.createChannel());
    var confirmed = new java.util.concurrent.atomic.AtomicBoolean();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              confirmed.set((boolean) invocation.callRealMethod());
              if (outcome.equals("CONFIRM_TIMEOUT"))
                throw new java.util.concurrent.TimeoutException();
              return false;
            })
        .when(channel)
        .waitForConfirms(5000);
    var wrapped = org.mockito.Mockito.spy(connection);
    org.mockito.Mockito.doReturn(channel).when(wrapped).createChannel();
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(wrapped))) {
      publish();
    } finally {
      connection.abort(1000);
    }
    assertThat(confirmed).isTrue();
    var pending = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(pending.get("status")).isEqualTo("pending");
    assertThat(pending.get("last_error_code")).isEqualTo(outcome);
    assertThat(pending.get("attempts")).isEqualTo(1L);
    assertThat(pending.get("published_at")).isNull();
    assertThat(pending.get("payload")).isEqualTo(original.get("payload"));
    assertThat(pending.get("event_id")).isEqualTo(original.get("event_id"));
    try (var reader = factory().newConnection();
        var reading = reader.createChannel()) {
      var first = reading.basicGet("organization.block-planned.v1", true);
      assertThat(first).isNotNull();
      assertCopy(first, original);
    }
    jdbc.update(
        "UPDATE outbox_events SET next_attempt_at=?", java.sql.Timestamp.from(Instant.EPOCH));
    publish();
    var complete = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(complete.get("status")).isEqualTo("published");
    assertThat(complete.get("attempts")).isEqualTo(2L);
    assertThat(complete.get("last_error_code")).isNull();
    assertThat(complete.get("published_at")).isNotNull();
    assertThat(complete.get("payload")).isEqualTo(original.get("payload"));
    assertThat(complete.get("event_id")).isEqualTo(original.get("event_id"));
    try (var reader = factory().newConnection();
        var reading = reader.createChannel()) {
      var repeated = reading.basicGet("organization.block-planned.v1", true);
      assertThat(repeated).isNotNull();
      assertCopy(repeated, original);
      assertThat(reading.basicGet("organization.block-planned.v1", true)).isNull();
    }
    assertThat(errors).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"BEFORE", "AFTER"})
  void s11_realProcessDeathReleasesClaimAndRetriesOriginalIdentity(String stage) throws Exception {
    seed();
    var original = jdbc.queryForMap("SELECT * FROM outbox_events");
    var root = Path.of("..", ".e2e-work").toAbsolutePath().normalize();
    Files.createDirectories(root);
    var scratch = Files.createTempDirectory(root, "recovery-").toAbsolutePath().normalize();
    assertThat(scratch.startsWith(root)).isTrue();
    var marker = scratch.resolve("ready");
    var args = scratch.resolve("java.args");
    var log = scratch.resolve("child.log");
    var classpath = System.getProperty("outbox.test.classpath");
    assertThat(classpath).isNotBlank();
    Files.writeString(
        args,
        "-cp\n\""
            + classpath.replace('\\', '/')
            + "\"\n"
            + PublisherCrashProcess.class.getName()
            + "\n"
            + stage
            + "\n\""
            + marker.toString().replace('\\', '/')
            + "\"\n");
    Process child = null;
    try {
      var executable =
          Path.of(
              System.getProperty("java.home"),
              "bin",
              System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
      var builder =
          new ProcessBuilder(executable.toString(), "@" + args)
              .redirectErrorStream(true)
              .redirectOutput(log.toFile());
      var env = builder.environment();
      env.put("TEST_DB_URL", postgres.getJdbcUrl());
      env.put("TEST_DB_USER", postgres.getUsername());
      env.put("TEST_DB_PASSWORD", postgres.getPassword());
      env.put("TEST_RABBIT_HOST", rabbit.getHost());
      env.put("TEST_RABBIT_PORT", rabbit.getMappedPort(5672).toString());
      env.put("TEST_RABBIT_USER", "recovery-test");
      env.put("TEST_RABBIT_PASSWORD", "recovery-test-secret");
      child = builder.start();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(25);
      while (!Files.exists(marker) && child.isAlive() && System.nanoTime() < deadline)
        Thread.sleep(25);
      assertThat(Files.exists(marker)).as("owned child reached real transaction barrier").isTrue();
      assertThat(child.isAlive()).isTrue();
      assertThat(jdbc.queryForMap("SELECT * FROM outbox_events")).isEqualTo(original);
      List<Map<String, Object>> locked =
          transaction.execute(
              status ->
                  jdbc.queryForList("SELECT event_id FROM outbox_events FOR UPDATE SKIP LOCKED"));
      assertThat(locked).isEmpty();
      try (var connection = factory().newConnection();
          var channel = connection.createChannel()) {
        var first = channel.basicGet(QUEUE, false);
        if (stage.equals("AFTER")) {
          assertThat(first).isNotNull();
          assertCopy(first, original);
          channel.basicNack(first.getEnvelope().getDeliveryTag(), false, true);
        } else assertThat(first).isNull();
      }
      child.destroyForcibly();
      assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
      assertThat(child.exitValue()).isNotZero();
      assertThat(jdbc.queryForMap("SELECT * FROM outbox_events")).isEqualTo(original);
      long releaseDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (System.nanoTime() < releaseDeadline) {
        List<Map<String, Object>> available =
            transaction.execute(
                status ->
                    jdbc.queryForList("SELECT event_id FROM outbox_events FOR UPDATE SKIP LOCKED"));
        if (!available.isEmpty()) break;
        Thread.sleep(25);
      }
      List<Map<String, Object>> released =
          transaction.execute(
              status -> jdbc.queryForList("SELECT event_id FROM outbox_events FOR UPDATE NOWAIT"));
      assertThat(released).hasSize(1);
      publish();
      var completed = jdbc.queryForMap("SELECT * FROM outbox_events");
      assertThat(completed.get("status")).isEqualTo("published");
      assertThat(completed.get("attempts")).isEqualTo(1L);
      assertThat(completed.get("payload")).isEqualTo(original.get("payload"));
      assertThat(completed.get("event_id")).isEqualTo(original.get("event_id"));
      assertThat(completed.get("published_at")).isNotNull();
      assertThat(errors).isEmpty();
      assertThat(auditEvents).hasSize(1);
      assertCopies(original, stage.equals("AFTER") ? 2 : 1);
    } finally {
      if (child != null && child.isAlive()) {
        child.destroyForcibly();
        assertThat(child.waitFor(10, TimeUnit.SECONDS)).isTrue();
      }
      for (var file : List.of(marker, args, log)) Files.deleteIfExists(file);
      Files.delete(scratch);
    }
  }

  void assertCopy(GetResponse message, Map<String, Object> original) throws Exception {
    assertThat(message.getProps().getMessageId()).isEqualTo(original.get("event_id").toString());
    assertThat(message.getProps().getDeliveryMode()).isEqualTo(2);
    assertThat(message.getProps().getContentType()).isEqualTo("application/json");
    assertThat(json.readTree(message.getBody()))
        .isEqualTo(json.readTree(original.get("payload").toString()));
  }

  @Test
  void s12_realAcceptanceSurvivesDatabaseRollbackAndRetryDuplicatesIdentity() throws Exception {
    seed();
    var original = jdbc.queryForMap("SELECT * FROM outbox_events");
    jdbc.execute(
        "CREATE FUNCTION recovery_reject_result() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'test result storage unavailable'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER recovery_failure BEFORE UPDATE ON outbox_events FOR EACH ROW EXECUTE FUNCTION recovery_reject_result()");
    try {
      publish();
      assertThat(jdbc.queryForMap("SELECT * FROM outbox_events")).isEqualTo(original);
      assertThat(auditEvents).isEmpty();
      assertThat(errors).containsExactly("STORAGE_UNAVAILABLE");
      try (var connection = factory().newConnection();
          var channel = connection.createChannel()) {
        var accepted = channel.basicGet(QUEUE, false);
        assertThat(accepted).isNotNull();
        assertCopy(accepted, original);
        channel.basicNack(accepted.getEnvelope().getDeliveryTag(), false, true);
      }
    } finally {
      jdbc.execute("DROP TRIGGER recovery_failure ON outbox_events");
      jdbc.execute("DROP FUNCTION recovery_reject_result()");
    }
    errors.clear();
    publish();
    assertThat(jdbc.queryForMap("SELECT * FROM outbox_events").get("status"))
        .isEqualTo("published");
    assertThat(jdbc.queryForMap("SELECT * FROM outbox_events").get("attempts")).isEqualTo(1L);
    assertThat(errors).isEmpty();
    assertThat(auditEvents).hasSize(1);
    assertCopies(original, 2);
  }

  void assertCopies(Map<String, Object> original, int expected) throws Exception {
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (channel.queueDeclarePassive(QUEUE).getMessageCount() != expected
          && System.nanoTime() < deadline) Thread.sleep(25);
      assertThat(channel.queueDeclarePassive(QUEUE).getMessageCount()).isEqualTo(expected);
      for (int index = 0; index < expected; index++) {
        var message = channel.basicGet(QUEUE, true);
        assertThat(message).isNotNull();
        assertCopy(message, original);
      }
      assertThat(channel.basicGet(QUEUE, true)).isNull();
    }
  }
}
