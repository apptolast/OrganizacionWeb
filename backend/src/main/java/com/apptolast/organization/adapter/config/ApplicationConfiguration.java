package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.CreateProject;
import com.apptolast.organization.application.ProjectCommit;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
  @Bean
  com.apptolast.organization.application.CancelBlock cancelBlock(
      com.apptolast.organization.application.BlockEditing store, Clock clock) {
    return new com.apptolast.organization.application.CancelBlock(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadBlocks readBlocks(
      com.apptolast.organization.application.BlockQueries queries) {
    return new com.apptolast.organization.application.ReadBlocks(queries);
  }

  @Bean
  com.apptolast.organization.application.PlanBlock planBlock(
      com.apptolast.organization.application.BlockPlanning planning,
      com.apptolast.organization.application.BlockCommit commit,
      com.apptolast.organization.application.ZoneCatalog catalog,
      Clock clock) {
    return new com.apptolast.organization.application.PlanBlock(planning, commit, catalog, clock);
  }

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

  @Bean
  com.apptolast.organization.application.CreateSubtask createSubtask(
      com.apptolast.organization.application.SubtaskCommit commit, Clock clock) {
    return new com.apptolast.organization.application.CreateSubtask(commit, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadSubtasks readSubtasks(
      com.apptolast.organization.application.SubtaskQueries queries) {
    return new com.apptolast.organization.application.ReadSubtasks(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadTaskStatus readTaskStatus(
      com.apptolast.organization.application.TaskStatusQueries queries) {
    return new com.apptolast.organization.application.ReadTaskStatus(queries);
  }

  @Bean
  com.apptolast.organization.application.ChangeTaskStatus changeTaskStatus(
      com.apptolast.organization.application.TaskStatusEditing store, Clock clock) {
    return new com.apptolast.organization.application.ChangeTaskStatus(store, clock);
  }

  @Bean
  com.apptolast.organization.application.ReadTaskHistory readTaskHistory(
      com.apptolast.organization.application.TaskHistoryQueries queries) {
    return new com.apptolast.organization.application.ReadTaskHistory(queries);
  }

  @Bean
  com.apptolast.organization.application.ReadAvailability readAvailability(
      com.apptolast.organization.application.AvailabilityQueries queries,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.ReadAvailability(queries, catalog);
  }

  @Bean
  com.apptolast.organization.application.SaveAvailability saveAvailability(
      com.apptolast.organization.application.AvailabilityEditing store,
      com.apptolast.organization.application.ZoneCatalog catalog,
      Clock clock) {
    return new com.apptolast.organization.application.SaveAvailability(store, catalog, clock);
  }

  @org.springframework.context.annotation.Bean
  com.apptolast.organization.application.ReadToday readToday(
      com.apptolast.organization.application.TodayQueries queries,
      Clock clock,
      com.apptolast.organization.application.ZoneCatalog catalog) {
    return new com.apptolast.organization.application.ReadToday(queries, clock, catalog);
  }
}
