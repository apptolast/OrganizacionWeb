package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.AvailabilityRevision;
import com.apptolast.organization.domain.BlockRequest;
import com.apptolast.organization.domain.ProjectRevision;
import com.apptolast.organization.domain.TaskRevision;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApplicationWiringTest {
  @Test
  void weekly_s1_readBeanUsesTheRealQueries() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(WeeklyReviewQueries.class))
                  .isInstanceOf(
                      com.apptolast.organization.adapter.persistence.PostgresWeeklyReviewQueries
                          .class);
              org.mockito.Mockito.doAnswer(
                      call -> {
                        var extractor =
                            (org.springframework.jdbc.core.ResultSetExtractor<?>)
                                call.getArgument(1);
                        return extractor.extractData(mock(java.sql.ResultSet.class));
                      })
                  .when(context.getBean(org.springframework.jdbc.core.JdbcTemplate.class))
                  .query(
                      org.mockito.ArgumentMatchers.anyString(),
                      org.mockito.ArgumentMatchers
                          .<org.springframework.jdbc.core.ResultSetExtractor<?>>any(),
                      org.mockito.ArgumentMatchers.any(Object[].class));
              var result =
                  context
                      .getBean(ReadWeeklyReviewUseCase.class)
                      .get("owner", java.time.LocalDate.parse("2026-09-07"), "UTC");
              assertThat(result.weekStart()).isEqualTo(java.time.LocalDate.parse("2026-09-07"));
              assertThat(result.days()).hasSize(7);
            });
  }

  private static final UUID PROJECT = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TASK = UUID.fromString("00000000-0000-0000-0000-000000000002");

  private static final class PortReached extends RuntimeException {
    private final Class<?> port;

    PortReached(Class<?> port) {
      this.port = port;
    }
  }

  private static <T> ApplicationContextRunner addPort(
      ApplicationContextRunner runner, Class<T> port) {
    return runner.withBean(
        port,
        () ->
            mock(
                port,
                invocation -> {
                  throw new PortReached(port);
                }));
  }

  private static ApplicationContextRunner freshContext() {
    var runner =
        new ApplicationContextRunner()
            .withUserConfiguration(ApplicationConfiguration.class)
            .withPropertyValues("app.auth.username=owner")
            .withBean(
                org.springframework.security.core.userdetails.UserDetailsService.class,
                () ->
                    new org.springframework.security.provisioning.InMemoryUserDetailsManager(
                        org.springframework.security.core.userdetails.User.withUsername("owner")
                            .password("{noop}fixture-unused")
                            .roles("USER")
                            .build()))
            .withBean(
                org.springframework.jdbc.core.JdbcTemplate.class,
                () -> org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class))
            .withBean(
                org.springframework.transaction.PlatformTransactionManager.class,
                () ->
                    org.mockito.Mockito.mock(
                        org.springframework.transaction.PlatformTransactionManager.class))
            .withBean(
                com.fasterxml.jackson.databind.ObjectMapper.class,
                com.fasterxml.jackson.databind.ObjectMapper::new)
            .withBean(ZoneCatalog.class, () -> () -> Set.of("UTC"));
    for (var port :
        List.of(
            BlockMoving.class,
            BlockChangeQueries.class,
            BlockEditing.class,
            TodayQueries.class,
            BlockQueries.class,
            BlockPlanning.class,
            BlockCommit.class,
            ProjectCommit.class,
            ProjectQueries.class,
            ProjectEditing.class,
            ProjectStatusEditing.class,
            TaskCommit.class,
            TaskQueries.class,
            TaskStatusEditing.class,
            TaskStatusQueries.class,
            TaskHistoryQueries.class,
            SubtaskCommit.class,
            SubtaskQueries.class,
            AvailabilityQueries.class,
            AvailabilityEditing.class,
            // El cifrado de secretos de los conectores lo publica ConnectorConfiguration, que este
            // contexto delgado no carga; sin él los beans del calendario externo no se construyen.
            SecretCipher.class)) {
      runner = addPort(runner, port);
    }
    return runner;
  }

  @Test
  void end_s13_readBeanUsesTheRealStore() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ReadWorkSessionEndUseCase.class)
                              .read("owner", UUID.randomUUID()))
                  .isInstanceOf(WorkSessionNotFoundException.class);
            });
  }

  @Test
  void end_s1_extensionBeanUsesTheRealStore() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var id = UUID.randomUUID();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ExtendWorkSessionUseCase.class)
                              .extend(
                                  "owner",
                                  id,
                                  UUID.randomUUID(),
                                  new WorkSessionRevision(id, 1),
                                  5))
                  .isInstanceOf(WorkSessionNotFoundException.class);
            });
  }

  @Test
  void pause_s25_receiptBeanUsesTheRealStore() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ReadWorkSessionChangesUseCase.class)
                              .byRequest("owner", UUID.randomUUID()))
                  .isInstanceOf(WorkSessionChangeNotFoundException.class);
            });
  }

  @Test
  void pause_s1_stateBeanUsesTheRealStore() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ReadWorkSessionStateUseCase.class)
                              .read("owner", UUID.randomUUID()))
                  .isInstanceOf(WorkSessionNotFoundException.class);
            });
  }

  @Test
  void pause_s2_commandBeanUsesTheRealStore() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              var id = UUID.randomUUID();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ChangeWorkSessionUseCase.class)
                              .pause(
                                  "owner", id, UUID.randomUUID(), new WorkSessionRevision(id, 1)))
                  .isInstanceOf(WorkSessionNotFoundException.class);
            });
  }

  @Test
  void reschedule_s12_cancelBeanReachesItsPortInFreshContext() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(CancelBlockUseCase.class)
                              .cancel(
                                  "owner", PROJECT, TASK, UUID.randomUUID(), UUID.randomUUID(), 1))
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error ->
                          assertThat(((PortReached) error).port).isEqualTo(BlockEditing.class));
            });
  }

  @Test
  void today_s1_readingBeanReachesItsPortInFreshContext() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(() -> context.getBean(ReadTodayUseCase.class).get("owner"))
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error ->
                          assertThat(((PortReached) error).port).isEqualTo(TodayQueries.class));
            });
  }

  @Test
  void s26_blockReadingBeanReachesItsPortInFreshContext() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ReadBlocksUseCase.class)
                              .list("owner", PROJECT, TASK, null))
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error ->
                          assertThat(((PortReached) error).port).isEqualTo(BlockQueries.class));
            });
  }

  @Test
  void s1_blockPlanningBeanReachesItsPortInFreshContext() {
    var request =
        new BlockRequest(
            "Meta",
            LocalDateTime.of(2030, 1, 7, 10, 0),
            LocalDateTime.of(2030, 1, 7, 11, 0),
            "UTC",
            null,
            null,
            false);
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(PlanBlockUseCase.class)
                              .preview("owner", PROJECT, TASK, request))
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error ->
                          assertThat(((PortReached) error).port).isEqualTo(BlockPlanning.class));
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"projects", "tasks", "subtasks", "taskStatus", "taskHistory", "availability"})
  void sharedReadingBeansReachTheirPortsInFreshContexts(String operation) {
    var expectedPort =
        switch (operation) {
          case "projects" -> ProjectQueries.class;
          case "tasks" -> TaskQueries.class;
          case "subtasks" -> SubtaskQueries.class;
          case "taskStatus" -> TaskStatusQueries.class;
          case "taskHistory" -> TaskHistoryQueries.class;
          case "availability" -> AvailabilityQueries.class;
          default -> throw new AssertionError(operation);
        };
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () -> {
                        switch (operation) {
                          case "projects" ->
                              context.getBean(ReadProjectsUseCase.class).list("owner", null);
                          case "tasks" ->
                              context.getBean(ReadTasksUseCase.class).list("owner", PROJECT, null);
                          case "subtasks" ->
                              context
                                  .getBean(ReadSubtasksUseCase.class)
                                  .list("owner", PROJECT, TASK, null);
                          case "taskStatus" ->
                              context
                                  .getBean(ReadTaskStatusUseCase.class)
                                  .status("owner", PROJECT, TASK);
                          case "taskHistory" ->
                              context
                                  .getBean(ReadTaskHistoryUseCase.class)
                                  .list("owner", PROJECT, TASK, null);
                          case "availability" ->
                              context.getBean(ReadAvailabilityUseCase.class).get("owner");
                          default -> throw new AssertionError(operation);
                        }
                      })
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error -> assertThat(((PortReached) error).port).isEqualTo(expectedPort));
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "createProject",
        "createTask",
        "createSubtask",
        "editProject",
        "changeTaskStatus",
        "saveAvailability"
      })
  void sharedWritingBeansReachTheirPortsInFreshContexts(String operation) {
    var expectedPort =
        switch (operation) {
          case "createProject" -> ProjectCommit.class;
          case "createTask" -> TaskCommit.class;
          case "createSubtask" -> SubtaskCommit.class;
          case "editProject" -> ProjectEditing.class;
          case "changeTaskStatus" -> TaskStatusEditing.class;
          case "saveAvailability" -> AvailabilityEditing.class;
          default -> throw new AssertionError(operation);
        };
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () -> {
                        switch (operation) {
                          case "createProject" ->
                              context
                                  .getBean(CreateProjectUseCase.class)
                                  .execute("owner", "Meta", "");
                          case "createTask" ->
                              context
                                  .getBean(CreateTaskUseCase.class)
                                  .execute("owner", PROJECT, "Tarea", "Hecha", 60);
                          case "createSubtask" ->
                              context
                                  .getBean(CreateSubtaskUseCase.class)
                                  .execute("owner", PROJECT, TASK, "Subtarea", "Hecha", 30);
                          case "editProject" ->
                              context
                                  .getBean(EditProjectUseCase.class)
                                  .execute(
                                      "owner",
                                      PROJECT,
                                      new ProjectRevision(PROJECT, 0),
                                      "Actualizado",
                                      "");
                          case "changeTaskStatus" ->
                              context
                                  .getBean(ChangeTaskStatusUseCase.class)
                                  .execute(
                                      "owner",
                                      PROJECT,
                                      TASK,
                                      new TaskRevision(TASK, 0),
                                      "completed");
                          case "saveAvailability" -> {
                            var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
                            for (var day : DayOfWeek.values()) days.put(day, 120);
                            context
                                .getBean(SaveAvailabilityUseCase.class)
                                .execute("owner", new AvailabilityRevision(null, 0), "UTC", days);
                          }
                          default -> throw new AssertionError(operation);
                        }
                      })
                  .isInstanceOf(PortReached.class)
                  .satisfies(
                      error -> assertThat(((PortReached) error).port).isEqualTo(expectedPort));
            });
  }

  @Test
  void history_s1_readBeanUsesTheRealQueries() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThatThrownBy(
                      () ->
                          context
                              .getBean(ReadHistoryUseCase.class)
                              .list(
                                  "owner",
                                  new HistoryFilters(null, PROJECT, TASK, null, null),
                                  null))
                  .isInstanceOf(ResourceNotFoundException.class);
            });
  }

  @Test
  void appearance_s1_s2_realReadAndSaveBeansReachTheSamePostgresAdapter() {
    freshContext()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(AppearanceQueries.class))
                  .isSameAs(context.getBean(AppearanceEditing.class))
                  .isInstanceOf(
                      com.apptolast.organization.adapter.persistence.PostgresAppearanceStore.class);
              assertThat(context.getBean(ReadAppearanceUseCase.class).get("owner")).isEmpty();
              org.mockito.Mockito.when(
                      context
                          .getBean(org.springframework.jdbc.core.JdbcTemplate.class)
                          .update(
                              org.mockito.ArgumentMatchers.anyString(),
                              org.mockito.ArgumentMatchers.any(Object[].class)))
                  .thenReturn(1);
              var result =
                  context
                      .getBean(SaveAppearanceUseCase.class)
                      .execute(
                          "owner",
                          new com.apptolast.organization.domain.AppearanceRevision(null, 0),
                          "DARK",
                          "#0000ff",
                          "#00ffff");
              assertThat(result.theme()).isEqualTo("DARK");
              assertThat(result.accentLight()).isEqualTo("#0000FF");
              assertThat(result.version()).isZero();
            });
  }
}
