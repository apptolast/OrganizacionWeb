package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.OutboxCandidate;
import com.apptolast.organization.application.ReadyEndpoint;
import com.apptolast.organization.application.WebhookOutbox;
import com.apptolast.organization.domain.OutboxMessage;
import com.apptolast.organization.domain.WebhookCursor;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reads the outbox on behalf of the webhooks and writes only on the webhook side. No statement here
 * ever updates outbox_events: its status, attempts, published_at and payload are untouchable.
 */
public final class PostgresWebhookOutbox implements WebhookOutbox {
  private static final String PING = WebhookDelivery.PING;

  private final JdbcTemplate jdbc;
  private final TransactionTemplate writing;
  private final ObjectMapper json;

  public PostgresWebhookOutbox(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, ObjectMapper json) {
    this.jdbc = jdbc;
    this.writing = new TransactionTemplate(transactions);
    this.json = json;
  }

  /** Active endpoints whose only pending deliveries, if any, are pings. */
  @Override
  public List<ReadyEndpoint> readyEndpoints() {
    return jdbc.query(
        """
        SELECT e.* FROM webhook_endpoints e
         WHERE e.status = 'active'
           AND NOT EXISTS (
             SELECT 1 FROM webhook_deliveries d
              WHERE d.endpoint_id = e.id AND d.status = 'pending' AND d.event_type <> ?)
         ORDER BY e.created_at, e.id
        """,
        PostgresWebhookOutbox::ready,
        PING);
  }

  @Override
  public List<OutboxCandidate> after(String ownerId, WebhookCursor cursor, Instant horizon) {
    return jdbc.query(
        """
        SELECT event_id, event_type, occurred_at, status, schema_version, payload::text AS payload
          FROM outbox_events
         WHERE owner_id = ?
           AND occurred_at <= ?
           AND (occurred_at, event_id) > (?, ?)
         ORDER BY occurred_at, event_id
        """,
        this::candidate,
        ownerId,
        stamp(horizon),
        stamp(cursor.occurredAt()),
        cursor.eventId());
  }

  @Override
  public void enqueue(UUID endpointId, OutboxCandidate candidate, UUID deliveryId, Instant now) {
    writing.executeWithoutResult(
        transaction -> {
          jdbc.update(
              """
              INSERT INTO webhook_deliveries (
                id, endpoint_id, owner_id, event_id, event_type, body, status, attempt,
                next_attempt_at, created_at, updated_at)
              SELECT ?, e.id, e.owner_id, ?, ?, ?, 'pending', 0, ?, ?, ?
                FROM webhook_endpoints e WHERE e.id = ?
              """,
              deliveryId,
              candidate.eventId(),
              candidate.eventType(),
              candidate.payload(),
              stamp(now),
              stamp(now),
              stamp(now),
              endpointId);
          advance(endpointId, new WebhookCursor(candidate.occurredAt(), candidate.eventId()));
        });
  }

  @Override
  public void skip(UUID endpointId, WebhookCursor cursor, String auditCode) {
    writing.executeWithoutResult(transaction -> advance(endpointId, cursor));
  }

  private void advance(UUID endpointId, WebhookCursor cursor) {
    jdbc.update(
        "UPDATE webhook_endpoints SET cursor_occurred_at=?, cursor_event_id=? WHERE id=?",
        stamp(cursor.occurredAt()),
        cursor.eventId(),
        endpointId);
  }

  private OutboxCandidate candidate(ResultSet rs, int index) throws SQLException {
    var payload = rs.getString("payload");
    var eventId = rs.getObject("event_id", UUID.class);
    var type = rs.getString("event_type");
    var version = rs.getInt("schema_version");
    var occurredAt = rs.getTimestamp("occurred_at").toInstant();
    return new OutboxCandidate(
        eventId,
        type,
        occurredAt,
        rs.getString("status"),
        version,
        payload,
        deliverable(eventId, type, version, occurredAt, payload));
  }

  /** Reuses the publisher's own contract check, so both channels agree on what a valid event is. */
  private boolean deliverable(
      UUID eventId, String type, int version, Instant occurredAt, String payload) {
    try {
      var body = json.readValue(payload, new TypeReference<Map<String, Object>>() {});
      var message =
          new OutboxMessage(
              eventId,
              UUID.fromString(String.valueOf(body.get("aggregateId"))),
              String.valueOf(body.get("ownerId")),
              occurredAt,
              type,
              version,
              payload,
              body,
              0L);
      return message.validationCode() == null;
    } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException broken) {
      return false;
    }
  }

  private static ReadyEndpoint ready(ResultSet rs, int index) throws SQLException {
    var endpoint =
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
    return new ReadyEndpoint(
        endpoint,
        rs.getString("owner_id"),
        new WebhookCursor(
            instant(rs, "cursor_occurred_at"), rs.getObject("cursor_event_id", UUID.class)));
  }

  private static Timestamp stamp(Instant instant) {
    return Timestamp.from(instant);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    var value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
