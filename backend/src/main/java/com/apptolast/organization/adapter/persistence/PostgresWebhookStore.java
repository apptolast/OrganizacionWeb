package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.WebhookDeliveries;
import com.apptolast.organization.application.WebhookEndpoints;
import com.apptolast.organization.application.WebhookOperationException;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The owner's webhook endpoints and their delivery log, over plain JdbcTemplate. */
public final class PostgresWebhookStore implements WebhookEndpoints, WebhookDeliveries {
  private static final UUID NIL_EVENT = new UUID(0L, 0L);
  private static final int MAX_ENDPOINTS_PER_OWNER = 5;

  private final JdbcTemplate jdbc;
  private final TransactionTemplate writing;

  public PostgresWebhookStore(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
    this.jdbc = jdbc;
    this.writing = new TransactionTemplate(transactions);
  }

  @Override
  public void insert(String owner, WebhookEndpoint endpoint, byte[] secretCiphertext) {
    writing.executeWithoutResult(
        status -> {
          lockOwner(owner);
          if (countOf(owner) >= MAX_ENDPOINTS_PER_OWNER)
            throw new WebhookOperationException(WebhookOperationException.Code.LIMIT);
          jdbc.update(
              """
              INSERT INTO webhook_endpoints (
                id, owner_id, url, description, event_types, status,
                disabled_reason, disabled_at, secret_ciphertext,
                cursor_occurred_at, cursor_event_id, created_at, updated_at)
              VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
              """,
              endpoint.id(),
              owner,
              endpoint.url(),
              endpoint.description(),
              endpoint.eventTypes().toArray(String[]::new),
              endpoint.status(),
              endpoint.disabledReason(),
              stamp(endpoint.disabledAt()),
              secretCiphertext,
              stamp(endpoint.createdAt()),
              NIL_EVENT,
              stamp(endpoint.createdAt()),
              stamp(endpoint.updatedAt()));
        });
  }

  @Override
  public List<WebhookEndpoint> list(String owner) {
    return jdbc.query(
        "SELECT * FROM webhook_endpoints WHERE owner_id=? ORDER BY created_at DESC, id DESC",
        ENDPOINT,
        owner);
  }

  @Override
  public Optional<WebhookEndpoint> find(String owner, UUID id) {
    return jdbc
        .query("SELECT * FROM webhook_endpoints WHERE owner_id=? AND id=?", ENDPOINT, owner, id)
        .stream()
        .findFirst();
  }

  @Override
  public Optional<WebhookEndpoint> changeStatus(String owner, UUID id, String status, Instant now) {
    return writing.execute(
        transaction -> {
          var current = find(owner, id);
          if (current.isEmpty()) return Optional.empty();
          var changed = current.get().withStatus(status, now);
          return Optional.of(save(owner, changed));
        });
  }

  /** Persists an already-decided endpoint state; the domain owns the transition rules. */
  private WebhookEndpoint save(String owner, WebhookEndpoint endpoint) {
    jdbc.update(
        """
        UPDATE webhook_endpoints
           SET status=?, disabled_reason=?, disabled_at=?, updated_at=?
         WHERE owner_id=? AND id=?
        """,
        endpoint.status(),
        endpoint.disabledReason(),
        stamp(endpoint.disabledAt()),
        stamp(endpoint.updatedAt()),
        owner,
        endpoint.id());
    return endpoint;
  }

  @Override
  public boolean delete(String owner, UUID id) {
    return Boolean.TRUE.equals(
        writing.execute(
            transaction ->
                jdbc.update("DELETE FROM webhook_endpoints WHERE owner_id=? AND id=?", owner, id)
                    == 1));
  }

  @Override
  public List<WebhookDelivery> list(String owner, UUID endpointId) {
    return jdbc.query(
        """
        SELECT * FROM webhook_deliveries
         WHERE owner_id=? AND endpoint_id=?
         ORDER BY updated_at DESC, id DESC
        """,
        DELIVERY,
        owner,
        endpointId);
  }

  @Override
  public boolean hasPendingPing(String owner, UUID endpointId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            """
            SELECT EXISTS (SELECT 1 FROM webhook_deliveries
             WHERE owner_id=? AND endpoint_id=? AND status='pending' AND event_type=?)
            """,
            Boolean.class,
            owner,
            endpointId,
            WebhookDelivery.PING));
  }

  @Override
  public WebhookDelivery enqueuePing(
      String owner, UUID endpointId, WebhookDelivery delivery, String body) {
    writing.executeWithoutResult(transaction -> insertDelivery(owner, endpointId, delivery, body));
    return delivery;
  }

  private void insertDelivery(
      String owner, UUID endpointId, WebhookDelivery delivery, String body) {
    jdbc.update(
        """
        INSERT INTO webhook_deliveries (
          id, endpoint_id, owner_id, event_id, event_type, body, status, attempt,
          http_status, latency_ms, error_class, next_attempt_at, created_at, updated_at)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """,
        delivery.id(),
        endpointId,
        owner,
        delivery.eventId(),
        delivery.eventType(),
        body,
        delivery.status(),
        delivery.attempt(),
        delivery.httpStatus(),
        delivery.latencyMs(),
        delivery.errorClass(),
        stamp(delivery.nextAttemptAt()),
        stamp(delivery.createdAt()),
        stamp(delivery.updatedAt()));
  }

  @Override
  public Optional<WebhookDelivery> find(String owner, UUID endpointId, UUID deliveryId) {
    return jdbc
        .query(
            "SELECT * FROM webhook_deliveries WHERE owner_id=? AND endpoint_id=? AND id=?",
            DELIVERY,
            owner,
            endpointId,
            deliveryId)
        .stream()
        .findFirst();
  }

  @Override
  public WebhookDelivery requeue(String owner, UUID endpointId, WebhookDelivery delivery) {
    writing.executeWithoutResult(
        transaction ->
            jdbc.update(
                """
                UPDATE webhook_deliveries
                   SET status=?, attempt=?, http_status=NULL, latency_ms=NULL,
                       error_class=NULL, next_attempt_at=?, leased_until=NULL, updated_at=?
                 WHERE owner_id=? AND endpoint_id=? AND id=?
                """,
                delivery.status(),
                delivery.attempt(),
                stamp(delivery.nextAttemptAt()),
                stamp(delivery.updatedAt()),
                owner,
                endpointId,
                delivery.id()));
    return delivery;
  }

  private void lockOwner(String owner) {
    jdbc.queryForObject(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
        Object.class,
        "webhook-owner:" + owner);
  }

  private int countOf(String owner) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM webhook_endpoints WHERE owner_id=?", Integer.class, owner);
  }

  private static Timestamp stamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    var value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static Integer number(ResultSet rs, String column) throws SQLException {
    var value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private static final RowMapper<WebhookEndpoint> ENDPOINT =
      (rs, index) ->
          new WebhookEndpoint(
              rs.getObject("id", UUID.class),
              rs.getString("url"),
              rs.getString("description"),
              List.of((String[]) rs.getArray("event_types").getArray()),
              rs.getString("status"),
              rs.getString("disabled_reason"),
              instant(rs, "disabled_at"),
              instant(rs, "created_at"),
              instant(rs, "updated_at"));

  private static final RowMapper<WebhookDelivery> DELIVERY =
      (rs, index) ->
          new WebhookDelivery(
              rs.getObject("id", UUID.class),
              rs.getObject("event_id", UUID.class),
              rs.getString("event_type"),
              rs.getString("status"),
              rs.getInt("attempt"),
              number(rs, "http_status"),
              number(rs, "latency_ms"),
              rs.getString("error_class"),
              instant(rs, "next_attempt_at"),
              instant(rs, "created_at"),
              instant(rs, "updated_at"));
}
