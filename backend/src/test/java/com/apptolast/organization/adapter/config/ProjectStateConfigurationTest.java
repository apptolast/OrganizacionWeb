package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProjectStateConfigurationTest {
  @ParameterizedTest
  @CsvSource({"absent,3", "1,1", "10,10", "0,0", "11,0", "abc,0", "1.5,0"})
  void s7_capacityIsValidatedAtStartup(String value, int expected) {
    var original =
        Project.create(java.util.UUID.randomUUID(), "owner", "Name", "", java.time.Instant.EPOCH);
    ProjectStatusEditing store =
        (owner, id, operation) -> operation.apply(new ProjectSnapshot(original, 0), 20L).snapshot();
    var runner =
        new ApplicationContextRunner()
            .withUserConfiguration(ApplicationConfiguration.class)
            .withBean(BlockEditing.class, () -> org.mockito.Mockito.mock(BlockEditing.class))
            .withBean(TodayQueries.class, () -> org.mockito.Mockito.mock(TodayQueries.class))
            .withBean(BlockQueries.class, () -> org.mockito.Mockito.mock(BlockQueries.class))
            .withBean(BlockPlanning.class, () -> org.mockito.Mockito.mock(BlockPlanning.class))
            .withBean(BlockCommit.class, () -> org.mockito.Mockito.mock(BlockCommit.class))
            .withBean(
                AvailabilityQueries.class,
                () -> org.mockito.Mockito.mock(AvailabilityQueries.class))
            .withBean(
                AvailabilityEditing.class,
                () -> org.mockito.Mockito.mock(AvailabilityEditing.class))
            .withBean(ZoneCatalog.class, () -> org.mockito.Mockito.mock(ZoneCatalog.class))
            .withBean(ProjectCommit.class, () -> org.mockito.Mockito.mock(ProjectCommit.class))
            .withBean(ProjectQueries.class, () -> org.mockito.Mockito.mock(ProjectQueries.class))
            .withBean(ProjectEditing.class, () -> org.mockito.Mockito.mock(ProjectEditing.class))
            .withBean(TaskCommit.class, () -> org.mockito.Mockito.mock(TaskCommit.class))
            .withBean(SubtaskCommit.class, () -> org.mockito.Mockito.mock(SubtaskCommit.class))
            .withBean(SubtaskQueries.class, () -> org.mockito.Mockito.mock(SubtaskQueries.class))
            .withBean(TaskQueries.class, () -> org.mockito.Mockito.mock(TaskQueries.class))
            .withBean(
                TaskStatusQueries.class, () -> org.mockito.Mockito.mock(TaskStatusQueries.class))
            .withBean(
                TaskHistoryQueries.class, () -> org.mockito.Mockito.mock(TaskHistoryQueries.class))
            .withBean(
                TaskStatusEditing.class, () -> org.mockito.Mockito.mock(TaskStatusEditing.class))
            .withBean(ProjectStatusEditing.class, () -> store);
    if (!value.equals("absent"))
      runner = runner.withPropertyValues("app.max-active-projects=" + value);
    runner.run(
        context -> {
          if (expected == 0) {
            assertThat(context).hasFailed();
          } else {
            assertThat(context).hasNotFailed();
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () ->
                        context
                            .getBean(ChangeProjectStatusUseCase.class)
                            .execute(
                                "owner",
                                original.id(),
                                new ProjectRevision(original.id(), 0),
                                "active"))
                .isInstanceOf(ActiveProjectLimitException.class)
                .satisfies(
                    error ->
                        assertThat(((ActiveProjectLimitException) error).limit())
                            .isEqualTo(expected));
          }
        });
  }
}
