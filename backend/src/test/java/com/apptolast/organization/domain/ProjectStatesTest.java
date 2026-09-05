package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class ProjectStatesTest {
  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"unknown", "ACTIVE"})
  void unrecognizedStatesCannotAuthorizeATransition(String value) {
    assertThat(ProjectStates.valid(value)).isFalse();
    assertThat(ProjectStates.allows(value, "active")).isFalse();
    assertThat(ProjectStates.allows("idea", value)).isFalse();
  }
}
