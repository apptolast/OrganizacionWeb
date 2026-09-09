package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.DispatchWebhooksUseCase;
import com.apptolast.organization.application.EnqueueWebhookDeliveriesUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The webhook worker exists only where app.webhooks.enabled is explicitly true. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.webhooks.enabled", havingValue = "true")
public class WebhookConfiguration {
  @Bean
  WebhookSchedule webhookSchedule(
      EnqueueWebhookDeliveriesUseCase enqueue, DispatchWebhooksUseCase dispatch) {
    return new WebhookSchedule(enqueue, dispatch);
  }
}
