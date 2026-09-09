package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.ExecuteAutomationsUseCase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AutomationScheduleTest {
  private final List<String> cycles = new CopyOnWriteArrayList<>();
  private final ExecuteAutomationsUseCase execute = () -> cycles.add("cycle");

  @Test
  void s15_withoutTheFlagThereIsNoWorkerAndNothingIsEverRead() throws InterruptedException {
    var runner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .withBean(ExecuteAutomationsUseCase.class, () -> execute)
            .withUserConfiguration(AutomationConfiguration.class);

    runner.run(context -> assertFalse(context.containsBean("automationSchedule")));
    runner
        .withPropertyValues("app.automations.enabled=false")
        .run(context -> assertFalse(context.containsBean("automationSchedule")));
    Thread.sleep(1500);
    assertEquals(List.of(), cycles, "a disabled worker must not read nor write anything");

    runner
        .withPropertyValues("app.automations.enabled=true")
        .run(context -> assertTrue(context.containsBean("automationSchedule")));
  }

  @Test
  void s15_eachTickRunsOneCycle() {
    new AutomationSchedule(execute).tick();

    assertEquals(List.of("cycle"), cycles);
  }

  @Test
  void s20_aFailingCycleNeverEscapesTheScheduledMethod() {
    ExecuteAutomationsUseCase broken =
        () -> {
          throw new IllegalStateException("test-only failure");
        };

    assertDoesNotThrow(() -> new AutomationSchedule(broken).tick());
  }
}
