package com.apptolast.organization.adapter.config;

import com.apptolast.organization.adapter.broker.RabbitBrokerPublisher;
import com.apptolast.organization.adapter.logging.Slf4jPublicationAudit;
import com.apptolast.organization.adapter.persistence.PostgresOutboxWork;
import com.apptolast.organization.application.*;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ConditionalOnProperty(name = "app.publisher.enabled", havingValue = "true")
@EnableScheduling
public class PublisherConfiguration {
  @Bean
  @ConditionalOnMissingBean
  OutboxWork outboxWork(
      org.springframework.jdbc.core.JdbcTemplate jdbc,
      org.springframework.transaction.support.TransactionTemplate transaction,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    return new PostgresOutboxWork(jdbc, transaction, json);
  }

  @Bean
  @ConditionalOnMissingBean
  PublicationAudit publicationAudit() {
    return new Slf4jPublicationAudit();
  }

  @Bean
  PublisherSchedule publisherSchedule(
      OutboxWork work,
      PublicationAudit audit,
      Clock clock,
      @Value("${app.publisher.rabbitmq.host:}") String host,
      @Value("${app.publisher.rabbitmq.port:5672}") String configuredPort,
      @Value("${app.publisher.rabbitmq.username:}") String username,
      @Value("${app.publisher.rabbitmq.password:}") String password,
      @Value("${app.publisher.rabbitmq.vhost:/}") String vhost) {
    int port;
    try {
      port = Integer.parseInt(configuredPort);
    } catch (NumberFormatException error) {
      port = 0;
    }
    if (host.isBlank()
        || username.isBlank()
        || password.isBlank()
        || vhost.isBlank()
        || port < 1
        || port > 65535) {
      audit.workerError("CONFIGURATION_ERROR");
      return new PublisherSchedule(() -> {});
    }
    return new PublisherSchedule(
        new PublishOutbox(
            work, new RabbitBrokerPublisher(host, port, username, password, vhost), audit, clock));
  }
}
