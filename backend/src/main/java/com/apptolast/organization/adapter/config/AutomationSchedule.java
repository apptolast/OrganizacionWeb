package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.ExecuteAutomationsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * One tick of the rule engine.
 *
 * <p>The method may not throw: a scheduled method that propagates stops the whole schedule.
 */
public final class AutomationSchedule {
  private static final Logger LOG = LoggerFactory.getLogger(AutomationSchedule.class);

  private final ExecuteAutomationsUseCase execute;

  public AutomationSchedule(ExecuteAutomationsUseCase execute) {
    this.execute = execute;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 1000)
  public void tick() {
    try {
      execute.runCycle();
    } catch (RuntimeException failure) {
      // The class of the failure is enough: no title, project name nor payload is ever logged.
      LOG.warn("Automation worker cycle failed; category={}", failure.getClass().getSimpleName());
    }
  }
}
