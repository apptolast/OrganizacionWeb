package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.Project;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CreateProjectTest {
  @Test
  void s17_doesNotConfirmWhenAtomicCommitFails() {
    RuntimeException cause = new RuntimeException("storage offline");
    StorageUnavailableException failure = new StorageUnavailableException(cause);
    CreateProject useCase =
        new CreateProject(
            (project, event) -> {
              throw failure;
            },
            Clock.systemUTC());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> useCase.execute("persona-a", "Idea", ""))
        .isSameAs(failure)
        .hasCause(cause)
        .hasMessage("El almacenamiento no está disponible.");
  }

  @Test
  void confirmsOnlyTimestampPrecisionSupportedByStorage() {
    Instant now = Instant.parse("2026-09-05T12:00:00.123456789Z");
    CreateProject useCase =
        new CreateProject(
            (project, event) -> assertThat(event.occurredAt()).isEqualTo(project.createdAt()),
            Clock.fixed(now, ZoneOffset.UTC));
    Project project = useCase.execute("persona-a", "Idea", "");
    assertThat(project.createdAt()).isEqualTo(Instant.parse("2026-09-05T12:00:00.123456Z"));
    assertThat(project.updatedAt()).isEqualTo(project.createdAt());
  }

  @Test
  void s1_s16_commitsProjectAndVersionedPrivateEventTogether() {
    Instant now = Instant.parse("2026-09-05T12:00:00Z");
    AtomicReference<Project> saved = new AtomicReference<>();
    AtomicReference<ProjectCreated> event = new AtomicReference<>();
    CreateProject useCase =
        new CreateProject(
            (project, created) -> {
              saved.set(project);
              event.set(created);
            },
            Clock.fixed(now, ZoneOffset.UTC));
    Project result = useCase.execute("persona-a", "  Idea  ", "Contenido privado");
    assertThat(saved.get()).isEqualTo(result);
    assertThat(result.id()).isNotNull();
    assertThat(result.ownerId()).isEqualTo("persona-a");
    assertThat(result.createdAt()).isEqualTo(now);
    assertThat(event.get().eventId()).isNotNull().isNotEqualTo(result.id());
    assertThat(event.get().aggregateId()).isEqualTo(result.id());
    assertThat(event.get().ownerId()).isEqualTo("persona-a");
    assertThat(event.get().occurredAt()).isEqualTo(now);
    assertThat(event.get().schemaVersion()).isEqualTo(1);
    assertThat(event.get().name()).isEqualTo("Idea");
    assertThat(event.get().type()).isEqualTo("ProjectCreated.v1");
    assertThat(event.get().toString()).doesNotContain("Contenido privado");
  }
}
