package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ExternalCalendarInputTest {
  static final String FEED = "https://feed.example.test/a.ics";

  static FieldError error(String label, String url) {
    var thrown =
        assertThrows(ValidationException.class, () -> ExternalCalendarInput.of(label, url));
    assertEquals(1, thrown.errors().size());
    return thrown.errors().getFirst();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      nullValues = "NULL",
      value = {
        "NULL,label,REQUIRED",
        "'   ',label,REQUIRED",
        "'\u00a0\u2003',label,REQUIRED",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa,label,TOO_LONG"
      })
  void s4_rejectsLabels(String label, String field, String code) {
    var error = error(label, FEED);
    assertEquals(field, error.field());
    assertEquals(code, error.code());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      nullValues = "NULL",
      value = {
        "NULL,REQUIRED",
        "'',REQUIRED",
        "feed.example.test/a.ics,INVALID_FORMAT",
        "no es una url,INVALID_FORMAT",
        "https://,INVALID_FORMAT",
        "http://feed.example.test/a.ics,INVALID_VALUE",
        "https://user:pass@feed.example.test/a.ics,INVALID_VALUE",
        "https://feed.example.test/a.ics#frag,INVALID_VALUE",
        "https://127.0.0.1/a.ics,INVALID_VALUE",
        "https://[::1]/a.ics,INVALID_VALUE",
        "https://169.254.169.254/latest/meta-data,INVALID_VALUE",
        "https://10.0.0.5/a.ics,INVALID_VALUE"
      })
  void s4_rejectsUrls(String url, String code) {
    var error = error("Trabajo", url);
    assertEquals("url", error.field());
    assertEquals(code, error.code());
  }

  @Test
  void s4_rejectsUrlsLongerThan2048Characters() {
    var prefix = "https://feed.example.test/";
    var tooLong = prefix + "a".repeat(2049 - prefix.length());
    assertEquals(2049, tooLong.length());
    assertEquals("TOO_LONG", error("Trabajo", tooLong).code());
  }

  @Test
  void s5_acceptsAUrlOfExactly2048Characters() {
    var prefix = "https://feed.example.test/";
    var boundary = prefix + "a".repeat(2048 - prefix.length() - 4) + "WXYZ";
    assertEquals(2048, boundary.length());
    assertEquals("WXYZ", ExternalCalendarInput.of("Trabajo", boundary).urlTail());
  }

  @Test
  void s4_reportsBothFieldsAtOnce() {
    var thrown =
        assertThrows(ValidationException.class, () -> ExternalCalendarInput.of(" ", "nope"));
    assertEquals(
        java.util.List.of("label", "url"),
        thrown.errors().stream().map(FieldError::field).toList());
  }

  @Test
  void s5_acceptsBoundariesAndExposesHostAndTail() {
    var emoji = "😀".repeat(40);
    assertEquals(emoji, ExternalCalendarInput.of(emoji, FEED).label());
    var trimmed = ExternalCalendarInput.of("  Casa  ", FEED);
    assertEquals("Casa", trimmed.label());
    assertEquals("feed.example.test", trimmed.urlHost());
    assertEquals(".ics", trimmed.urlTail());
    var query = ExternalCalendarInput.of("Trabajo", "https://feed.example.test/x?k=v&t=WXYZ");
    assertEquals("WXYZ", query.urlTail());
    assertEquals("https://feed.example.test/x?k=v&t=WXYZ", query.url());
  }
}
