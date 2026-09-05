package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;
import org.junit.jupiter.api.Test;

class ReadProjectsTest {
  @Test
  void s2_emptyOwnCollectionHasNoContinuation() {
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    org.mockito.Mockito.when(queries.list("owner", null, 21)).thenReturn(List.of());
    var page = new ReadProjects(queries).list("owner", null);
    assertThat(page.items()).isEmpty();
    assertThat(page.next()).isNull();
  }

  @Test
  void s4_pageLimitAndCursorUseLastVisibleItem() {
    var values =
        java.util.stream.IntStream.range(0, 21)
            .mapToObj(
                i ->
                    new com.apptolast.organization.domain.ProjectSummary(
                        UUID.randomUUID(),
                        "Idea " + i,
                        "idea",
                        java.time.Instant.EPOCH.minusSeconds(i),
                        java.time.Instant.EPOCH))
            .toList();
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    var after =
        new com.apptolast.organization.domain.ProjectPosition(
            java.time.Instant.EPOCH.plusSeconds(1), UUID.randomUUID());
    org.mockito.Mockito.when(queries.list("owner", after, 21)).thenReturn(values);
    var page = new ReadProjects(queries).list("owner", after);
    assertThat(page.items()).containsExactlyElementsOf(values.subList(0, 20));
    assertThat(page.next())
        .isEqualTo(
            new com.apptolast.organization.domain.ProjectPosition(
                values.get(19).createdAt(), values.get(19).id()));
    assertThat(page.next().createdAt()).isEqualTo(values.get(19).createdAt());
    assertThat(page.next().id()).isEqualTo(values.get(19).id());
  }

  @Test
  void s9_detailReturnsOriginalOwnedProject() {
    var project =
        com.apptolast.organization.domain.Project.create(
            UUID.randomUUID(), "owner", "Idea", "Description", java.time.Instant.EPOCH);
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    org.mockito.Mockito.when(queries.find("owner", project.id())).thenReturn(Optional.of(project));
    assertThat(new ReadProjects(queries).detail("owner", project.id())).isSameAs(project);
  }

  @Test
  void s10_missingOwnedDetailHasUniformDomainError() {
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    var id = UUID.randomUUID();
    org.mockito.Mockito.when(queries.find("owner", id)).thenReturn(Optional.empty());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> new ReadProjects(queries).detail("owner", id))
        .isInstanceOf(ProjectNotFoundException.class)
        .hasMessage("Proyecto no encontrado");
  }

  @Test
  void s1_readPageIsAnImmutableSnapshot() {
    var summary =
        new com.apptolast.organization.domain.ProjectSummary(
            UUID.randomUUID(), "Idea", "idea", java.time.Instant.EPOCH, java.time.Instant.EPOCH);
    var rows = new ArrayList<com.apptolast.organization.domain.ProjectSummary>();
    rows.add(summary);
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    org.mockito.Mockito.when(queries.list("owner", null, 21)).thenReturn(rows);
    var page = new ReadProjects(queries).list("owner", null);
    rows.clear();
    assertThat(page.items()).containsExactly(summary);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> page.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {1, 20})
  void s1_s5_lastPagePreservesDisplayValuesAndHasNoContinuation(int count) {
    var id = UUID.randomUUID();
    var created = java.time.Instant.EPOCH;
    var updated = created.plusSeconds(1);
    var summary =
        new com.apptolast.organization.domain.ProjectSummary(
            id, "Display 😀", "idea", created, updated);
    var queries = org.mockito.Mockito.mock(ProjectQueries.class);
    org.mockito.Mockito.when(queries.list("owner", null, 21))
        .thenReturn(java.util.Collections.nCopies(count, summary));
    var page = new ReadProjects(queries).list("owner", null);
    assertThat(page.items())
        .hasSize(count)
        .allSatisfy(
            item -> {
              assertThat(item.id()).isEqualTo(id);
              assertThat(item.name()).isEqualTo("Display 😀");
              assertThat(item.status()).isEqualTo("idea");
              assertThat(item.createdAt()).isEqualTo(created);
              assertThat(item.updatedAt()).isEqualTo(updated);
            });
    assertThat(page.next()).isNull();
  }
}
