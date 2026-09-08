package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GithubRepositoryTest {
  @ParameterizedTest
  @CsvSource({
    "' octocat/Hello-World\u00a0',octocat/Hello-World",
    "'\u2003OCTOCAT/hello.world_x-1',OCTOCAT/hello.world_x-1"
  })
  void s5_trimsUnicodeWhiteSpaceAndKeepsCase(String raw, String expected) {
    assertEquals(expected, GithubRepository.parse(raw).fullName());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "octocat",
        "-octocat/repo",
        "octocat-/repo",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/repo",
        "octocat/mi repo",
        "https://github.com/octocat/repo",
        "atacante.test/x/y",
        "octocat/"
      })
  void s5_s35_rejectsInvalidFormat(String raw) {
    var error = assertThrows(ValidationException.class, () -> GithubRepository.parse(raw));
    assertEquals(1, error.errors().size());
    assertEquals("repository", error.errors().getFirst().field());
    assertEquals("INVALID_FORMAT", error.errors().getFirst().code());
  }

  @org.junit.jupiter.api.Test
  void s5_rejectsRepositoryNameLongerThanHundred() {
    assertThrows(
        ValidationException.class, () -> GithubRepository.parse("octocat/" + "a".repeat(101)));
    assertEquals(
        "octocat/" + "a".repeat(100),
        GithubRepository.parse("octocat/" + "a".repeat(100)).fullName());
  }

  @org.junit.jupiter.api.Test
  void s5_absentRepositoryIsRequired() {
    var error = assertThrows(ValidationException.class, () -> GithubRepository.parse(null));
    assertEquals("REQUIRED", error.errors().getFirst().code());
    assertEquals("repository", error.errors().getFirst().field());
  }
}
