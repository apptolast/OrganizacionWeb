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

  @Bean
  com.apptolast.organization.application.ReadProjects readProjects(
      com.apptolast.organization.application.ProjectQueries queries) {
    return new com.apptolast.organization.application.ReadProjects(queries);
  }

  @Bean
  com.apptolast.organization.application.EditProject editProject(
      com.apptolast.organization.application.ProjectEditing store, Clock clock) {
    return new com.apptolast.organization.application.EditProject(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ChangeProjectStatus changeProjectStatus(
      com.apptolast.organization.application.ProjectStatusEditing store,
      Clock clock,
      @org.springframework.beans.factory.annotation.Value("${app.max-active-projects:3}")
          String configured) {
    int limit = Integer.parseInt(configured);
    if (limit < 1 || limit > 10)
      throw new IllegalArgumentException("APP_MAX_ACTIVE_PROJECTS must be an integer from 1 to 10");
    return new com.apptolast.organization.application.ChangeProjectStatus(store, clock, limit);
  }

  @Bean
  com.apptolast.organization.application.CreateTask createTask(
      com.apptolast.organization.application.TaskCommit commit, Clock clock) {
    return new com.apptolast.organization.application.CreateTask(commit, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadTasks readTasks(
      com.apptolast.organization.application.TaskQueries queries) {
    return new com.apptolast.organization.application.ReadTasks(queries);
  }
}
