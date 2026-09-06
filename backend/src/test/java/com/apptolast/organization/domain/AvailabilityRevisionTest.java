package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityRevisionTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"true,-1", "false,-1", "false,1"})
  void cannotRepresentAnInvalidPrecondition(boolean configured, long version) {
    assertThatThrownBy(
            () ->
                new AvailabilityRevision(configured ? java.util.UUID.randomUUID() : null, version))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
