package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SecurityConfigurationTest {
  @ParameterizedTest
  @CsvSource({"'',secret", "'   ',secret", "user,''", "user,'   '"})
  void bootstrapRejectsBlankCredentials(String username, String password) {
    assertThatThrownBy(() -> new SecurityConfiguration().users(username, password))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
