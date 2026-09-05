package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.adapter.broker.RabbitBrokerPublisher;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.PublicationAttempt;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Test-only executable: an OS kill interrupts a real open PostgreSQL transaction. */
public final class PublisherCrashProcess {
  public static void main(String[] arguments) {
    var env = System.getenv();
    var source =
        new DriverManagerDataSource(
            env.get("TEST_DB_URL"), env.get("TEST_DB_USER"), env.get("TEST_DB_PASSWORD"));
    var broker =
        new RabbitBrokerPublisher(
            env.get("TEST_RABBIT_HOST"),
            Integer.parseInt(env.get("TEST_RABBIT_PORT")),
            env.get("TEST_RABBIT_USER"),
            env.get("TEST_RABBIT_PASSWORD"),
            "/");
    BrokerPublisher barrier =
        message -> {
          if ("AFTER".equals(arguments[0]) && broker.publish(message) != DeliveryOutcome.ACCEPTED)
            throw new AssertionError("Broker did not accept fixture event");
          try {
            Files.writeString(Path.of(arguments[1]), "READY");
            new CountDownLatch(1).await();
            throw new AssertionError("Barrier unexpectedly released");
          } catch (Exception error) {
            throw new IllegalStateException("Fixture barrier failed", error);
          }
        };
    var audit =
        new PublicationAudit() {
          public void event(PublicationAttempt attempt) {}

          public void workerError(String code) {
            throw new AssertionError(code);
          }
        };
    new PublishOutbox(
            new PostgresOutboxWork(
                new JdbcTemplate(source),
                new TransactionTemplate(new DataSourceTransactionManager(source)),
                new ObjectMapper()),
            barrier,
            audit,
            Clock.systemUTC())
        .runCycle();
    throw new AssertionError("Child did not reach transaction barrier");
  }
}
