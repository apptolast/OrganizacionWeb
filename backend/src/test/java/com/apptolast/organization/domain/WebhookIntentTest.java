package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookIntentTest {
  private static final String URL = "https://example.com/hooks";
  private static final List<String> CATALOG =
      List.of(
          "ProjectCreated.v1",
          "ProjectUpdated.v1",
          "ProjectStatusChanged.v1",
          "TaskCreated.v1",
          "SubtaskCreated.v1",
          "TaskStatusChanged.v1",
          "BlockPlanned.v1",
          "BlockChanged.v1",
          "WorkSessionStarted.v1",
          "WorkSessionStateChanged.v1",
          "WorkSessionExtended.v1",
          "WorkSessionClosed.v1");

  @Test
  void s2_httpsAbsoluteUrlWithOptionalPortIsAccepted() {
    var intent =
        new WebhookIntent("https://example.com:8443/hooks?x=1", "", List.of("TaskCreated.v1"));
    assertEquals("https://example.com:8443/hooks?x=1", intent.url());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://example.com/hooks",
        "HTTPS://example.com/hooks",
        "https://user:pw@example.com/hooks",
        "https://example.com/hooks#frag",
        "https:///hooks",
        "https://example.com:0/hooks",
        "https://example.com:65536/hooks",
        "example.com/hooks",
        ""
      })
  void s2_urlMustBeHttpsAbsoluteWithHostWithoutCredentialsOrFragment(String url) {
    var error =
        assertThrows(
            WebhookInvalidException.class,
            () -> new WebhookIntent(url, "", List.of("TaskCreated.v1")));
    assertEquals(List.of("url"), error.errors().stream().map(FieldError::field).toList());
  }

  @Test
  void s2_urlLongerThan2048CodePointsIsRejectedAndNullToo() {
    var longUrl = "https://example.com/" + "a".repeat(2049 - "https://example.com/".length());
    assertEquals(2049, longUrl.codePointCount(0, longUrl.length()));
    assertThrows(
        WebhookInvalidException.class,
        () -> new WebhookIntent(longUrl, "", List.of("TaskCreated.v1")));
    assertThrows(
        WebhookInvalidException.class,
        () -> new WebhookIntent(null, "", List.of("TaskCreated.v1")));
    var limit = "https://example.com/" + "a".repeat(2048 - "https://example.com/".length());
    assertEquals(limit, new WebhookIntent(limit, "", List.of("TaskCreated.v1")).url());
  }

  @Test
  void s3_descriptionIsOptionalStrippedAndKeepsEightyCodePoints() {
    assertEquals("", new WebhookIntent(URL, null, List.of("ProjectCreated.v1")).description());
    assertEquals(
        "Mi hook",
        new WebhookIntent(URL, "  Mi hook  ", List.of("ProjectCreated.v1")).description());
    var eighty = "😀".repeat(80);
    assertEquals(
        eighty, new WebhookIntent(URL, eighty, List.of("ProjectCreated.v1")).description());
  }

  @Test
  void s3_descriptionRejectsEightyOneCodePointsAndControlCharacters() {
    for (var description : List.of("😀".repeat(81), "a\u0001b")) {
      var error =
          assertThrows(
              WebhookInvalidException.class,
              () -> new WebhookIntent(URL, description, List.of("ProjectCreated.v1")));
      assertEquals(List.of("description"), error.errors().stream().map(FieldError::field).toList());
    }
  }

  @Test
  void s3_eventTypesAreCanonicalisedToCatalogOrder() {
    assertEquals(
        List.of("TaskCreated.v1", "TaskStatusChanged.v1"),
        new WebhookIntent(URL, "", List.of("TaskStatusChanged.v1", "TaskCreated.v1")).eventTypes());
    var reversed = new ArrayList<>(CATALOG);
    java.util.Collections.reverse(reversed);
    assertEquals(CATALOG, new WebhookIntent(URL, "", reversed).eventTypes());
    assertEquals(CATALOG, WebhookIntent.CATALOG);
  }

  @Test
  void s3_eventTypesRejectEmptyUnknownDuplicatedOrWrongCaseNames() {
    var thirteen = new ArrayList<>(CATALOG);
    thirteen.add("ProjectCreated.v2");
    for (var types :
        List.<List<String>>of(
            List.of(),
            List.of("webhook.ping.v1"),
            List.of("TaskCreated.v1", "TaskCreated.v1"),
            List.of("taskcreated.v1"),
            thirteen)) {
      var error =
          assertThrows(WebhookInvalidException.class, () -> new WebhookIntent(URL, "", types));
      assertEquals(List.of("eventTypes"), error.errors().stream().map(FieldError::field).toList());
    }
    assertThrows(WebhookInvalidException.class, () -> new WebhookIntent(URL, "", null));
  }

  @Test
  void s3_everyInvalidFieldIsReportedTogether() {
    var error =
        assertThrows(
            WebhookInvalidException.class,
            () -> new WebhookIntent("https://example.com/h", "x".repeat(81), List.of()));
    assertEquals(
        List.of("description", "eventTypes"),
        error.errors().stream().map(FieldError::field).toList());
  }
}
