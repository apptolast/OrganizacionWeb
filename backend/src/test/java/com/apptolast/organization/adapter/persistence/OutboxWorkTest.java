package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class OutboxWorkTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");
  static JdbcTemplate jdbc;
  static TransactionTemplate transaction;
  static ObjectMapper json;
  final List<PublicationAttempt> auditEvents = new ArrayList<>();
  final List<String> errors = new ArrayList<>();
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
  static void database() {
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
  }

  @BeforeEach
  void clear() {
    jdbc.execute("TRUNCATE outbox_events, projects CASCADE");
  }

  ProjectCreated seed() {
    Project project =
        Project.create(UUID.randomUUID(), "persona-a", "Idea", "Private description", NOW);
    ProjectCreated event =
        new ProjectCreated(
            UUID.randomUUID(),
            project.id(),
            project.ownerId(),
            NOW,
            1,
            project.name(),
            "ProjectCreated.v1");
    new PostgresProjectCommit(jdbc, transaction, json).save(project, event);
    return event;
  }

  PublishOutbox publisher(BrokerPublisher broker) {
    return new PublishOutbox(
        new PostgresOutboxWork(jdbc, transaction, json),
        broker,
        audit,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void s5_failedPublishPersistsRetryAndExcludesSameCycle() {
    seed();
    var before = jdbc.queryForMap("SELECT * FROM outbox_events");
    List<OutboxMessage> sent = new ArrayList<>();
    publisher(
            message -> {
              sent.add(message);
              return DeliveryOutcome.BROKER_UNAVAILABLE;
            })
        .runCycle();
    var after = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(sent).hasSize(1);
    assertThat(after.get("status")).isEqualTo("pending");
    assertThat(after.get("attempts")).isEqualTo(1L);
    assertThat(after.get("published_at")).isNull();
    assertThat(after.get("payload")).isEqualTo(before.get("payload"));
    assertThat(after.get("last_error_code")).isEqualTo("BROKER_UNAVAILABLE");
    assertThat(((java.sql.Timestamp) after.get("next_attempt_at")).toInstant())
        .isEqualTo(NOW.plusSeconds(1));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"9,0", "10,1"})
  void s8_onlyClaimsDueEventsIncludingExactBoundary(int second, int expected) {
    seed();
    jdbc.update(
        "UPDATE outbox_events SET next_attempt_at=?", java.sql.Timestamp.from(NOW.plusSeconds(10)));
    List<OutboxMessage> sent = new ArrayList<>();
    new PublishOutbox(
            new PostgresOutboxWork(jdbc, transaction, json),
            message -> {
              sent.add(message);
              return DeliveryOutcome.ACCEPTED;
            },
            audit,
            Clock.fixed(NOW.plusSeconds(second), ZoneOffset.UTC))
        .runCycle();
    assertThat(sent).hasSize(expected);
  }

  @Test
  void s17_ordersAvailableEventsAndLeavesTwentyFirstPending() {
    for (int index = 0; index < 21; index++) {
      ProjectCreated event = seed();
      Instant occurred = NOW.minusSeconds(index % 2);
      jdbc.update(
          "UPDATE outbox_events SET occurred_at=?, payload=jsonb_set(payload,'{occurredAt}',to_jsonb(?::text)) WHERE event_id=?",
          java.sql.Timestamp.from(occurred),
          occurred.toString(),
          event.eventId());
    }
    List<UUID> expected =
        jdbc.queryForList(
            "SELECT event_id FROM outbox_events ORDER BY occurred_at,event_id LIMIT 20",
            UUID.class);
    List<UUID> sent = new ArrayList<>();
    publisher(
            message -> {
              sent.add(message.eventId());
              return DeliveryOutcome.ACCEPTED;
            })
        .runCycle();
    assertThat(sent).containsExactlyElementsOf(expected);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE status='pending'", Long.class))
        .isEqualTo(1L);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "[]", "\"text\"", "42"})
  void s15_nonObjectPayloadIsBlockedWithoutStoppingValidWork(String raw) {
    ProjectCreated invalid = seed();
    jdbc.update(
        "UPDATE outbox_events SET payload=?::jsonb,attempts=3 WHERE event_id=?",
        raw,
        invalid.eventId());
    ProjectCreated valid = seed();
    List<UUID> sent = new ArrayList<>();
    var worker =
        publisher(
            message -> {
              sent.add(message.eventId());
              return DeliveryOutcome.ACCEPTED;
            });
    worker.runCycle();
    worker.runCycle();
    assertThat(sent).containsExactly(valid.eventId());
    var record =
        jdbc.queryForMap("SELECT * FROM outbox_events WHERE event_id=?", invalid.eventId());
    assertThat(record.get("status")).isEqualTo("blocked");
    assertThat(record.get("last_error_code")).isEqualTo("INVALID_EVENT");
    assertThat(record.get("attempts")).isEqualTo(3L);
    assertThat(record.get("payload").toString()).isEqualTo(raw);
    assertThat(record.get("published_at")).isNull();
  }

  @Test
  void s2_uncommittedCreationIsInvisibleToPublisher() {
    transaction.executeWithoutResult(
        status -> {
          seed();
          try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            executor
                .submit(
                    () ->
                        publisher(
                                message -> {
                                  throw new AssertionError("Uncommitted event sent");
                                })
                            .runCycle())
                .get(5, java.util.concurrent.TimeUnit.SECONDS);
          } catch (Exception error) {
            throw new RuntimeException(error);
          }
          assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
              .isEqualTo(1L);
          status.setRollbackOnly();
        });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s12_resultWriteFailureRollsBackWithoutPublishedAudit() {
    seed();
    var before = jdbc.queryForMap("SELECT * FROM outbox_events");
    jdbc.execute(
        "CREATE FUNCTION test_reject_result() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private database failure'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER test_failure BEFORE UPDATE ON outbox_events FOR EACH ROW EXECUTE FUNCTION test_reject_result()");
    List<OutboxMessage> accepted = new ArrayList<>();
    try {
      publisher(
              message -> {
                accepted.add(message);
                return DeliveryOutcome.ACCEPTED;
              })
          .runCycle();
      assertThat(accepted).hasSize(1);
      assertThat(jdbc.queryForMap("SELECT * FROM outbox_events")).isEqualTo(before);
      assertThat(auditEvents).isEmpty();
      assertThat(errors).containsExactly("STORAGE_UNAVAILABLE");
    } finally {
      jdbc.execute("DROP TRIGGER test_failure ON outbox_events");
      jdbc.execute("DROP FUNCTION test_reject_result()");
    }
  }

  @Test
  void s4_s13_otherReplicaSkipsClaimedRowWhileConfirmationPending() throws Exception {
    seed();
    seed();
    var claimed = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var firstId = new java.util.concurrent.atomic.AtomicReference<UUID>();
    try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var first =
          executor.submit(
              () ->
                  new PostgresOutboxWork(jdbc, transaction, json)
                      .processNext(
                          NOW,
                          Set.of(),
                          message -> {
                            firstId.set(message.eventId());
                            claimed.countDown();
                            try {
                              if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                                throw new AssertionError("Confirmation barrier timed out");
                            } catch (InterruptedException error) {
                              Thread.currentThread().interrupt();
                              throw new RuntimeException(error);
                            }
                            return new PublicationAttempt(
                                message.eventId(), "published", 1, NOW, null, null);
                          }));
      try {
        assertThat(claimed.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var pending =
            jdbc.queryForMap("SELECT * FROM outbox_events WHERE event_id=?", firstId.get());
        assertThat(pending.get("status")).isEqualTo("pending");
        assertThat(pending.get("published_at")).isNull();
        List<UUID> secondSent = new ArrayList<>();
        publisher(
                message -> {
                  secondSent.add(message.eventId());
                  return DeliveryOutcome.ACCEPTED;
                })
            .runCycle();
        assertThat(secondSent).hasSize(1).doesNotContain(firstId.get());
        assertThat(jdbc.queryForMap("SELECT * FROM outbox_events WHERE event_id=?", firstId.get()))
            .isEqualTo(pending);
      } finally {
        release.countDown();
      }
      assertThat(first.get(5, java.util.concurrent.TimeUnit.SECONDS)).isPresent();
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE status='published'", Long.class))
        .isEqualTo(2L);
  }

  @Test
  void s1_migrationAndConfirmedPublicationPreserveOriginalRecord() {
    ProjectCreated original = seed();
    var before = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(before.get("attempts")).isEqualTo(0L);
    List<OutboxMessage> sent = new ArrayList<>();
    publisher(
            message -> {
              sent.add(message);
              return DeliveryOutcome.ACCEPTED;
            })
        .runCycle();
    var after = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(after.get("event_id")).isEqualTo(original.eventId());
    assertThat(after.get("payload")).isEqualTo(before.get("payload"));
    assertThat(after.get("status")).isEqualTo("published");
    assertThat(after.get("attempts")).isEqualTo(1L);
    assertThat(((java.sql.Timestamp) after.get("published_at")).toInstant()).isEqualTo(NOW);
    assertThat(after.get("last_error_code")).isNull();
    assertThat(sent).hasSize(1);
    assertThat(sent.getFirst().eventId()).isEqualTo(original.eventId());
    assertThat(sent.getFirst().json()).isEqualTo(before.get("payload").toString());
    assertThat(auditEvents).hasSize(1);
  }

  @Test
  void s3_emptyOutboxDoesNotSendOrCreateRecords() {
    publisher(
            event -> {
              throw new AssertionError("Empty outbox sent");
            })
        .runCycle();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    assertThat(auditEvents).isEmpty();
  }

  @Test
  void s10_publishedRecordAndTimestampRemainUnchangedOnLaterCycle() {
    seed();
    publisher(event -> DeliveryOutcome.ACCEPTED).runCycle();
    var before = jdbc.queryForMap("SELECT * FROM outbox_events");
    publisher(
            event -> {
              throw new AssertionError("Published event resent");
            })
        .runCycle();
    assertThat(jdbc.queryForMap("SELECT * FROM outbox_events")).isEqualTo(before);
    assertThat(auditEvents).hasSize(1);
  }

  @Test
  void s18_unavailablePostgresNeverInvokesBroker() throws Exception {
    int port;
    try (var socket = new java.net.ServerSocket(0)) {
      port = socket.getLocalPort();
    }
    var unavailable =
        new DriverManagerDataSource(
            "jdbc:postgresql://127.0.0.1:" + port + "/unavailable?connectTimeout=1",
            "test",
            "test");
    var work =
        new PostgresOutboxWork(
            new JdbcTemplate(unavailable),
            new TransactionTemplate(new DataSourceTransactionManager(unavailable)),
            json);
    new PublishOutbox(
            work,
            event -> {
              throw new AssertionError("DB unavailable sent");
            },
            audit,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .runCycle();
    assertThat(errors).containsExactly("STORAGE_UNAVAILABLE");
    assertThat(auditEvents).isEmpty();
  }
}
