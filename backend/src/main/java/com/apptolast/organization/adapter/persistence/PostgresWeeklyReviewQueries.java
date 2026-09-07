package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.application.WeeklyReviewQueries;
import com.apptolast.organization.domain.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresWeeklyReviewQueries implements WeeklyReviewQueries {
  private final PostgresAvailabilityStore availability;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresWeeklyReviewQueries(
      PostgresAvailabilityStore availability,
      JdbcTemplate jdbc,
      PlatformTransactionManager manager) {
    this.availability = availability;
    this.jdbc = jdbc;
    transaction = new TransactionTemplate(manager);
    transaction.setReadOnly(true);
    transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public WeeklyReview read(
      String owner, Function<Optional<Availability>, WeeklyReviewWindow> window) {
    try {
      return transaction.execute(
          status -> {
            var week = window.apply(availability.find(owner));
            var bounds = week.emptyReview();
            var planned =
                jdbc.query(
                    """
          SELECT coalesce(v.start_at,b.start_at) start_at,coalesce(v.end_at,b.end_at) end_at FROM planned_blocks b
          LEFT JOIN block_projections v ON v.block_id=b.id
          JOIN projects p ON p.id=b.project_id JOIN tasks t ON (t.project_id,t.id)=(b.project_id,b.task_id)
          WHERE p.owner_id=? AND coalesce(v.status,'planned')='planned' AND coalesce(v.start_at,b.start_at)<? AND coalesce(v.end_at,b.end_at)>?
          """,
                    (row, n) ->
                        new WeeklyReviewWindow.Interval(
                            row.getObject("start_at", OffsetDateTime.class).toInstant(),
                            row.getObject("end_at", OffsetDateTime.class).toInstant()),
                    owner,
                    bounds.endAt().atOffset(ZoneOffset.UTC),
                    bounds.startAt().atOffset(ZoneOffset.UTC));
            var sessions =
                jdbc.query(
                    """
          SELECT s.id,s.started_at,coalesce(s.changed_at,s.started_at) changed_at,s.changed_at IS NULL unknown_end,s.status,s.running_since,s.worked_microseconds,
            coalesce(s.last_decision_at,s.changed_at,s.started_at) decision_at,i.start_at,i.end_at,
            EXISTS(SELECT 1 FROM work_session_changes c WHERE c.session_id=s.id) has_changes
          FROM work_sessions s
          JOIN projects p ON p.id=s.project_id JOIN tasks t ON (t.project_id,t.id)=(s.project_id,s.task_id)
          LEFT JOIN work_session_intervals i ON i.session_id=s.id
          WHERE s.owner_id=? AND p.owner_id=? AND (
            EXISTS (SELECT 1 FROM work_session_intervals x WHERE x.session_id=s.id AND x.start_at<? AND x.end_at>?)
            OR (s.status='running' AND coalesce(s.running_since,s.started_at)<? AND ?::timestamptz>?)
            OR (s.status='closed' AND s.changed_at IS NULL AND s.started_at>=? AND s.started_at<?))
          ORDER BY s.id,i.start_at,i.end_at
          """,
                    (ResultSetExtractor<List<SessionWork>>)
                        row -> {
                          var result = new LinkedHashMap<UUID, SessionWork>();
                          while (row.next()) {
                            var id = row.getObject("id", UUID.class);
                            var session = result.get(id);
                            if (session == null) {
                              if (!Set.of("running", "paused", "closed")
                                      .contains(row.getString("status"))
                                  || (!"running".equals(row.getString("status"))
                                      && row.getObject("running_since") != null))
                                throw new StorageUnavailableException(
                                    new IllegalStateException("Unknown session status"));
                              session =
                                  new SessionWork(
                                      row.getBoolean("unknown_end")
                                          && "closed".equals(row.getString("status")),
                                      row.getBoolean("has_changes"),
                                      instant(row, "started_at"),
                                      instant(row, "changed_at"),
                                      instant(row, "decision_at"),
                                      row.getLong("worked_microseconds"),
                                      "running".equals(row.getString("status"))
                                          ? (instant(row, "running_since") == null
                                              ? instant(row, "started_at")
                                              : instant(row, "running_since"))
                                          : null,
                                      new ArrayList<>());
                              result.put(id, session);
                            }
                            if (row.getObject("start_at") != null)
                              session
                                  .intervals()
                                  .add(
                                      new WeeklyReviewWindow.Interval(
                                          instant(row, "start_at"), instant(row, "end_at")));
                          }
                          return List.copyOf(result.values());
                        },
                    owner,
                    owner,
                    bounds.endAt().atOffset(ZoneOffset.UTC),
                    bounds.startAt().atOffset(ZoneOffset.UTC),
                    bounds.endAt().atOffset(ZoneOffset.UTC),
                    bounds.serverNow().atOffset(ZoneOffset.UTC),
                    bounds.startAt().atOffset(ZoneOffset.UTC),
                    bounds.startAt().atOffset(ZoneOffset.UTC),
                    bounds.endAt().atOffset(ZoneOffset.UTC));
            var worked = new ArrayList<WeeklyReviewWindow.Interval>();
            long unquantified = 0;
            for (var session : sessions) {
              if (session.decision().isAfter(bounds.serverNow())
                  || session.changed().isAfter(bounds.serverNow())
                  || session.intervals().stream()
                      .anyMatch(i -> i.endAt().isAfter(bounds.serverNow())))
                throw new WeeklyReviewTimeOutOfRangeException();
              if (session.changed().isBefore(session.started())
                  || (session.runningSince() != null
                      && !session.runningSince().equals(session.changed())))
                throw new StorageUnavailableException(
                    new IllegalStateException("Inconsistent running interval"));
              if (session.unknownEnd()) {
                if (session.hasChanges() || !session.intervals().isEmpty() || session.worked() != 0)
                  throw new StorageUnavailableException(
                      new IllegalStateException("Missing session end"));
                unquantified++;
                continue;
              }

              Instant previous = session.started();
              for (var interval : session.intervals()) {
                if (interval.endAt().isBefore(interval.startAt())
                    || interval.endAt().isAfter(session.changed())
                    || (previous != null && interval.startAt().isBefore(previous)))
                  throw new StorageUnavailableException(
                      new IllegalStateException("Overlapping work intervals"));
                previous = interval.endAt();
              }
              var sum =
                  session.intervals().stream()
                      .mapToLong(
                          i -> java.time.temporal.ChronoUnit.MICROS.between(i.startAt(), i.endAt()))
                      .reduce(0, Math::addExact);
              if (sum != session.worked())
                throw new StorageUnavailableException(
                    new IllegalStateException("Inconsistent work session intervals"));
              worked.addAll(session.intervals());
              if (session.runningSince() != null)
                worked.add(
                    new WeeklyReviewWindow.Interval(session.runningSince(), bounds.serverNow()));
            }
            return week.summarize(planned, worked, unquantified);
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException
        | ArithmeticException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private static Instant instant(ResultSet row, String column) throws SQLException {
    var value = row.getObject(column, OffsetDateTime.class);
    return value == null ? null : value.toInstant();
  }

  private record SessionWork(
      boolean unknownEnd,
      boolean hasChanges,
      Instant started,
      Instant changed,
      Instant decision,
      long worked,
      Instant runningSince,
      List<WeeklyReviewWindow.Interval> intervals) {}
}
