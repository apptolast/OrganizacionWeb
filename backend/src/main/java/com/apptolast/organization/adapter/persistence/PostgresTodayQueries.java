package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.application.TodayQueries;
import com.apptolast.organization.domain.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;

@org.springframework.stereotype.Component
public final class PostgresTodayQueries implements TodayQueries {
  private final PostgresAvailabilityStore availability;
  private final JdbcTemplate jdbc;
  private final org.springframework.transaction.support.TransactionTemplate transaction;

  public PostgresTodayQueries(
      PostgresAvailabilityStore availability,
      JdbcTemplate jdbc,
      org.springframework.transaction.PlatformTransactionManager manager) {
    this.availability = availability;
    this.jdbc = jdbc;
    this.transaction = new org.springframework.transaction.support.TransactionTemplate(manager);
    transaction.setReadOnly(true);
    transaction.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
  }

  public TodayWindow.Agenda read(
      String owner, Function<Optional<Availability>, TodayWindow> window) {
    try {
      return transaction.execute(
          status -> {
            var day = window.apply(availability.find(owner));
            var items =
                jdbc.query(
                    "SELECT "
                        + PostgresBlockStore.CURRENT_COLUMNS
                        + ",owner_project.name AS project_name,t.title AS task_title FROM"
                        + " planned_blocks b LEFT JOIN block_projections p ON p.block_id=b.id JOIN"
                        + " projects owner_project ON owner_project.id=b.project_id JOIN tasks t ON"
                        + " t.project_id=b.project_id AND t.id=b.task_id WHERE"
                        + " owner_project.owner_id=? AND coalesce(p.status,'planned')='planned' AND"
                        + " coalesce(p.start_at,b.start_at)<? AND coalesce(p.end_at,b.end_at)>?"
                        + " ORDER BY coalesce(p.start_at,b.start_at),b.id",
                    (row, n) ->
                        new TodayItem(
                            PostgresBlockStore.MAPPER.mapRow(row, n),
                            row.getString("project_name"),
                            row.getString("task_title")),
                    owner,
                    Timestamp.from(day.dayEndAt()),
                    Timestamp.from(day.dayStartAt()));
            return day.summarize(items);
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
