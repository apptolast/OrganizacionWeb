package com.apptolast.organization.adapter.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @s12: el registro lleva host, código y duración, y nunca la ruta de la dirección.
 */
class ExternalCalendarAuditTest {
  static final String HOST = "feed.example.test";

  static void recording(Consumer<ListAppender<ILoggingEvent>> work) {
    Logger logger = (Logger) LoggerFactory.getLogger("organization.external-calendar");
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      work.accept(appender);
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void s12_aFailureRecordsHostCodeAndDuration() {
    recording(
        appender -> {
          new Slf4jExternalCalendarAudit()
              .syncFinished(HOST, SyncStatus.FAILED, FeedError.FEED_HTTP_ERROR, 137);
          assertThat(appender.list)
              .singleElement()
              .satisfies(
                  event ->
                      assertThat(event.getFormattedMessage())
                          .isEqualTo(
                              "host=feed.example.test status=FAILED code=FEED_HTTP_ERROR"
                                  + " durationMs=137"));
        });
  }

  @Test
  void s14_aSuccessRecordsNoErrorCode() {
    recording(
        appender -> {
          new Slf4jExternalCalendarAudit().syncFinished(HOST, SyncStatus.OK, null, 42);
          assertThat(appender.list)
              .singleElement()
              .satisfies(
                  event ->
                      assertThat(event.getFormattedMessage())
                          .isEqualTo("host=feed.example.test status=OK code=NONE durationMs=42"));
        });
  }

  @Test
  void s2_theRecordNeverCarriesThePathOfTheAddress() {
    recording(
        appender -> {
          new Slf4jExternalCalendarAudit()
              .syncFinished(HOST, SyncStatus.FAILED, FeedError.SECRET_UNREADABLE, 0);
          assertThat(appender.list.getFirst().getFormattedMessage())
              .doesNotContain("abc123")
              .doesNotContain("https://")
              .doesNotContain("/calendar/");
        });
  }
}
