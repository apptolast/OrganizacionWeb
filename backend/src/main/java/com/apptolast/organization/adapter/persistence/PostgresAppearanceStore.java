package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AppearanceQueries;
import com.apptolast.organization.domain.Appearance;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresAppearanceStore
    implements AppearanceQueries, com.apptolast.organization.application.AppearanceEditing {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresAppearanceStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(transaction.getTransactionManager());
    this.transaction.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public Optional<Appearance> find(String owner) {
    return select(owner, false);
  }

  private Optional<Appearance> select(String owner, boolean lock) {
    try {
      return jdbc
          .query(
              "SELECT * FROM appearance_preferences WHERE owner_id=?" + (lock ? " FOR UPDATE" : ""),
              (row, index) -> {
                var values =
                    new com.apptolast.organization.domain.AppearanceValues(
                        row.getString("theme"),
                        row.getString("accent_light"),
                        row.getString("accent_dark"));
                if (!values.accentLight().equals(row.getString("accent_light"))
                    || !values.accentDark().equals(row.getString("accent_dark"))
                    || row.getObject("id", UUID.class) == null
                    || row.getObject("version", Long.class) == null
                    || row.getLong("version") < 0)
                  throw new IllegalArgumentException("Stored appearance metadata invalid");
                var updated = row.getObject("updated_at", OffsetDateTime.class);
                if (updated.getYear() < 1 || updated.getYear() > 9999)
                  throw new IllegalArgumentException("Stored appearance timestamp invalid");
                return new Appearance(
                    row.getObject("id", UUID.class),
                    row.getString("owner_id"),
                    row.getString("theme"),
                    row.getString("accent_light"),
                    row.getString("accent_dark"),
                    row.getLong("version"),
                    updated.toInstant());
              },
              owner)
          .stream()
          .findFirst();
    } catch (RuntimeException error) {
      throw new com.apptolast.organization.application.StorageUnavailableException(error);
    }
  }

  public Appearance save(
      String owner, java.util.function.Function<Optional<Appearance>, Appearance> operation) {
    try {
      return transaction.execute(
          status -> {
            var prior = select(owner, true);
            var next = operation.apply(prior);
            if (prior.isPresent() && next == prior.get()) return next;
            if (prior.isPresent()) {
              int affected =
                  jdbc.update(
                      "UPDATE appearance_preferences SET theme=?,accent_light=?,accent_dark=?,version=?,updated_at=? WHERE owner_id=? AND id=? AND version=?",
                      next.theme(),
                      next.accentLight(),
                      next.accentDark(),
                      next.version(),
                      next.updatedAt().atOffset(java.time.ZoneOffset.UTC),
                      owner,
                      prior.get().id(),
                      prior.get().version());
              if (affected != 1)
                throw new com.apptolast.organization.application.StorageUnavailableException(
                    new IllegalStateException("Appearance update did not affect one row"));
              return next;
            }
            int inserted =
                jdbc.update(
                    "INSERT INTO appearance_preferences(id,owner_id,theme,accent_light,accent_dark,version,updated_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT(owner_id) DO NOTHING",
                    next.id(),
                    owner,
                    next.theme(),
                    next.accentLight(),
                    next.accentDark(),
                    next.version(),
                    next.updatedAt().atOffset(java.time.ZoneOffset.UTC));
            if (inserted != 1) {
              if (find(owner).isPresent())
                throw new com.apptolast.organization.application.AppearanceConflictException();
              throw new com.apptolast.organization.application.StorageUnavailableException(
                  new IllegalStateException("Appearance insert did not affect one row"));
            }
            return next;
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new com.apptolast.organization.application.StorageUnavailableException(error);
    }
  }
}
