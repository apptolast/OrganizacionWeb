package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AvailabilityConflictException;
import com.apptolast.organization.application.AvailabilityEditing;
import com.apptolast.organization.application.AvailabilityQueries;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.Availability;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.util.*;
import java.util.function.Function;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresAvailabilityStore implements AvailabilityQueries, AvailabilityEditing {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresAvailabilityStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.transaction = transaction;
  }

  private static final RowMapper<Availability> MAPPER =
      (row, n) -> {
        var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
        for (var day : DayOfWeek.values())
          days.put(day, row.getInt(day.name().toLowerCase(Locale.ROOT) + "_minutes"));
        return new Availability(
            row.getObject("id", UUID.class),
            row.getString("owner_id"),
            row.getString("zone_id"),
            days,
            row.getLong("version"),
            row.getTimestamp("created_at").toInstant(),
            row.getTimestamp("updated_at").toInstant());
      };

  public Optional<Availability> find(String owner) {
    try {
      return jdbc
          .query("SELECT * FROM availability_preferences WHERE owner_id=?", MAPPER, owner)
          .stream()
          .findFirst();
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  public Availability save(String owner, Function<Optional<Availability>, Availability> operation) {
    try {
      return transaction.execute(
          status -> {
            var prior =
                jdbc
                    .query(
                        "SELECT * FROM availability_preferences WHERE owner_id=? FOR UPDATE",
                        MAPPER,
                        owner)
                    .stream()
                    .findFirst();
            var next = operation.apply(prior);
            if (prior.isPresent() && next == prior.get()) return next;
            if (prior.isPresent()) {
              var update = new ArrayList<Object>();
              update.add(next.zoneId());
              for (var day : DayOfWeek.values()) update.add(next.dailyMinutes().get(day));
              update.add(next.version());
              update.add(Timestamp.from(next.updatedAt()));
              update.add(owner);
              update.add(next.id());
              update.add(prior.get().version());
              int updated =
                  jdbc.update(
                      "UPDATE availability_preferences SET zone_id=?,monday_minutes=?,tuesday_minutes=?,wednesday_minutes=?,thursday_minutes=?,friday_minutes=?,saturday_minutes=?,sunday_minutes=?,version=?,updated_at=? WHERE owner_id=? AND id=? AND version=?",
                      update.toArray());
              if (updated != 1)
                throw new StorageUnavailableException(
                    new IllegalStateException("Preference update did not affect one row"));
              return next;
            }
            var values = new ArrayList<Object>();
            values.add(next.id());
            values.add(owner);
            values.add(next.zoneId());
            for (var day : DayOfWeek.values()) values.add(next.dailyMinutes().get(day));
            values.add(next.version());
            values.add(Timestamp.from(next.createdAt()));
            values.add(Timestamp.from(next.updatedAt()));
            int inserted =
                jdbc.update(
                    "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (owner_id) DO NOTHING",
                    values.toArray());
            // READ_COMMITTED makes the winning insert visible after ON CONFLICT waits.
            // This distinguishes a concurrent winner from a trigger suppressing the write.
            if (inserted != 1) {
              if (find(owner).isPresent()) throw new AvailabilityConflictException();
              throw new StorageUnavailableException(
                  new IllegalStateException("Preference insert did not affect one row"));
            }
            return next;
          });
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
