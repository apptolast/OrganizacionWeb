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
            .withBean(ZoneCatalog.class, () -> () -> Set.of("UTC"));
    for (var port :
        List.of(
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
            AvailabilityEditing.class)) {
      runner = addPort(runner, port);
    }
    return runner;
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
}
