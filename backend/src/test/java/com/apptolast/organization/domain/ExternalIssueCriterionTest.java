package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExternalIssueCriterionTest {
  private static final String URL = "https://github.com/octocat/Hello-World/issues/7";

  private static String criterionOf(String body) {
    return new ExternalIssue("101", "Arreglar login", body, URL).taskCompletionCriterion();
  }

  @Test
  void s15_absentBodyLeavesOnlyTheIssueUrl() {
    assertEquals(URL, criterionOf(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "  \n\r\n\t"})
  void s15_blankBodyIsIndistinguishableFromAbsentBody(String blank) {
    assertEquals(URL, criterionOf(blank));
  }

  @Test
  void s15_bodyFollowsTheUrlAfterOneBlankLineAndLosesEveryCarriageReturn() {
    var criterion = criterionOf("línea1\r\nlínea2");
    assertEquals(URL + "\n\nlínea1\nlínea2", criterion);
    assertFalse(criterion.contains("\r"));
  }

  @Test
  void s15_onlyTheFirstTwentyLinesOfTheBodySurvive() {
    var body = IntStream.rangeClosed(1, 25).mapToObj(n -> "L" + n).reduce((a, b) -> a + "\n" + b);
    var expected =
        IntStream.rangeClosed(1, 20).mapToObj(n -> "L" + n).reduce((a, b) -> a + "\n" + b);
    var criterion = criterionOf(body.orElseThrow());
    assertEquals(URL + "\n\n" + expected.orElseThrow(), criterion);
    assertFalse(criterion.contains("L21"));
  }

  @Test
  void s15_aLongSingleLineIsCutToTwoThousandCodePointsEndingInEllipsis() {
    var criterion = criterionOf("a".repeat(2100));
    assertEquals(2000, criterion.codePointCount(0, criterion.length()));
    assertTrue(criterion.startsWith(URL + "\n\n"));
    assertTrue(criterion.endsWith("…"));
    assertEquals(1999, criterion.substring(0, criterion.length() - 1).length());
  }

  @Test
  void s15_theCutCountsCodePointsAndNeverSplitsASurrogatePair() {
    var criterion = criterionOf("🚀".repeat(2100));
    assertEquals(2000, criterion.codePointCount(0, criterion.length()));
    assertFalse(Character.isHighSurrogate(criterion.charAt(criterion.length() - 2)));
  }
}
