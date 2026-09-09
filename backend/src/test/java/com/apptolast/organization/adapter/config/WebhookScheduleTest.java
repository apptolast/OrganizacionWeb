package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.DispatchWebhooksUseCase;
import com.apptolast.organization.application.EnqueueWebhookDeliveriesUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WebhookScheduleTest {
  private final List<String> calls = new ArrayList<>();
  private final EnqueueWebhookDeliveriesUseCase enqueue = () -> calls.add("enqueue");
  private final DispatchWebhooksUseCase dispatch = () -> calls.add("dispatch");

  @Test
  void s18_s20_eachTickEnqueuesBeforeItDispatchesSoANewEventCanShipInTheSameTick() {
    new WebhookSchedule(enqueue, dispatch).tick();

    assertEquals(List.of("enqueue", "dispatch"), calls);
  }

  @Test
  void s32_aFailingCycleNeverEscapesTheScheduledMethod() {
    EnqueueWebhookDeliveriesUseCase broken =
        () -> {
          throw new IllegalStateException("test-only failure");
        };

    assertDoesNotThrow(() -> new WebhookSchedule(broken, dispatch).tick());
    assertEquals(List.of("dispatch"), calls, "a broken enqueue must not stop the dispatch");
  }

  @Test
  void s32_theWorkerOnlyExistsWhenTheFlagIsExplicitlyTrue() {
    var runner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .withBean(EnqueueWebhookDeliveriesUseCase.class, () -> enqueue)
            .withBean(DispatchWebhooksUseCase.class, () -> dispatch)
            .withUserConfiguration(WebhookConfiguration.class);

    runner.run(context -> assertFalse(context.containsBean("webhookSchedule")));
    runner
        .withPropertyValues("app.webhooks.enabled=false")
        .run(context -> assertFalse(context.containsBean("webhookSchedule")));
    runner
        .withPropertyValues("app.webhooks.enabled=true")
        .run(context -> assertTrue(context.containsBean("webhookSchedule")));
  }
}
