package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ClaimedDelivery;
import com.apptolast.organization.application.WebhookWork;
import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Claims one due delivery under a lease and settles it afterwards.
 *
 * <p>Amendment B1: claiming and recording are two short transactions with the HTTP exchange in
 * between, so no connection is ever held open inside a transaction. A worker that dies mid-attempt
 * simply lets its lease lapse and the delivery is retried.
 */
public final class PostgresWebhookWork implements WebhookWork {
  /** Long enough to outlive the ten-second exchange, short enough to recover quickly. */
  private static final Duration LEASE = Duration.ofMinutes(5);

  private static final int KEPT_TERMINAL_DELIVERIES = 50;

  /** Opens the stored secret of an endpoint; the owner and the id are the associated data. */
  @FunctionalInterface
  public interface SecretOpener {
    String open(String ownerId, UUID endpointId, byte[] ciphertext);
  }

  private final JdbcTemplate jdbc;
  private final TransactionTemplate writing;
  private final SecretOpener secrets;

  public PostgresWebhookWork(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, SecretOpener secrets) {
    this.jdbc = jdbc;
    this.writing = new TransactionTemplate(transactions);
    this.secrets = secrets;
  }

  @Override
  public Optional<ClaimedDelivery> claimNext(Instant now) {
    return Optional.ofNullable(
        writing.execute(
            transaction -> {
              var claimed =
                  jdbc.query(
                      """
                      SELECT d.*, e.owner_id AS endpoint_owner, e.url, e.description,
                             e.event_types, e.status AS endpoint_status, e.disabled_reason,
                             e.disabled_at, e.created_at AS endpoint_created_at,
                             e.updated_at AS endpoint_updated_at, e.secret_ciphertext
                        FROM webhook_deliveries d
                        JOIN webhook_endpoints e ON e.id = d.endpoint_id
                       WHERE d.status = 'pending'
                         AND d.next_attempt_at <= ?
                         AND e.status = 'active'
                         AND (d.leased_until IS NULL OR d.leased_until <= ?)
                       ORDER BY d.next_attempt_at, d.id
                       FOR UPDATE OF d SKIP LOCKED
                       LIMIT 1
                      """,
                      this::claim,
                      stamp(now),
                      stamp(now));
              if (claimed.isEmpty()) return null;
              var row = claimed.getFirst();
              jdbc.update(
                  "UPDATE webhook_deliveries SET leased_until=? WHERE id=?",
                  stamp(now.plus(LEASE)),
                  row.delivery().id());
              return handOver(row);
            }));
  }

  /**
   * Opens the stored secret <b>after</b> the lease, never inside the {@code RowMapper}.
   *
   * <p>A row sealed with an already rotated key cannot be opened, and project-spec declares that
   * case expected. Opening it while mapping meant the whole claim was rolled back before the lease
   * was written, so the same row headed the {@code next_attempt_at, d.id} order again on the next
   * tick and no delivery of any owner ever left again. Now the row is already leased and its secret
   * arrives as absent, which the worker settles as one failed attempt with an audited code.
   *
   * <p>The failure itself is deliberately dropped: it carries a message about key material and this
   * class must never let it reach a log.
   */
  private ClaimedDelivery handOver(LeasedRow row) {
    String secret;
    try {
      secret = secrets.open(row.owner(), row.endpoint().id(), row.sealedSecret());
    } catch (RuntimeException unreadable) {
      secret = null;
    }
    return new ClaimedDelivery(row.endpoint(), row.owner(), row.delivery(), row.body(), secret);
  }

  @Override
  public void record(ClaimedDelivery claimed, WebhookDelivery result, WebhookEndpoint endpoint) {
    writing.executeWithoutResult(
        transaction -> {
          jdbc.update(
              """
              UPDATE webhook_deliveries
                 SET status=?, attempt=?, http_status=?, latency_ms=?, error_class=?,
                     next_attempt_at=?, leased_until=NULL, updated_at=?
               WHERE id=?
              """,
              result.status(),
              result.attempt(),
              result.httpStatus(),
              result.latencyMs(),
              result.errorClass(),
              stamp(result.nextAttemptAt()),
              stamp(result.updatedAt()),
              result.id());
          if (endpoint != null) disable(claimed.ownerId(), endpoint);
          prune(claimed.endpoint().id());
        });
  }

  private void disable(String owner, WebhookEndpoint endpoint) {
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
  }

  /** Keeps the fifty most recently updated terminals; a pending delivery is never pruned. */
  private void prune(UUID endpointId) {
    jdbc.update(
        """
        DELETE FROM webhook_deliveries
         WHERE endpoint_id=? AND status <> 'pending' AND id NOT IN (
           SELECT id FROM webhook_deliveries
            WHERE endpoint_id=? AND status <> 'pending'
            ORDER BY updated_at DESC, id DESC LIMIT ?)
        """,
        endpointId,
        endpointId,
        KEPT_TERMINAL_DELIVERIES);
  }

  /** One claimed row with its secret still sealed: nothing here can fail on bad key material. */
  private record LeasedRow(
      WebhookEndpoint endpoint,
      String owner,
      WebhookDelivery delivery,
      String body,
      byte[] sealedSecret) {}

  private LeasedRow claim(ResultSet rs, int index) throws SQLException {
    var endpoint =
        new WebhookEndpoint(
            rs.getObject("endpoint_id", UUID.class),
            rs.getString("url"),
            rs.getString("description"),
            List.of((String[]) rs.getArray("event_types").getArray()),
            rs.getString("endpoint_status"),
            rs.getString("disabled_reason"),
            instant(rs, "disabled_at"),
            instant(rs, "endpoint_created_at"),
            instant(rs, "endpoint_updated_at"));
    var delivery =
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
    return new LeasedRow(
        endpoint,
        rs.getString("endpoint_owner"),
        delivery,
        rs.getString("body"),
        rs.getBytes("secret_ciphertext"));
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
}
