package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.CreateProject;
import com.apptolast.organization.application.ProjectCommit;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  CreateProject createProject(ProjectCommit commit, Clock clock) {
    return new CreateProject(commit, clock);
  }
}
