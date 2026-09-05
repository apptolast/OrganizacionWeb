package com.apptolast.organization.adapter.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.DeliveryOutcome;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.extension.ExtendWith(
    org.springframework.boot.test.system.OutputCaptureExtension.class)
class RabbitBrokerFailuresTest {
  @Test
  void s5_timedOutChannelIsAbortedBeforeFreshAttempt() throws Exception {
    var first = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var second = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var oldChannel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    var freshChannel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(first.createChannel()).thenReturn(oldChannel);
    org.mockito.Mockito.when(second.createChannel()).thenReturn(freshChannel);
    org.mockito.Mockito.when(oldChannel.waitForConfirms(5000))
        .thenThrow(new java.util.concurrent.TimeoutException())
        .thenReturn(true);
    org.mockito.Mockito.when(freshChannel.waitForConfirms(5000))
        .thenThrow(new java.util.concurrent.TimeoutException());
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(first, second))) {
      var publisher = new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/");
      var event = new RabbitBrokerPublisherTest().message();
      assertThat(publisher.publish(event)).isEqualTo(DeliveryOutcome.CONFIRM_TIMEOUT);
      assertThat(publisher.publish(event)).isEqualTo(DeliveryOutcome.CONFIRM_TIMEOUT);
      var order = org.mockito.Mockito.inOrder(first, second, freshChannel);
      order.verify(first).abort(1000);
      order.verify(second).createChannel();
      order.verify(freshChannel).waitForConfirms(5000);
      org.mockito.Mockito.verify(oldChannel, org.mockito.Mockito.times(1)).waitForConfirms(5000);
    }
  }

  @Test
  void s5_shutdownCancellationPreservesInterruptFlag() throws Exception {
    var connection = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var channel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(connection.createChannel()).thenReturn(channel);
    org.mockito.Mockito.when(channel.waitForConfirms(5000)).thenThrow(new InterruptedException());
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(connection))) {
      assertThat(
              new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/")
                  .publish(new RabbitBrokerPublisherTest().message()))
          .isEqualTo(DeliveryOutcome.BROKER_UNAVAILABLE);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void s6_returnRemainsAuthoritativeWhenConfirmTimesOut() throws Exception {
    var connection = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var channel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(connection.createChannel()).thenReturn(channel);
    var callback =
        new java.util.concurrent.atomic.AtomicReference<com.rabbitmq.client.ReturnCallback>();
    org.mockito.Mockito.when(
            channel.addReturnListener(
                org.mockito.ArgumentMatchers.any(com.rabbitmq.client.ReturnCallback.class)))
        .thenAnswer(
            call -> {
              callback.set(call.getArgument(0));
              return null;
            });
    org.mockito.Mockito.when(channel.waitForConfirms(5000))
        .thenAnswer(
            call -> {
              callback
                  .get()
                  .handle(
                      new com.rabbitmq.client.Return(
                          312,
                          "NO_ROUTE",
                          "organization.events",
                          "project.created.v1",
                          null,
                          new byte[0]));
              throw new java.util.concurrent.TimeoutException();
            });
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(connection))) {
      assertThat(
              new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/")
                  .publish(new RabbitBrokerPublisherTest().message()))
          .isEqualTo(DeliveryOutcome.UNROUTABLE);
    }
  }

  @Test
  void s23_clientExceptionDoesNotExposeSensitiveDetails(
      org.springframework.boot.test.system.CapturedOutput output) throws Exception {
    var publisher =
        new RabbitBrokerPublisher("broker", 5672, "canary-user", "canary-password", "/");
    var field = RabbitBrokerPublisher.class.getDeclaredField("factory");
    field.setAccessible(true);
    var factory = (com.rabbitmq.client.ConnectionFactory) field.get(publisher);
    factory
        .getExceptionHandler()
        .handleUnexpectedConnectionDriverException(
            org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class),
            new java.io.IOException(
                "amqp://canary-user:canary-password@broker/private-owner/private-name"));
    assertThat(output.getAll())
        .doesNotContain("canary-user", "canary-password", "private-owner", "private-name");
  }

  @Test
  void s5_transportAndCleanupHaveFiniteBoundsWithoutRecovery() throws Exception {
    var connection = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var channel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(connection.createChannel()).thenReturn(channel);
    org.mockito.Mockito.when(channel.waitForConfirms(5000)).thenReturn(true);
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(connection))) {
      new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/")
          .publish(new RabbitBrokerPublisherTest().message());
      var factory = factories.constructed().getFirst();
      org.mockito.Mockito.verify(factory).setAutomaticRecoveryEnabled(false);
      org.mockito.Mockito.verify(factory).setTopologyRecoveryEnabled(false);
      org.mockito.Mockito.verify(factory).setConnectionTimeout(1000);
      org.mockito.Mockito.verify(factory).setChannelRpcTimeout(1000);
      org.mockito.Mockito.verify(factory).setShutdownTimeout(1000);
      org.mockito.Mockito.verify(factory).useNio();
      var nio = org.mockito.ArgumentCaptor.forClass(com.rabbitmq.client.impl.nio.NioParams.class);
      org.mockito.Mockito.verify(factory).setNioParams(nio.capture());
      assertThat(nio.getValue().getWriteEnqueuingTimeoutInMs()).isEqualTo(1000);
      org.mockito.Mockito.verify(connection).abort(1000);
      org.mockito.Mockito.verify(connection, org.mockito.Mockito.never()).close();
      org.mockito.Mockito.verify(channel, org.mockito.Mockito.never()).close();
    }
  }

  @Test
  void s5_tcpPeerThatNeverHandshakesIsBounded() throws Exception {
    try (var server = new ServerSocket(0)) {
      var accepted = new java.util.concurrent.CompletableFuture<java.net.Socket>();
      var accepting =
          Thread.ofPlatform()
              .start(
                  () -> {
                    try {
                      accepted.complete(server.accept());
                    } catch (Exception error) {
                      accepted.completeExceptionally(error);
                    }
                  });
      try {
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
            java.time.Duration.ofSeconds(3),
            () ->
                assertThat(
                        new RabbitBrokerPublisher(
                                "127.0.0.1", server.getLocalPort(), "test", "secret", "/")
                            .publish(new RabbitBrokerPublisherTest().message()))
                    .isEqualTo(DeliveryOutcome.BROKER_UNAVAILABLE));
      } finally {
        if (accepted.isDone()) accepted.get().close();
        server.close();
        accepting.join(1000);
      }
    }
  }

  @Test
  void s5_unavailableBrokerReturnsRetryCode() throws Exception {
    int closedPort;
    try (var socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    var publisher =
        new RabbitBrokerPublisher("127.0.0.1", closedPort, "test-user", "test-secret", "/");
    assertThat(publisher.publish(new RabbitBrokerPublisherTest().message()))
        .isEqualTo(DeliveryOutcome.BROKER_UNAVAILABLE);
  }

  @Test
  void s5_negativeConfirmIsNotAcceptance() throws Exception {
    var connection = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var channel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(connection.createChannel()).thenReturn(channel);
    org.mockito.Mockito.when(channel.waitForConfirms(5000)).thenReturn(false);
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(connection))) {
      assertThat(
              new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/")
                  .publish(new RabbitBrokerPublisherTest().message()))
          .isEqualTo(DeliveryOutcome.BROKER_NACK);
    }
  }

  @Test
  void s5_confirmTimeoutKeepsRetryClassification() throws Exception {
    var connection = org.mockito.Mockito.mock(com.rabbitmq.client.Connection.class);
    var channel = org.mockito.Mockito.mock(com.rabbitmq.client.Channel.class);
    org.mockito.Mockito.when(connection.createChannel()).thenReturn(channel);
    org.mockito.Mockito.when(channel.waitForConfirms(5000))
        .thenThrow(new java.util.concurrent.TimeoutException());
    try (var factories =
        org.mockito.Mockito.mockConstruction(
            com.rabbitmq.client.ConnectionFactory.class,
            (factory, context) ->
                org.mockito.Mockito.when(factory.newConnection()).thenReturn(connection))) {
      assertThat(
              new RabbitBrokerPublisher("broker", 5672, "test", "secret", "/")
                  .publish(new RabbitBrokerPublisherTest().message()))
          .isEqualTo(DeliveryOutcome.CONFIRM_TIMEOUT);
    }
  }
}
