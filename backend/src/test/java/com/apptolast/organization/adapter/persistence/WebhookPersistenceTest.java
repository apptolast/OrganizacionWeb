package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.ManageWebhook;
import com.apptolast.organization.application.WebhookOperationException;
import com.apptolast.organization.application.WebhookSecrets;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class WebhookPersistenceTest {
  private static final Instant NOW = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final UUID NIL = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private static PostgresWebhookStore store() {
    return new PostgresWebhookStore(Database.JDBC, Database.TRANSACTIONS);
  }

  private static WebhookEndpoint endpoint(Instant createdAt) {
    return new WebhookEndpoint(
        UUID.randomUUID(),
        "https://example.com/hooks",
        "Mi hook",
        List.of("TaskCreated.v1", "TaskStatusChanged.v1"),
        "active",
        null,
        null,
        createdAt,
        createdAt);
  }

  /** Los secretos están disponibles: lo que se juzga aquí es el aislamiento, no la clave. */
  private static final WebhookSecrets KEYED =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return true;
        }

        @Override
        public byte[] encrypt(String ownerId, UUID endpointId, String secret) {
          return cipher(secret);
        }

        @Override
        public String decrypt(String ownerId, UUID endpointId, byte[] ciphertext) {
          return new String(ciphertext, StandardCharsets.UTF_8).replace("cipher:", "");
        }
      };

  private static byte[] cipher(String secret) {
    return ("cipher:" + secret).getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void s1_insertingSealsTheCursorAtTheCreationInstantAndTheNilEventId() {
    var owner = "cursor-" + UUID.randomUUID();
    var endpoint = endpoint(NOW);
    store().insert(owner, endpoint, cipher("whsec_x"));

    var cursor =
        Database.JDBC.queryForMap(
            "SELECT cursor_occurred_at, cursor_event_id FROM webhook_endpoints WHERE id=?",
            endpoint.id());
    assertEquals(NOW, ((java.sql.Timestamp) cursor.get("cursor_occurred_at")).toInstant());
    assertEquals(NIL, cursor.get("cursor_event_id"));
  }

  @Test
  void s8_theStoredSecretIsOnlyTheCiphertextAndNeverTheClearSecret() {
    var owner = "secret-" + UUID.randomUUID();
    var endpoint = endpoint(NOW);
    store().insert(owner, endpoint, cipher("whsec_clear"));

    var stored =
        Database.JDBC.queryForObject(
            "SELECT secret_ciphertext FROM webhook_endpoints WHERE id=?",
            byte[].class,
            endpoint.id());
    assertArrayEquals(cipher("whsec_clear"), stored);
    var columns =
        Database.JDBC.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name='webhook_endpoints'",
            String.class);
    assertFalse(columns.contains("secret"));
  }

  @Test
  void s6_theQuotaOfFiveCountsDisabledEndpointsToo() {
    var owner = "quota-" + UUID.randomUUID();
    var store = store();
    for (var index = 0; index < 5; index++) {
      var endpoint = endpoint(NOW.plusSeconds(index));
      store.insert(owner, endpoint, cipher("whsec_x"));
      if (index < 2) store.changeStatus(owner, endpoint.id(), "disabled", NOW);
    }
    assertEquals(
        WebhookOperationException.Code.LIMIT,
        assertThrows(
                WebhookOperationException.class,
                () -> store.insert(owner, endpoint(NOW), cipher("whsec_x")))
            .code());
    assertEquals(5, count(owner));
  }

  @Test
  void s7_concurrentCreationsForTheLastSeatProduceASingleInsert() throws Exception {
    var owner = "race-" + UUID.randomUUID();
    var store = store();
    for (var index = 0; index < 4; index++)
      store.insert(owner, endpoint(NOW.plusSeconds(index)), cipher("whsec_x"));

    Callable<Boolean> attempt =
        () -> {
          try {
            store.insert(owner, endpoint(NOW.plusSeconds(9)), cipher("whsec_x"));
            return true;
          } catch (WebhookOperationException error) {
            assertEquals(WebhookOperationException.Code.LIMIT, error.code());
            return false;
          }
        };
    try (var pool = Executors.newFixedThreadPool(2)) {
      var results = pool.invokeAll(List.of(attempt, attempt));
      var accepted = 0;
      for (var result : results) if (result.get()) accepted++;
      assertEquals(1, accepted);
    }
    assertEquals(5, count(owner));
  }

  @Test
  void s10_listingOrdersByCreatedAtThenIdDescendingAndOnlyForTheOwner() {
    var owner = "list-" + UUID.randomUUID();
    var stranger = "other-" + UUID.randomUUID();
    var store = store();
    var older = endpoint(NOW.minusSeconds(60));
    var tieLow =
        new WebhookEndpoint(
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "https://example.com/a",
            "",
            List.of("TaskCreated.v1"),
            "active",
            null,
            null,
            NOW,
            NOW);
    var tieHigh =
        new WebhookEndpoint(
            UUID.fromString("99999999-9999-4999-8999-999999999999"),
            "https://example.com/b",
            "",
            List.of("TaskCreated.v1"),
            "active",
            null,
            null,
            NOW,
            NOW);
    store.insert(owner, older, cipher("whsec_x"));
    store.insert(owner, tieLow, cipher("whsec_x"));
    store.insert(owner, tieHigh, cipher("whsec_x"));
    store.insert(stranger, endpoint(NOW), cipher("whsec_x"));

    assertEquals(
        List.of(tieHigh.id(), tieLow.id(), older.id()),
        store.list(owner).stream().map(WebhookEndpoint::id).toList());
    assertTrue(store.list("nobody-" + UUID.randomUUID()).isEmpty());
  }

  @Test
  void s11_aForeignEndpointIsInvisibleToEveryRead() {
    var owner = "mine-" + UUID.randomUUID();
    var stranger = "yours-" + UUID.randomUUID();
    var store = store();
    var foreign = endpoint(NOW);
    store.insert(stranger, foreign, cipher("whsec_x"));

    assertTrue(store.find(owner, foreign.id()).isEmpty());
    assertTrue(store.changeStatus(owner, foreign.id(), "disabled", NOW).isEmpty());
    assertFalse(store.delete(owner, foreign.id()));
    assertEquals("active", store.find(stranger, foreign.id()).orElseThrow().status());
  }

  @Test
  void s13_deletingRemovesItsDeliveriesAndLeavesTheOthersIntact() {
    var owner = "delete-" + UUID.randomUUID();
    var store = store();
    var doomed = endpoint(NOW);
    var kept = endpoint(NOW);
    store.insert(owner, doomed, cipher("whsec_x"));
    store.insert(owner, kept, cipher("whsec_x"));
    store.enqueuePing(owner, doomed.id(), WebhookDelivery.ping(UUID.randomUUID(), NOW), "{}");
    var survivor = WebhookDelivery.ping(UUID.randomUUID(), NOW);
    store.enqueuePing(owner, kept.id(), survivor, "{}");

    assertTrue(store.delete(owner, doomed.id()));
    assertEquals(
        0,
        Database.JDBC.queryForObject(
            "SELECT count(*) FROM webhook_deliveries WHERE endpoint_id=?",
            Integer.class,
            doomed.id()));
    assertEquals(
        List.of(survivor.id()),
        store.list(owner, kept.id()).stream().map(WebhookDelivery::id).toList());
    assertFalse(store.delete(owner, doomed.id()));
  }

  /**
   * Última fila de @s30: una entrega terminal que SÍ existe, pero pertenece a otro webhook del
   * mismo propietario. El aislamiento vive únicamente en el predicado {@code endpoint_id} de las
   * consultas, así que sólo se puede ejercer contra la base real; con el doble de aplicación, que
   * ignora el endpointId, esta fila es inconstruible.
   */
  @Test
  void s30_aTerminalDeliveryOfAnotherWebhookOfTheSameOwnerIsNeitherFoundNorRedelivered() {
    var owner = "isolation-" + UUID.randomUUID();
    var store = store();
    var asked = endpoint(NOW);
    var owning = endpoint(NOW);
    store.insert(owner, asked, cipher("whsec_x"));
    store.insert(owner, owning, cipher("whsec_y"));
    var delivery = WebhookDelivery.ping(UUID.randomUUID(), NOW);
    store.enqueuePing(owner, owning.id(), delivery, "{}");
    Database.JDBC.update(
        "UPDATE webhook_deliveries SET status='succeeded', attempt=1, http_status=200,"
            + " latency_ms=12, next_attempt_at=NULL WHERE id=?",
        delivery.id());

    assertTrue(store.find(owner, asked.id(), delivery.id()).isEmpty());
    assertTrue(store.list(owner, asked.id()).isEmpty());
    assertEquals(
        List.of(delivery.id()),
        store.list(owner, owning.id()).stream().map(WebhookDelivery::id).toList());

    var manage = new ManageWebhook(store, store, KEYED, Clock.fixed(NOW, ZoneOffset.UTC));
    var refused =
        assertThrows(
            WebhookOperationException.class,
            () -> manage.redeliver(owner, asked.id(), delivery.id()));

    assertEquals(WebhookOperationException.Code.NOT_FOUND, refused.code());
    assertEquals("succeeded", store.find(owner, owning.id(), delivery.id()).orElseThrow().status());
  }

  private static int count(String owner) {
    return Database.JDBC.queryForObject(
        "SELECT count(*) FROM webhook_endpoints WHERE owner_id=?", Integer.class, owner);
  }

  // One PostgreSQL process and migration per JVM, independent of test-instance lifecycle.
  static final class Database {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17.9-alpine");
    static final JdbcTemplate JDBC;
    static final DataSourceTransactionManager TRANSACTIONS;

    static {
      PG.start();
      var source = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
      Flyway.configure().dataSource(source).load().migrate();
      JDBC = new JdbcTemplate(source);
      TRANSACTIONS = new DataSourceTransactionManager(source);
    }
  }
}
