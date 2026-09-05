package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.apptolast.organization.adapter.logging.Slf4jPublicationAudit;
import com.apptolast.organization.domain.PublicationAttempt;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;

class PublicationAuditTest {
  @org.junit.jupiter.api.Test
  void s18_workerFailureHasOnlyStableCode() {
    Logger logger = (Logger) LoggerFactory.getLogger("organization.outbox");
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      new Slf4jPublicationAudit().workerError("STORAGE_UNAVAILABLE");
      assertThat(appender.list)
          .singleElement()
          .satisfies(
              event ->
                  assertThat(event.getFormattedMessage())
                      .isEqualTo("outcome=worker_error code=STORAGE_UNAVAILABLE"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @ParameterizedTest
  @CsvSource({"published,1,,1", "retry,3,BROKER_UNAVAILABLE,3", "blocked,3,INVALID_EVENT,0"})
  void s23_recordsOnlySafeOutcomeMetadata(
      String outcome, long attempts, String code, long loggedAttempt) {
    Logger logger = (Logger) LoggerFactory.getLogger("organization.outbox");
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    UUID eventId = UUID.randomUUID();
    try {
      new Slf4jPublicationAudit()
          .event(new PublicationAttempt(eventId, outcome, attempts, Instant.EPOCH, null, code));
      assertThat(appender.list).hasSize(1);
      String record = appender.list.getFirst().getFormattedMessage();
      assertThat(record)
          .contains("eventId=" + eventId, "outcome=" + outcome, "attempt=" + loggedAttempt);
      if (code != null) assertThat(record).contains("code=" + code);
      assertThat(record)
          .doesNotContain("ownerId", "payload", "description", "password", "amqp://", "name=");
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
