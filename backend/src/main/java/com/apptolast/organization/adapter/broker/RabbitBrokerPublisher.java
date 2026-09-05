package com.apptolast.organization.adapter.broker;

import com.apptolast.organization.application.BrokerPublisher;
import com.apptolast.organization.application.DeliveryOutcome;
import com.apptolast.organization.domain.OutboxMessage;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class RabbitBrokerPublisher implements BrokerPublisher {
  private final ConnectionFactory factory;

  public RabbitBrokerPublisher(String host, int port, String user, String password, String vhost) {
    factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(user);
    factory.setPassword(password);
    factory.setVirtualHost(vhost);
    factory.setHandshakeTimeout(1000);
    factory.setConnectionTimeout(1000);
    factory.setChannelRpcTimeout(1000);
    factory.setShutdownTimeout(1000);
    factory.setAutomaticRecoveryEnabled(false);
    factory.setTopologyRecoveryEnabled(false);
    factory.useNio();
    factory.setNioParams(
        new com.rabbitmq.client.impl.nio.NioParams().setWriteEnqueuingTimeoutInMs(1000));
    factory.setExceptionHandler(
        new com.rabbitmq.client.impl.StrictExceptionHandler() {
          @Override
          protected void log(String message, Throwable error) {
            // The application records only classified outcomes; transport exceptions may contain
            // secrets.
          }
        });
  }

  @Override
  public DeliveryOutcome publish(OutboxMessage message) {
    String kind =
        switch (message.type()) {
          case "ProjectCreated.v1" -> "created";
          case "ProjectUpdated.v1" -> "updated";
          case "ProjectStatusChanged.v1" -> "status-changed";
          default -> throw new IllegalArgumentException("Unsupported event type");
        };
    String queue = "organization.project-" + kind + ".v1", routing = "project." + kind + ".v1";
    com.rabbitmq.client.Connection connection = null;
    try {
      connection = factory.newConnection();
      var channel = connection.createChannel();
      channel.exchangeDeclare("organization.events", "direct", true);
      channel.queueDeclare(queue, true, false, false, Map.of("x-queue-type", "quorum"));
      channel.queueBind(queue, "organization.events", routing);
      channel.confirmSelect();
      var returned = new java.util.concurrent.atomic.AtomicBoolean();
      channel.addReturnListener((com.rabbitmq.client.ReturnCallback) reply -> returned.set(true));
      channel.basicPublish(
          "organization.events",
          routing,
          true,
          new AMQP.BasicProperties.Builder()
              .messageId(message.eventId().toString())
              .contentType("application/json")
              .deliveryMode(2)
              .build(),
          message.json().getBytes(StandardCharsets.UTF_8));
      try {
        boolean confirmed = channel.waitForConfirms(5000);
        return returned.get()
            ? DeliveryOutcome.UNROUTABLE
            : confirmed ? DeliveryOutcome.ACCEPTED : DeliveryOutcome.BROKER_NACK;
      } catch (java.util.concurrent.TimeoutException timeout) {
        return returned.get() ? DeliveryOutcome.UNROUTABLE : DeliveryOutcome.CONFIRM_TIMEOUT;
      }
    } catch (Exception error) {
      if (error instanceof InterruptedException) Thread.currentThread().interrupt();
      for (Throwable cause = error; cause != null; cause = cause.getCause()) {
        if (cause instanceof com.rabbitmq.client.ShutdownSignalException signal
            && signal.getReason() instanceof AMQP.Channel.Close close
            && close.getReplyCode() == 406)
          throw new com.apptolast.organization.application.TopologyMismatchException();
      }
      return DeliveryOutcome.BROKER_UNAVAILABLE;
    } finally {
      if (connection != null) connection.abort(1000);
    }
  }
}
