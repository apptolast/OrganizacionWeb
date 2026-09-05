package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PublisherConfigurationTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "host,''",
    "username,''",
    "password,'   '",
    "vhost,''",
    "port,0",
    "port,65536",
    "port,invalid"
  })
  void s22_allInvalidSettingsStayInactive(String field, String value) {
    var work = org.mockito.Mockito.mock(com.apptolast.organization.application.OutboxWork.class);
    var audit =
        org.mockito.Mockito.mock(com.apptolast.organization.application.PublicationAudit.class);
    new ApplicationContextRunner()
        .withUserConfiguration(PublisherConfiguration.class)
        .withBean(com.apptolast.organization.application.OutboxWork.class, () -> work)
        .withBean(com.apptolast.organization.application.PublicationAudit.class, () -> audit)
        .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
        .withPropertyValues(
            "app.publisher.enabled=true",
            "app.publisher.rabbitmq.host=127.0.0.1",
            "app.publisher.rabbitmq.username=test",
            "app.publisher.rabbitmq.password=test-secret")
        .withPropertyValues("app.publisher.rabbitmq." + field + "=" + value)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              context.getBean(PublisherSchedule.class).tick();
              org.mockito.Mockito.verify(audit).workerError("CONFIGURATION_ERROR");
              org.mockito.Mockito.verifyNoInteractions(work);
            });
  }

  @Test
  void enabledValidConfigurationSchedulesRealCycles() {
    var first = new java.util.concurrent.CountDownLatch(1);
    com.apptolast.organization.application.OutboxWork work =
        (now, excluded, operation) -> {
          first.countDown();
          return java.util.Optional.empty();
        };
    var audit =
        org.mockito.Mockito.mock(com.apptolast.organization.application.PublicationAudit.class);
    new ApplicationContextRunner()
        .withUserConfiguration(PublisherConfiguration.class)
        .withBean(com.apptolast.organization.application.OutboxWork.class, () -> work)
        .withBean(com.apptolast.organization.application.PublicationAudit.class, () -> audit)
        .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
        .withPropertyValues(
            "app.publisher.enabled=true",
            "app.publisher.rabbitmq.host=127.0.0.1",
            "app.publisher.rabbitmq.username=test",
            "app.publisher.rabbitmq.password=test-secret")
        .run(
            context -> {
              assertThat(first.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
              org.mockito.Mockito.verifyNoInteractions(audit);
            });
  }

  @Test
  void s22_enabledMissingSecretIsReportedWithoutClaimingOrConnecting() {
    var work = org.mockito.Mockito.mock(com.apptolast.organization.application.OutboxWork.class);
    var audit =
        org.mockito.Mockito.mock(com.apptolast.organization.application.PublicationAudit.class);
    new ApplicationContextRunner()
        .withUserConfiguration(PublisherConfiguration.class)
        .withBean(com.apptolast.organization.application.OutboxWork.class, () -> work)
        .withBean(com.apptolast.organization.application.PublicationAudit.class, () -> audit)
        .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
        .withPropertyValues(
            "app.publisher.enabled=true",
            "app.publisher.rabbitmq.host=127.0.0.1",
            "app.publisher.rabbitmq.username=test",
            "app.publisher.rabbitmq.password=")
        .run(
            context -> {
              assertThat(context).hasSingleBean(PublisherSchedule.class);
              context.getBean(PublisherSchedule.class).tick();
              org.mockito.Mockito.verify(audit).workerError("CONFIGURATION_ERROR");
              org.mockito.Mockito.verifyNoInteractions(work);
            });
  }

  @Test
  void s19_disabledPublisherHasNoWorkerOrBrokerBeans() {
    new ApplicationContextRunner()
        .withUserConfiguration(PublisherConfiguration.class)
        .withPropertyValues("app.publisher.enabled=false")
        .run(
            context ->
                assertThat(context)
                    .doesNotHaveBean("publisherSchedule")
                    .doesNotHaveBean("outboxWork")
                    .doesNotHaveBean("brokerPublisher"));
  }

  @Test
  void s18_storageFailureSchedulesNextCycleAtLeastOneSecondLater() {
    var calls = new java.util.concurrent.CopyOnWriteArrayList<Long>();
    var second = new java.util.concurrent.CountDownLatch(2);
    com.apptolast.organization.application.OutboxWork work =
        (now, excluded, operation) -> {
          calls.add(System.nanoTime());
          second.countDown();
          throw new com.apptolast.organization.application.StorageUnavailableException(
              new IllegalStateException("unavailable"));
        };
    var audit =
        org.mockito.Mockito.mock(com.apptolast.organization.application.PublicationAudit.class);
    new ApplicationContextRunner()
        .withUserConfiguration(PublisherConfiguration.class)
        .withBean(com.apptolast.organization.application.OutboxWork.class, () -> work)
        .withBean(com.apptolast.organization.application.PublicationAudit.class, () -> audit)
        .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
        .withPropertyValues(
            "app.publisher.enabled=true",
            "app.publisher.rabbitmq.host=127.0.0.1",
            "app.publisher.rabbitmq.username=test",
            "app.publisher.rabbitmq.password=test-secret")
        .run(
            context -> {
              assertThat(second.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
              assertThat(calls.get(1) - calls.get(0)).isGreaterThanOrEqualTo(1_000_000_000L);
              org.mockito.Mockito.verify(audit, org.mockito.Mockito.atLeastOnce())
                  .workerError("STORAGE_UNAVAILABLE");
            });
  }
}
