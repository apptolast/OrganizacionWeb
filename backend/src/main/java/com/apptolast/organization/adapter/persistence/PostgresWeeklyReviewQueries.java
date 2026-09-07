package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.WeeklyReviewQueries;
import com.apptolast.organization.domain.*;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
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
    return transaction.execute(status -> window.apply(availability.find(owner)).emptyReview());
  }
}
