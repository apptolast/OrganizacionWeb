package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.CustomizationScope;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadCustomizationTest {
  @Test
  void s1_readsAbsenceForOnlyTheAuthenticatedOwnerAndRequestedScope() {
    var read =
        new ReadCustomization(
            (owner, scope) -> {
              assertThat(owner).isEqualTo("owner-a");
              assertThat(scope).isEqualTo(CustomizationScope.PROJECT);
              return Optional.empty();
            });
    assertThat(read.get("owner-a", CustomizationScope.PROJECT)).isEmpty();
  }

  @Test
  void s2_readsTheStoredScopeAndDefinitionOrderWithoutChangingItsRevision() {
    var first =
        new com.apptolast.organization.domain.CustomFieldDefinition(
            java.util.UUID.fromString("d05147e5-ddcc-4eb1-bcbc-f3102349b6f7"),
            "Referencia",
            com.apptolast.organization.domain.CustomFieldType.TEXT,
            true);
    var second =
        new com.apptolast.organization.domain.CustomFieldDefinition(
            java.util.UUID.fromString("a05147e5-ddcc-4eb1-bcbc-f3102349b6f7"),
            "Revisado",
            com.apptolast.organization.domain.CustomFieldType.BOOLEAN,
            false);
    var configuration =
        new com.apptolast.organization.domain.Customization(
            java.util.UUID.fromString("5b5147e5-ddcc-4eb1-bcbc-f3102349b6f7"),
            "owner-b",
            CustomizationScope.TASK,
            java.util.List.of("estimatedMinutes", "updatedAt"),
            java.util.List.of(first, second),
            7,
            java.time.Instant.parse("2026-09-07T20:00:00.123456Z"));
    var expected = Optional.of(configuration);
    var read =
        new ReadCustomization(
            (owner, scope) -> {
              assertThat(owner).isEqualTo("owner-b");
              assertThat(scope).isEqualTo(CustomizationScope.TASK);
              return expected;
            });
    assertThat(read.get("owner-b", CustomizationScope.TASK)).isSameAs(expected);
    assertThat(configuration.visibleFields()).containsExactly("estimatedMinutes", "updatedAt");
    assertThat(configuration.customFields()).containsExactly(first, second);
    assertThat(configuration.version()).isEqualTo(7);
    assertThat(configuration.updatedAt()).isEqualTo("2026-09-07T20:00:00.123456Z");
  }
}
