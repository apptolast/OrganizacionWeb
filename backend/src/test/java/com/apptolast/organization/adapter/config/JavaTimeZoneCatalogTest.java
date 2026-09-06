package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;

class JavaTimeZoneCatalogTest {
  @org.junit.jupiter.api.Test
  void s2_usesBackendIdsWithoutAliasFilters() {
    var expected = new java.util.HashSet<>(java.time.ZoneId.getAvailableZoneIds());
    expected.add("UTC");
    assertThat(new JavaTimeZoneCatalog().zones()).containsExactlyInAnyOrderElementsOf(expected);
  }
}
