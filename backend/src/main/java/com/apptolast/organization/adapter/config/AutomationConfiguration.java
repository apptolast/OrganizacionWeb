package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.ExecuteAutomationsUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The rule engine exists only where app.automations.enabled is explicitly true. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.automations.enabled", havingValue = "true")
public class AutomationConfiguration {
  @Bean
  AutomationSchedule automationSchedule(ExecuteAutomationsUseCase execute) {
    return new AutomationSchedule(execute);
  }
}
