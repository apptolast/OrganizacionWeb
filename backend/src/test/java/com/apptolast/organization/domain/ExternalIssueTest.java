package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExternalIssueTest {
  private static final String URL = "https://github.com/octocat/Hello-World/issues/7";

  static Stream<Arguments> titles() {
    String rocket = "🚀";
    return Stream.of(
        Arguments.of("a".repeat(159), "a".repeat(159)),
        Arguments.of("a".repeat(160), "a".repeat(160)),
        Arguments.of("a".repeat(161), "a".repeat(159) + "…"),
        Arguments.of(rocket.repeat(161), rocket.repeat(159) + "…"),
        Arguments.of("  Arreglar login ", "Arreglar login"),
        Arguments.of("   ", ""),
        Arguments.of(null, ""),
        Arguments.of("b".repeat(155) + "      ", "b".repeat(155)));
  }

  @ParameterizedTest
  @MethodSource("titles")
  void s14_titleIsTrimmedThenCutToOneHundredSixtyCodePointsWithEllipsis(
      String raw, String expected) {
    var title = new ExternalIssue("101", raw, null, URL).taskTitle();
    assertEquals(expected, title);
    assertTrue(title.codePointCount(0, title.length()) <= 160);
  }
}
