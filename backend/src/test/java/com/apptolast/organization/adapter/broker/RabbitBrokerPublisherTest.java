package com.apptolast.organization.adapter.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.DeliveryOutcome;
import com.apptolast.organization.domain.OutboxMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class RabbitBrokerPublisherTest {
  @Container
  static final GenericContainer<?> rabbit =
      new GenericContainer<>("rabbitmq:4.3.5-management-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "broker-test")
          .withEnv("RABBITMQ_DEFAULT_PASS", "broker-test-secret")
          .withEnv("RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS", "+S 2:2")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*\\n", 1));

  final ObjectMapper json = new ObjectMapper();

  @Test
  void reschedule_s26_routesOriginalThirteenFieldsToDurableQuorumQueue() throws Exception {
    var base = message();
    var payload = new java.util.HashMap<>(base.payload());
    payload.remove("name");
    payload.put("type", "BlockChanged.v1");
    payload.put("changeId", UUID.randomUUID().toString());
    payload.put("blockId", UUID.randomUUID().toString());
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("kind", "RESCHEDULED");
    payload.put("revision", 2L);
    payload.put(
        "before",
        Map.of(
            "startAt",
            "2030-01-07T10:00:00Z",
            "endAt",
            "2030-01-07T11:00:00Z",
            "zoneId",
            "Historical/Removed",
            "durationMinutes",
            60));
    payload.put(
        "after",
        Map.of(
            "startAt",
            "2030-01-07T12:00:00Z",
            "endAt",
            "2030-01-07T13:00:00Z",
            "zoneId",
            "Historical/Removed",
            "durationMinutes",
            60));
    var event =
        new OutboxMessage(
            base.eventId(),
            base.aggregateId(),
            base.ownerId(),
            base.occurredAt(),
            "BlockChanged.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(event)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare(
          "organization.block-changed.v1", true, false, false, Map.of("x-queue-type", "quorum"));
      var delivered = channel.basicGet("organization.block-changed.v1", true);
      assertThat(delivered).isNotNull();
      assertThat(delivered.getBody())
          .isEqualTo(event.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      var body = json.readTree(delivered.getBody());
      assertThat(body.size()).isEqualTo(13);
      assertThat(body.get("before").size()).isEqualTo(4);
      assertThat(body.get("after").size()).isEqualTo(4);
      assertThat(delivered.getEnvelope().getRoutingKey()).isEqualTo("block.changed.v1");
      assertThat(delivered.getProps().getMessageId()).isEqualTo(event.eventId().toString());
      assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(delivered.getProps().getContentType()).isEqualTo("application/json");
    }
  }

  @Test
  void block_s36_routesOriginalTwelveFieldsToDurableQuorumQueue() throws Exception {
    var base = message();
    var payload = new java.util.HashMap<>(base.payload());
    payload.remove("name");
    payload.put("type", "BlockPlanned.v1");
    payload.put("blockId", UUID.randomUUID().toString());
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("startAt", "2030-01-07T10:00:00Z");
    payload.put("endAt", "2030-01-07T11:00:00Z");
    payload.put("zoneId", "Historical/Removed");
    payload.put("durationMinutes", 60);
    var event =
        new OutboxMessage(
            base.eventId(),
            base.aggregateId(),
            base.ownerId(),
            base.occurredAt(),
            "BlockPlanned.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(event)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDeclare(
          "organization.block-planned.v1", true, false, false, Map.of("x-queue-type", "quorum"));
      var delivered = channel.basicGet("organization.block-planned.v1", true);
      assertThat(delivered).isNotNull();
      assertThat(delivered.getBody())
          .isEqualTo(event.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertThat(json.readTree(delivered.getBody()).size()).isEqualTo(12);
      assertThat(delivered.getEnvelope().getRoutingKey()).isEqualTo("block.planned.v1");
      assertThat(delivered.getProps().getMessageId()).isEqualTo(event.eventId().toString());
      assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(delivered.getProps().getContentType()).isEqualTo("application/json");
    }
  }

  OutboxMessage message() throws Exception {
    UUID eventId = UUID.randomUUID(), aggregateId = UUID.randomUUID();
    Instant now = Instant.parse("2026-09-05T12:00:00Z");
    Map<String, Object> payload =
        Map.of(
            "eventId",
            eventId.toString(),
            "aggregateId",
            aggregateId.toString(),
            "ownerId",
            "owner-canary",
            "occurredAt",
            now.toString(),
            "schemaVersion",
            1,
            "name",
            "name-canary",
            "type",
            "ProjectCreated.v1");
    return new OutboxMessage(
        eventId,
        aggregateId,
        "owner-canary",
        now,
        "ProjectCreated.v1",
        1,
        json.writeValueAsString(payload),
        payload,
        0);
  }

  ConnectionFactory factory() {
    var factory = new ConnectionFactory();
    factory.setHost(rabbit.getHost());
    factory.setPort(rabbit.getMappedPort(5672));
    factory.setUsername("broker-test");
    factory.setPassword("broker-test-secret");
    return factory;
  }

  RabbitBrokerPublisher publisher() {
    return new RabbitBrokerPublisher(
        rabbit.getHost(), rabbit.getMappedPort(5672), "broker-test", "broker-test-secret", "/");
  }

  @Test
  void s1_confirmsPersistentOriginalJsonAndMetadataOnRealBroker() throws Exception {
    var event = message();
    assertThat(publisher().publish(event)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var received = channel.basicGet("organization.project-created.v1", true);
      assertThat(received).isNotNull();
      assertThat(received.getProps().getMessageId()).isEqualTo(event.eventId().toString());
      assertThat(received.getProps().getContentType()).isEqualTo("application/json");
      assertThat(received.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(json.readTree(received.getBody())).isEqualTo(json.readTree(event.json()));
      assertThat(json.readTree(received.getBody()).has("description")).isFalse();
    }
  }

  @Test
  void s6_realMandatoryReturnWinsOverPositiveConfirm() throws Exception {
    var connection = factory().newConnection();
    var channel = org.mockito.Mockito.spy(connection.createChannel());
    var positiveConfirm = new java.util.concurrent.atomic.AtomicBoolean();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              boolean confirmed = (boolean) invocation.callRealMethod();
              positiveConfirm.set(confirmed);
              return confirmed;
            })
        .when(channel)
        .waitForConfirms(5000);
    var wrappedConnection = org.mockito.Mockito.spy(connection);
    org.mockito.Mockito.doReturn(channel).when(wrappedConnection).createChannel();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              channel.queueUnbind(
                  "organization.project-created.v1", "organization.events", "project.created.v1");
              return invocation.callRealMethod();
            })
        .when(channel)
        .basicPublish(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyBoolean(),
            org.mockito.ArgumentMatchers.any(com.rabbitmq.client.AMQP.BasicProperties.class),
            org.mockito.ArgumentMatchers.any(byte[].class));
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(wrappedConnection))) {
      assertThat(publisher().publish(message())).isEqualTo(DeliveryOutcome.UNROUTABLE);
      assertThat(positiveConfirm).isTrue();
    } finally {
      connection.abort(1000);
    }
  }

  @Test
  void s21_incompatibleQueueIsPreservedAndReported() throws Exception {
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      channel.queueDelete("organization.project-created.v1");
      channel.queueDeclare(
          "organization.project-created.v1", true, false, false, Map.of("x-queue-type", "classic"));
      channel.basicPublish(
          "",
          "organization.project-created.v1",
          null,
          "retained-canary".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      try {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher().publish(message()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("topology_mismatch");
        assertThat(channel.basicGet("organization.project-created.v1", true).getBody())
            .isEqualTo("retained-canary".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      } finally {
        channel.queueDelete("organization.project-created.v1");
      }
    }
  }

  @Test
  void edit_s16_routesUpdatedToDedicatedDurableQueue() throws Exception {
    var source = message();
    var payload = new java.util.HashMap<>(source.payload());
    payload.put("type", "ProjectUpdated.v1");
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "ProjectUpdated.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(event)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var received = channel.basicGet("organization.project-updated.v1", true);
      assertThat(received).isNotNull();
      assertThat(received.getEnvelope().getRoutingKey()).isEqualTo("project.updated.v1");
      assertThat(received.getProps().getMessageId()).isEqualTo(event.eventId().toString());
      assertThat(received.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(received.getProps().getContentType()).isEqualTo("application/json");
      assertThat(received.getBody())
          .isEqualTo(event.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  void states_s13_routesStatusChangedToDedicatedQueue() throws Exception {
    var source = message();
    var payload = new java.util.HashMap<>(source.payload());
    payload.remove("name");
    payload.put("type", "ProjectStatusChanged.v1");
    payload.put("fromStatus", "active");
    payload.put("toStatus", "paused");
    var event =
        new OutboxMessage(
            source.eventId(),
            source.aggregateId(),
            source.ownerId(),
            source.occurredAt(),
            "ProjectStatusChanged.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(event)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var received = channel.basicGet("organization.project-status-changed.v1", true);
      assertThat(received).isNotNull();
      assertThat(received.getEnvelope().getRoutingKey()).isEqualTo("project.status-changed.v1");
      assertThat(received.getProps().getMessageId()).isEqualTo(event.eventId().toString());
      assertThat(received.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(received.getProps().getContentType()).isEqualTo("application/json");
      assertThat(received.getBody())
          .isEqualTo(event.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  void task_s17_publishesTaskRouteWithoutChangingProjectRoutes() throws Exception {
    var base = message();
    var payload = new java.util.HashMap<String, Object>(base.payload());
    payload.remove("name");
    payload.put("type", "TaskCreated.v1");
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("title", "Tarea");
    var task =
        new OutboxMessage(
            base.eventId(),
            base.aggregateId(),
            base.ownerId(),
            base.occurredAt(),
            "TaskCreated.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(task)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var delivered = channel.basicGet("organization.task-created.v1", true);
      assertThat(delivered).isNotNull();
      assertThat(delivered.getBody())
          .isEqualTo(task.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(delivered.getProps().getMessageId()).isEqualTo(task.eventId().toString());
      assertThat(delivered.getEnvelope().getRoutingKey()).isEqualTo("task.created.v1");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"host", "vhost"})
  void task_s17_doesNotFallBackToDefaultBrokerDestination(String changed) throws Exception {
    var publisher =
        new RabbitBrokerPublisher(
            changed.equals("host") ? "does-not-exist.invalid" : rabbit.getHost(),
            rabbit.getMappedPort(5672),
            "broker-test",
            "broker-test-secret",
            changed.equals("vhost") ? "/unavailable-vhost" : "/");
    assertThat(publisher.publish(message())).isEqualTo(DeliveryOutcome.BROKER_UNAVAILABLE);
  }

  @Test
  void subtask_s21_publishesOriginalNineFieldsToOwnQuorumRoute() throws Exception {
    var base = message();
    var payload = new java.util.HashMap<String, Object>(base.payload());
    payload.remove("name");
    payload.put("type", "SubtaskCreated.v1");
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("parentTaskId", UUID.randomUUID().toString());
    payload.put("title", "Tarea");
    var task =
        new OutboxMessage(
            base.eventId(),
            base.aggregateId(),
            base.ownerId(),
            base.occurredAt(),
            "SubtaskCreated.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(task)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var delivered = channel.basicGet("organization.subtask-created.v1", true);
      assertThat(delivered).isNotNull();
      assertThat(delivered.getBody())
          .isEqualTo(task.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(delivered.getProps().getMessageId()).isEqualTo(task.eventId().toString());
      assertThat(delivered.getEnvelope().getRoutingKey()).isEqualTo("subtask.created.v1");
    }
  }

  @Test
  void taskStatus_s20_publishesOriginalNineFieldsToOwnQuorumRoute() throws Exception {
    var base = message();
    var payload = new java.util.HashMap<String, Object>(base.payload());
    payload.remove("name");
    payload.put("type", "TaskStatusChanged.v1");
    payload.put("taskId", UUID.randomUUID().toString());
    payload.put("fromStatus", "pending");
    payload.put("toStatus", "completed");
    var task =
        new OutboxMessage(
            base.eventId(),
            base.aggregateId(),
            base.ownerId(),
            base.occurredAt(),
            "TaskStatusChanged.v1",
            1,
            json.writeValueAsString(payload),
            payload,
            0);
    assertThat(publisher().publish(task)).isEqualTo(DeliveryOutcome.ACCEPTED);
    try (var connection = factory().newConnection();
        var channel = connection.createChannel()) {
      var delivered = channel.basicGet("organization.task-status-changed.v1", true);
      assertThat(delivered).isNotNull();
      assertThat(delivered.getBody())
          .isEqualTo(task.json().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      assertThat(delivered.getProps().getDeliveryMode()).isEqualTo(2);
      assertThat(delivered.getProps().getMessageId()).isEqualTo(task.eventId().toString());
      assertThat(delivered.getEnvelope().getRoutingKey()).isEqualTo("task.status-changed.v1");
    }
  }
}
