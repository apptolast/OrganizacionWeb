package com.apptolast.organization.adapter.connectors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * B11 de progress/security_review_connectors.md: el PAT del usuario viaja como Authorization Bearer
 * a esta base, así que sólo puede apuntar a GitHub o al servidor falso de las pruebas.
 */
class GithubApiBaseTest {
  @ParameterizedTest
  @CsvSource({
    "https://api.github.com,https://api.github.com",
    "https://api.github.com/,https://api.github.com",
    "http://127.0.0.1:18093,http://127.0.0.1:18093",
    "http://127.0.0.1:18093/,http://127.0.0.1:18093",
    "http://localhost:9999,http://localhost:9999",
    "http://[::1]:9999,http://[::1]:9999",
    "https://127.0.0.1:9999,https://127.0.0.1:9999"
  })
  void s35_b11_acceptsGithubAndLoopbackOnlyAndDropsTheTrailingSlash(String raw, String expected) {
    assertEquals(expected, GithubApiBase.of(raw).value());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://api.github.com",
        "https://api.github.com.atacante.test",
        "https://atacante.test",
        "https://api.github.com:8443",
        "https://api.github.com/enterprise",
        "http://10.0.0.1",
        "http://169.254.169.254",
        "https://evil.test/?x=https://api.github.com",
        "ftp://api.github.com",
        "//api.github.com",
        "api.github.com",
        "",
        "   "
      })
  void s35_b11_anyOtherBaseStopsTheStartupNamingTheProperty(String raw) {
    var error = assertThrows(IllegalArgumentException.class, () -> GithubApiBase.of(raw));
    assertTrue(error.getMessage().contains("app.github.api-base"), error.getMessage());
  }

  @Test
  void s35_b11_anAbsentBaseStopsTheStartupToo() {
    assertThrows(IllegalArgumentException.class, () -> GithubApiBase.of(null));
  }

  @Test
  void s35_b11_theBaseBuildsRepositoryAndUserPathsWithoutFollowingUserInput() {
    var base = GithubApiBase.of("http://127.0.0.1:18093");
    assertEquals(
        "http://127.0.0.1:18093/repos/octocat/Hello-World", base.repository("octocat/Hello-World"));
    assertEquals("http://127.0.0.1:18093/user", base.user());
    assertEquals(
        "http://127.0.0.1:18093/repos/octocat/Hello-World/issues?state=open&per_page=100&sort=created&direction=desc&page=2",
        base.issues("octocat/Hello-World", 2));
  }
}
