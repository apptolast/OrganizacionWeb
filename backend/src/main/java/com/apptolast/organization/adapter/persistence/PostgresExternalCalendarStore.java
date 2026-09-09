package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ExternalCalendarStore;
import com.apptolast.organization.application.StoredSubscription;
import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.SyncSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Suscripción y su instantánea. La descarga ocurre fuera de la transacción; aquí solo se confirma,
 * y siempre condicionada a la versión leída antes de descargar, de modo que dos sincronizaciones
 * concurrentes nunca mezclen instantáneas.
 */
public final class PostgresExternalCalendarStore implements ExternalCalendarStore {
  private static final String COLUMNS =
      "id, label, url_ciphertext, url_host, url_tail, version, updated_at, last_attempt_at,"
          + " last_sync_at, last_status, last_error, snapshot_zone_id, imported, skipped_recurring,"
          + " skipped_cancelled, skipped_invalid, truncated";
  private static final String RESET =
      " last_attempt_at = NULL, last_sync_at = NULL, last_status = NULL, last_error = NULL,"
          + " snapshot_zone_id = NULL, imported = 0, skipped_recurring = 0, skipped_cancelled = 0,"
          + " skipped_invalid = 0, truncated = FALSE";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;

  public PostgresExternalCalendarStore(JdbcTemplate jdbc, TransactionTemplate transactions) {
    this.jdbc = jdbc;
    this.transactions = transactions;
  }

  @Override
  public Optional<StoredSubscription> find(String ownerId) {
    return reading(() -> read(ownerId));
  }

  @Override
  public StoredSubscription create(
      String ownerId, UUID id, ExternalCalendarInput input, byte[] urlCiphertext, Instant now) {
    return writing(
        () -> {
          jdbc.update(
              "INSERT INTO external_calendar_subscriptions (owner_id, id, label, url_ciphertext,"
                  + " url_host, url_tail, version, created_at, updated_at) VALUES (?,?,?,?,?,?,0,?,?)",
              ownerId,
              id,
              input.label(),
              urlCiphertext,
              input.urlHost(),
              input.urlTail(),
              at(now),
              at(now));
          return read(ownerId).orElseThrow();
        });
  }

  @Override
  public StoredSubscription relabel(String ownerId, String label, Instant now) {
    return writing(
        () -> {
          jdbc.update(
              "UPDATE external_calendar_subscriptions SET label = ?, version = version + 1,"
                  + " updated_at = ? WHERE owner_id = ?",
              label,
              at(now),
              ownerId);
          return read(ownerId).orElseThrow();
        });
  }

  @Override
  public StoredSubscription rebind(
      String ownerId, ExternalCalendarInput input, byte[] urlCiphertext, Instant now) {
    return writing(
        () -> {
          jdbc.update(
              "UPDATE external_calendar_subscriptions SET label = ?, url_ciphertext = ?,"
                  + " url_host = ?, url_tail = ?, version = version + 1, updated_at = ?,"
                  + RESET
                  + " WHERE owner_id = ?",
              input.label(),
              urlCiphertext,
              input.urlHost(),
              input.urlTail(),
              at(now),
              ownerId);
          jdbc.update("DELETE FROM external_calendar_events WHERE owner_id = ?", ownerId);
          return read(ownerId).orElseThrow();
        });
  }

  @Override
  public boolean delete(String ownerId) {
    return Boolean.TRUE.equals(
        writing(
            () ->
                jdbc.update("DELETE FROM external_calendar_subscriptions WHERE owner_id = ?", ownerId)
                    > 0));
  }

  @Override
  public Optional<StoredSubscription> commitSuccess(
      String ownerId,
      long expectedVersion,
      SyncSummary summary,
      List<ExternalEvent> events,
      Instant syncAt) {
    return writing(
        () -> {
          int updated =
              jdbc.update(
                  "UPDATE external_calendar_subscriptions SET version = version + 1, updated_at = ?,"
                      + " last_attempt_at = ?, last_sync_at = ?, last_status = 'OK',"
                      + " last_error = NULL, snapshot_zone_id = ?, imported = ?,"
                      + " skipped_recurring = ?, skipped_cancelled = ?, skipped_invalid = ?,"
                      + " truncated = ? WHERE owner_id = ? AND version = ?",
                  at(syncAt),
                  at(syncAt),
                  at(syncAt),
                  summary.snapshotZoneId(),
                  summary.imported(),
                  summary.skippedRecurring(),
                  summary.skippedCancelled(),
                  summary.skippedInvalid(),
                  summary.truncated(),
                  ownerId,
                  expectedVersion);
          if (updated == 0) return Optional.<StoredSubscription>empty();
          jdbc.update("DELETE FROM external_calendar_events WHERE owner_id = ?", ownerId);
          insert(ownerId, events);
          return read(ownerId);
        });
  }

  @Override
  public Optional<StoredSubscription> commitFailure(
      String ownerId, long expectedVersion, FeedError error, Instant attemptAt) {
    return writing(
        () -> {
          int updated =
              jdbc.update(
                  "UPDATE external_calendar_subscriptions SET version = version + 1, updated_at = ?,"
                      + " last_attempt_at = ?, last_status = 'FAILED', last_error = ?"
                      + " WHERE owner_id = ? AND version = ?",
                  at(attemptAt),
                  at(attemptAt),
                  error.name(),
                  ownerId,
                  expectedVersion);
          return updated == 0 ? Optional.<StoredSubscription>empty() : read(ownerId);
        });
  }

  @Override
  public List<ExternalEvent> events(String ownerId, Instant from, Instant to) {
    return reading(
        () ->
            jdbc.query(
                "SELECT uid, summary, start_at, end_at, all_day FROM external_calendar_events"
                    + " WHERE owner_id = ? AND start_at < ? AND end_at > ?"
                    + " ORDER BY start_at, uid COLLATE \"C\"",
                (rs, row) -> event(rs),
                ownerId,
                at(to),
                at(from)));
  }

  private void insert(String ownerId, List<ExternalEvent> events) {
    jdbc.batchUpdate(
        "INSERT INTO external_calendar_events (owner_id, uid, summary, start_at, end_at, all_day)"
            + " VALUES (?,?,?,?,?,?)",
        events.stream()
            .map(
                event ->
                    new Object[] {
                      ownerId,
                      event.uid(),
                      event.summary(),
                      at(event.startAt()),
                      at(event.endAt()),
                      event.allDay()
                    })
            .toList());
  }

  private Optional<StoredSubscription> read(String ownerId) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM external_calendar_subscriptions WHERE owner_id = ?",
            (rs, row) -> stored(rs),
            ownerId)
        .stream()
        .findFirst();
  }

  private static StoredSubscription stored(ResultSet rs) throws SQLException {
    var subscription =
        new ExternalCalendarSubscription(
            rs.getObject("id", UUID.class),
            rs.getString("label"),
            rs.getString("url_host"),
            rs.getString("url_tail"),
            instant(rs, "last_attempt_at"),
            instant(rs, "last_sync_at"),
            enumeration(rs.getString("last_status"), SyncStatus::valueOf),
            enumeration(rs.getString("last_error"), FeedError::valueOf),
            rs.getString("snapshot_zone_id"),
            rs.getInt("imported"),
            rs.getInt("skipped_recurring"),
            rs.getInt("skipped_cancelled"),
            rs.getInt("skipped_invalid"),
            rs.getBoolean("truncated"),
            instant(rs, "updated_at"));
    return new StoredSubscription(
        subscription, rs.getBytes("url_ciphertext"), rs.getLong("version"));
  }

  private static ExternalEvent event(ResultSet rs) throws SQLException {
    return new ExternalEvent(
        rs.getString("uid"),
        rs.getString("summary"),
        instant(rs, "start_at"),
        instant(rs, "end_at"),
        rs.getBoolean("all_day"));
  }

  private static <T> T enumeration(String value, java.util.function.Function<String, T> parse) {
    return value == null ? null : parse.apply(value);
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    var value = rs.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private static OffsetDateTime at(Instant instant) {
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private <T> T writing(java.util.function.Supplier<T> work) {
    return transactions.execute(status -> work.get());
  }

  private <T> T reading(java.util.function.Supplier<T> work) {
    var template = new TransactionTemplate(transactions.getTransactionManager());
    template.setReadOnly(true);
    template.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    return template.execute(status -> work.get());
  }
}
