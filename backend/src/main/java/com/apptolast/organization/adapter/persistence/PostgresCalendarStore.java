package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.CalendarFeedTokens;
import com.apptolast.organization.application.CalendarQueries;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.CalendarEntry;
import com.apptolast.organization.domain.CalendarSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The single owner row that holds a feed fingerprint, and the read of the blocks it publishes. Both
 * live in one adapter because the public feed resolves the token and reads the calendar inside the
 * same read-only repeatable-read snapshot.
 */
public final class PostgresCalendarStore implements CalendarFeedTokens, CalendarQueries {
  /** One more than the contract ceiling, so the use case can refuse before serialising. */
  private static final int CEILING_PROBE = 2001;

  private static final String CURRENT_BLOCKS =
      "SELECT b.id AS block_id,b.project_id,b.task_id,t.title,b.objective,"
          + "coalesce(v.start_at,b.start_at) AS start_at,coalesce(v.end_at,b.end_at) AS end_at,"
          + "coalesce(v.version,1) AS version,coalesce(v.updated_at,b.created_at) AS stamped_at"
          + " FROM planned_blocks b LEFT JOIN block_projections v ON v.block_id=b.id"
          + " JOIN projects owner_project ON owner_project.id=b.project_id"
          + " JOIN tasks t ON t.project_id=b.project_id AND t.id=b.task_id"
          + " WHERE owner_project.owner_id=? AND coalesce(v.status,'planned')='planned'"
          + " AND coalesce(v.end_at,b.end_at)>? AND coalesce(v.start_at,b.start_at)<?"
          + " ORDER BY coalesce(v.start_at,b.start_at),b.id LIMIT "
          + CEILING_PROBE;

  private final JdbcTemplate jdbc;
  private final TransactionTemplate snapshot;

  public PostgresCalendarStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
    this.jdbc = jdbc;
    this.snapshot = new TransactionTemplate(manager);
    this.snapshot.setReadOnly(true);
    this.snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  @Override
  public Optional<Instant> createdAt(String owner) {
    return guarded(
        () ->
            jdbc
                .query(
                    "SELECT created_at FROM calendar_feed_tokens WHERE owner_id=?",
                    (row, index) -> row.getObject("created_at", OffsetDateTime.class).toInstant(),
                    owner)
                .stream()
                .findFirst());
  }

  @Override
  public void replace(String owner, byte[] fingerprint, Instant createdAt) {
    guarded(
        () ->
            jdbc.update(
                "INSERT INTO calendar_feed_tokens(owner_id,token_hash,created_at) VALUES (?,?,?)"
                    + " ON CONFLICT (owner_id) DO UPDATE SET token_hash=excluded.token_hash,"
                    + "created_at=excluded.created_at",
                owner,
                fingerprint,
                Timestamp.from(createdAt)));
  }

  @Override
  public void revoke(String owner) {
    guarded(() -> jdbc.update("DELETE FROM calendar_feed_tokens WHERE owner_id=?", owner));
  }

  /** The whole digest is the only key; no prefix or partial comparison exists in the query. */
  @Override
  public Optional<String> ownerOf(byte[] fingerprint) {
    return guarded(
        () ->
            jdbc
                .query(
                    "SELECT owner_id FROM calendar_feed_tokens WHERE token_hash=?",
                    (row, index) -> row.getString("owner_id"),
                    (Object) fingerprint)
                .stream()
                .findFirst());
  }

  @Override
  public CalendarSnapshot read(String owner, Instant from, Instant to) {
    return guarded(
        () ->
            snapshot.execute(
                status ->
                    new CalendarSnapshot(
                        zoneOf(owner),
                        jdbc.query(
                            CURRENT_BLOCKS,
                            PostgresCalendarStore::entry,
                            owner,
                            Timestamp.from(from),
                            Timestamp.from(to)))));
  }

  private Optional<String> zoneOf(String owner) {
    return jdbc
        .query(
            "SELECT zone_id FROM availability_preferences WHERE owner_id=?",
            (row, index) -> row.getString("zone_id"),
            owner)
        .stream()
        .findFirst();
  }

  private static CalendarEntry entry(java.sql.ResultSet row, int index)
      throws java.sql.SQLException {
    return new CalendarEntry(
        row.getObject("block_id", UUID.class),
        row.getObject("project_id", UUID.class),
        row.getObject("task_id", UUID.class),
        row.getString("title"),
        row.getString("objective"),
        row.getObject("start_at", OffsetDateTime.class).toInstant(),
        row.getObject("end_at", OffsetDateTime.class).toInstant(),
        row.getLong("version"),
        row.getObject("stamped_at", OffsetDateTime.class).toInstant());
  }

  private static <T> T guarded(java.util.function.Supplier<T> work) {
    try {
      return work.get();
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
