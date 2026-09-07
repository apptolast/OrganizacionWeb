package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.HistoryController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = HistoryController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class HistoryApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean ReadHistoryUseCase history;

  private final UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private final UUID project = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private final UUID task = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private final Instant now = Instant.parse("2026-09-07T10:00:00.123456Z");

  private PlannedBlock block() {
    var start = LocalDateTime.parse("2026-09-08T10:00");
    return new PlannedBlock(
        id,
        project,
        task,
        new BlockRequest(
            "Objetivo original",
            start,
            start.plusMinutes(25),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            true),
        new ResolvedBlockTime(
            start.toInstant(ZoneOffset.UTC),
            start.plusMinutes(25).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            25),
        now);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"", "Sessions", "other"})
  void s12_categoryMustBeOneOfTheExactPublicValues(String value) throws Exception {
    mvc.perform(get("/api/v1/history").with(user("owner")).param("category", value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("category"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"", "2026-02-30", "0000-01-01", "10000-01-01", "+10000-01-01", "2026-9-07"})
  void s12_dateRequiresARealFourDigitYearInPublicRange(String value) throws Exception {
    mvc.perform(get("/api/v1/history").with(user("owner")).param("from", value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("from"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Autowired com.fasterxml.jackson.databind.ObjectMapper json;

  private HistoryFilters emptyFilters() {
    return new HistoryFilters(null, null, null, null, null);
  }

  private HistoryCursor cursor() {
    return new HistoryCursor(
        "owner",
        emptyFilters(),
        new HistoryPosition(now, "SESSION_STARTED", id),
        new HistoryPosition(now.minusSeconds(19), "SESSION_STARTED", id));
  }

  private String token(HistoryCursor cursor) throws Exception {
    var body = json.<com.fasterxml.jackson.databind.node.ObjectNode>valueToTree(cursor);
    body.put("version", 1);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(body));
  }

  private String rawToken(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"extra", "owner", "filters", "upper", "after", "version"})
  void s15_cursorEnvelopeRequiresExactlyItsFiveFields(String field) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    if (field.equals("extra")) body.put("extra", true);
    else body.remove(field);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"2", "null", "\"1\"", "1.0", "true"})
  void s15_cursorVersionIsIntegerOneWithoutCoercion(String value) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    body.set("version", json.readTree(value));
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"filters,to", "upper,id", "after,type"})
  void s15_nestedCursorObjectsRequireAllTheirFields(String object, String field) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get(object)).remove(field);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "owner",
        "upper.type",
        "upper.id",
        "upper.occurredAt",
        "filters.category",
        "filters.projectId",
        "filters.taskId",
        "filters.from",
        "filters.to"
      })
  void s15_cursorTextFieldsNeverCoerceNumbers(String path) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    var parts = path.split("\\.");
    var parent =
        parts.length == 1
            ? body
            : (com.fasterxml.jackson.databind.node.ObjectNode) body.get(parts[0]);
    parent.put(parts[parts.length - 1], 42);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "0000-01-01T00:00:00Z",
        "+10000-01-01T00:00:00Z",
        "2026-09-07T10:00:00.123456789Z",
        "2026-09-07T10:00:00+00:00"
      })
  void s15_cursorTimeRequiresUtcMicrosecondsAndPublicYears(String value) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("upper")).put("occurredAt", value);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"1-1-1-1-1", "AAAAAAAAAAAAAAAAAAAAAA==", "ABCDEFAB-CDEF-4ABC-8DEF-ABCDEFABCDEF"})
  void s15_cursorUuidUsesCanonicalLowercaseText(String value) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("after")).put("id", value);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"projectId", "taskId"})
  void s15_cursorContextIdsCannotUseAlternateUuidEncoding(String field) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("filters"))
        .put(field, "AAAAAAAAAAAAAAAAAAAAAA==");
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"from", "to"})
  void s15_cursorFilterDatesCannotEscapeThePublicYearRange(String field) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("filters")).put(field, "0000-01-01");
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"owner", "filters", "boundaries"})
  void s16_cursorBindingIsDeferredUntilApplicationChecksOwnership(String defect) throws Exception {
    var filters = new HistoryFilters(null, project, null, null, null);
    var cursor = new HistoryCursor("owner", filters, cursor().upper(), cursor().after());
    cursor =
        switch (defect) {
          case "owner" ->
              new HistoryCursor("another-owner", filters, cursor.upper(), cursor.after());
          case "filters" ->
              new HistoryCursor(
                  "owner",
                  new HistoryFilters("planning", project, null, null, null),
                  cursor.upper(),
                  cursor.after());
          default -> new HistoryCursor("owner", filters, cursor.after(), cursor.upper());
        };
    when(history.list("owner", filters, cursor)).thenThrow(new ResourceNotFoundException());
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("projectId", project.toString())
                .param("cursor", token(cursor)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    verify(history).list("owner", filters, cursor);
    verifyNoMoreInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"2026-09-07T24:00:00Z", "2026-09-07T23:59:60Z", "2026-09-07T10:00:00.123456000Z"})
  void s15_cursorTimeDoesNotNormalizeInvalidLexicalPrecision(String value) throws Exception {
    var body =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(Base64.getUrlDecoder().decode(token(cursor())));
    ((com.fasterxml.jackson.databind.node.ObjectNode) body.get("after")).put("occurredAt", value);
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("cursor", rawToken(body.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"planning", "task-status"})
  void s14_cursorPreservesNonNullFiltersAndExtremeUtcBoundaries(String category) throws Exception {
    var project = UUID.fromString("abcdefab-cdef-4abc-8def-abcdefabcdef");
    var filters =
        new HistoryFilters(
            category, project, task, LocalDate.of(2024, 2, 29), LocalDate.of(2024, 2, 29));
    var cursor =
        new HistoryCursor(
            "owner",
            filters,
            new HistoryPosition(Instant.parse("9999-12-31T23:59:59.999999Z"), "BLOCK_CHANGED", id),
            new HistoryPosition(Instant.parse("0001-01-01T00:00:00Z"), "BLOCK_PLANNED", id));
    when(history.list("owner", filters, cursor)).thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("category", category)
                .param("projectId", project.toString().toUpperCase(Locale.ROOT))
                .param("taskId", task.toString())
                .param("from", "2024-02-29")
                .param("to", "2024-02-29")
                .param("cursor", token(cursor)))
        .andExpect(status().isOk());
    verify(history).list("owner", filters, cursor);
    verifyNoMoreInteractions(history);
  }

  @Test
  void s17_applicationCursorMismatchPreservesValidationProblem() throws Exception {
    when(history.list("owner", emptyFilters(), cursor()))
        .thenThrow(
            new ValidationException(
                List.of(new FieldError("cursor", "INVALID_VALUE", "Cursor incompatible."))));
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", token(cursor())))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"))
        .andExpect(jsonPath("$.items").doesNotExist());
  }

  @Test
  void s23_storageFailureReturnsNoPartialPageOrPrivateDetails() throws Exception {
    when(history.list("owner", emptyFilters(), null))
        .thenThrow(
            new StorageUnavailableException(
                new IllegalStateException("SELECT private_note FROM private_table")));
    var response =
        mvc.perform(get("/api/v1/history").with(user("owner")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andExpect(jsonPath("$.items").doesNotExist())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    org.assertj.core.api.Assertions.assertThat(response.getContentAsString())
        .doesNotContain("private_note", "private_table", "IllegalStateException");
  }

  @Test
  void s18_authenticationPrecedesInvalidQueryAndCursor() throws Exception {
    mvc.perform(get("/api/v1/history?limit=20&cursor=!"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(history);
  }

  @Test
  void s15_cursorFilterCategoryMustBeKnownBeforeContextLookup() throws Exception {
    var invalid =
        new HistoryCursor(
            "owner",
            new HistoryFilters("Sessions", null, null, null, null),
            cursor().upper(),
            cursor().after());
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", token(invalid)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @Test
  void s15_cursorBoundaryRequiresAKnownFactType() throws Exception {
    var invalid =
        new HistoryCursor(
            "owner",
            emptyFilters(),
            new HistoryPosition(now, "SESSION_CLOSED", id),
            cursor().after());
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", token(invalid)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @Test
  void s15_trailingJsonAfterCursorIsRejected() throws Exception {
    var raw =
        new String(
                Base64.getUrlDecoder().decode(token(cursor())),
                java.nio.charset.StandardCharsets.UTF_8)
            + " {}";
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", rawToken(raw)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @Test
  void s15_duplicateCursorKeyIsRejectedEvenWithTheSameValue() throws Exception {
    var raw =
        new String(
            Base64.getUrlDecoder().decode(token(cursor())),
            java.nio.charset.StandardCharsets.UTF_8);
    raw = raw.replace("\"owner\":\"owner\"", "\"owner\":\"owner\",\"owner\":\"owner\"");
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", rawToken(raw)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @Test
  void s15_nonCanonicalBase64AliasIsRejected() throws Exception {
    var value =
        new String(
            Base64.getUrlDecoder().decode(token(cursor())),
            java.nio.charset.StandardCharsets.UTF_8);
    while (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length % 3 == 0) value += " ";
    var canonical =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    var alias =
        canonical.substring(0, canonical.length() - 1)
            + alphabet.charAt(alphabet.indexOf(canonical.charAt(canonical.length() - 1)) + 1);
    org.assertj.core.api.Assertions.assertThat(Base64.getUrlDecoder().decode(alias))
        .isEqualTo(Base64.getUrlDecoder().decode(canonical));
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", alias))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(history);
  }

  @Test
  void s15_paddedBase64IsRejectedBeforeLookingUpForeignContext() throws Exception {
    var value =
        new String(
            Base64.getUrlDecoder().decode(token(cursor())),
            java.nio.charset.StandardCharsets.UTF_8);
    while (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length % 3 == 0) value += " ";
    var padded =
        Base64.getUrlEncoder()
            .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    when(history.list(anyString(), any(), any())).thenThrow(new ResourceNotFoundException());
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("projectId", project.toString())
                .param("cursor", padded))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cursor"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s14_cursorIsDecodedAndForwardedWithoutReplacingItsBoundaries() throws Exception {
    when(history.list("owner", emptyFilters(), cursor()))
        .thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(get("/api/v1/history").with(user("owner")).param("cursor", token(cursor())))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[],\"nextCursor\":null}", true));
    verify(history).list("owner", emptyFilters(), cursor());
    verifyNoMoreInteractions(history);
  }

  @Test
  void s14_nextCursorHasCanonicalClosedEnvelopeAndBothPageBoundaries() throws Exception {
    List<HistoryEntry<?>> entries = new ArrayList<>();
    for (int index = 0; index < 20; index++) {
      var eventId = new UUID(0, index + 1);
      var time = now.minusSeconds(index);
      var start = new SessionStart(eventId, project, task, time, 25, time.plusSeconds(1500), "UTC");
      entries.add(
          new HistoryEntry<>(
              eventId, "SESSION_STARTED", time, project, "Entrega", task, "Publicar", start));
    }
    var upper = new HistoryPosition(now, "SESSION_STARTED", entries.getFirst().id());
    var after =
        new HistoryPosition(now.minusSeconds(19), "SESSION_STARTED", entries.getLast().id());
    when(history.list("owner", emptyFilters(), null))
        .thenReturn(
            new HistoryPage(entries, new HistoryCursor("owner", emptyFilters(), upper, after)));
    var response =
        mvc.perform(get("/api/v1/history").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(20))
            .andReturn()
            .getResponse();
    var encoded = json.readTree(response.getContentAsString()).get("nextCursor").textValue();
    org.assertj.core.api.Assertions.assertThat(encoded).matches("[A-Za-z0-9_-]+");
    var bytes = Base64.getUrlDecoder().decode(encoded);
    org.assertj.core.api.Assertions.assertThat(
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
        .isEqualTo(encoded);
    var body = json.readTree(bytes);
    org.assertj.core.api.Assertions.assertThat(body)
        .isEqualTo(
            json.readTree(
                """
        {"version":1,"owner":"owner","filters":{"category":null,"projectId":null,"taskId":null,"from":null,"to":null},
        "upper":{"occurredAt":"2026-09-07T10:00:00.123456Z","type":"SESSION_STARTED","id":"00000000-0000-0000-0000-000000000001"},
        "after":{"occurredAt":"2026-09-07T09:59:41.123456Z","type":"SESSION_STARTED","id":"00000000-0000-0000-0000-000000000014"}}
        """));
  }

  @Test
  void s12_invertedDatesAreRejectedOnToBeforeCursor() throws Exception {
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("from", "2026-09-08")
                .param("to", "2026-09-07")
                .param("cursor", "!"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("to"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s12_taskFilterRequiresProjectBeforeCursorParsing() throws Exception {
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("taskId", task.toString())
                .param("cursor", "!"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("taskId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s1_originalSessionStartKeepsSevenFieldsAndMicrosecondsBeforeEpoch() throws Exception {
    var began = Instant.parse("0001-01-01T00:00:00.123456Z");
    var start = new SessionStart(id, project, task, began, 25, began.plusSeconds(1500), "UTC");
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_STARTED", began, project, "Entrega", task, "Publicar", start)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].length()").value(8))
        .andExpect(jsonPath("$.items[0].occurredAt").value(began.toString()))
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.startedAt").value(began.toString()))
        .andExpect(jsonPath("$.items[0].details.plannedEndAt").value("0001-01-01T00:25:00.123456Z"))
        .andExpect(jsonPath("$.items[0].details.plannedMinutes").value(25))
        .andExpect(jsonPath("$.items[0].details.status").doesNotExist());
  }

  @Test
  void s1_extensionKeepsHistoricalEndsWithoutInventingNetTime() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "running", 2, start.startedAt(), 0, start.startedAt());
    var extension =
        new WorkSessionExtension(15, start.plannedEndAt(), start.plannedEndAt().plusSeconds(900));
    var receipt =
        new WorkSessionTransitionReceipt(id, id, "EXTEND", now, before, after, null, extension);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.extension.length()").value(3))
        .andExpect(jsonPath("$.items[0].details.extension.additionalMinutes").value(15))
        .andExpect(
            jsonPath("$.items[0].details.extension.previousEndAt")
                .value(start.plannedEndAt().toString()))
        .andExpect(
            jsonPath("$.items[0].details.extension.effectiveEndAt")
                .value(extension.effectiveEndAt().toString()))
        .andExpect(
            jsonPath("$.items[0].details.after.changedAt").value(start.startedAt().toString()))
        .andExpect(jsonPath("$.items[0].details.after.workedMicroseconds").value("0"))
        .andExpect(jsonPath("$.items[0].details.closure").doesNotExist());
  }

  @Test
  void s1_closeReceiptKeepsHistoricalNotesAndDateWithoutExtension() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "closed", 2, now, 60000000, null);
    var closure =
        new WorkSessionClosure("  avance\n🙂", " próximo ", LocalDate.of(2026, 9, 7), "UTC");
    var receipt = new WorkSessionTransitionReceipt(id, id, "CLOSE", now, before, after, closure);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.closure.length()").value(4))
        .andExpect(jsonPath("$.items[0].details.closure.progressNote").value("  avance\n🙂"))
        .andExpect(jsonPath("$.items[0].details.closure.nextStep").value(" próximo "))
        .andExpect(jsonPath("$.items[0].details.closure.workDate").value("2026-09-07"))
        .andExpect(jsonPath("$.items[0].details.extension").doesNotExist());
  }

  @Test
  void s12_repeatedCategoryDoesNotSilentlyChooseTheFirstValue() throws Exception {
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("category", "sessions", "planning"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("category"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s12_unknownQueryIsRejectedBeforeUnavailableStorage() throws Exception {
    mvc.perform(get("/api/v1/history?limit=20").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s8_filtersNormalizeContextAndPreserveInclusiveExtremeDates() throws Exception {
    var project = UUID.fromString("abcdefab-cdef-4abc-8def-abcdefabcdef");
    var filters =
        new HistoryFilters(
            "sessions", project, task, LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31));
    when(history.list("owner", filters, null)).thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("category", "sessions")
                .param("projectId", project.toString().toUpperCase(java.util.Locale.ROOT))
                .param("taskId", task.toString())
                .param("from", "0001-01-01")
                .param("to", "9999-12-31"))
        .andExpect(status().isOk());
    verify(history).list("owner", filters, null);
    verifyNoMoreInteractions(history);
  }

  @Test
  void s1_pauseReceiptPreservesSixFieldsAndDecimalCounterStrings() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "paused", 2, now, 60000000, null);
    var receipt = new WorkSessionTransitionReceipt(id, id, "PAUSE", now, before, after);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(6))
        .andExpect(jsonPath("$.items[0].details.after.revision").value("2"))
        .andExpect(jsonPath("$.items[0].details.after.workedMicroseconds").value("60000000"))
        .andExpect(
            jsonPath("$.items[0].details.after.runningSince")
                .value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].details.closure").doesNotExist())
        .andExpect(jsonPath("$.items[0].details.extension").doesNotExist());
  }

  @Test
  void s1_blockCancellationUsesPublicRevisionAndOriginalBefore() throws Exception {
    var receipt =
        new BlockChangeReceipt(id, id, "CANCELLED", 9007199254740993L, now, block(), null);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "BLOCK_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(
            jsonPath("$.items[0].details.revision").value("\"block:" + id + ":9007199254740993\""))
        .andExpect(jsonPath("$.items[0].details.before.objective").value("Objetivo original"))
        .andExpect(jsonPath("$.items[0].details.after").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].details.version").doesNotExist());
  }

  @Test
  void s1_originalBlockIsFlattenedWithoutRequestOrBudgetConsent() throws Exception {
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "BLOCK_PLANNED", now, project, "Entrega", task, "Publicar", block())),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(9))
        .andExpect(jsonPath("$.items[0].details.objective").value("Objetivo original"))
        .andExpect(jsonPath("$.items[0].details.durationMinutes").value(25))
        .andExpect(jsonPath("$.items[0].details.startAt").value("2026-09-08T10:00:00Z"))
        .andExpect(jsonPath("$.items[0].details.createdAt").value(now.toString()))
        .andExpect(jsonPath("$.items[0].details.request").doesNotExist());
  }

  @Test
  void s1_taskStatusDetailDoesNotExposeInternalTaskVersion() throws Exception {
    var id = java.util.UUID.fromString("11111111-1111-4111-8111-111111111111");
    var project = java.util.UUID.fromString("22222222-2222-4222-8222-222222222222");
    var task = java.util.UUID.fromString("33333333-3333-4333-8333-333333333333");
    var time = java.time.Instant.parse("2026-09-07T10:00:00.123456Z");
    var detail =
        new com.apptolast.organization.domain.TaskHistoryEntry(
            id, 99, "pending", "completed", time);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id,
                        "TASK_STATUS_CHANGED",
                        time,
                        project,
                        "Entrega",
                        task,
                        "Publicar",
                        detail)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
            {"items":[{"id":"11111111-1111-4111-8111-111111111111","type":"TASK_STATUS_CHANGED",
            "occurredAt":"2026-09-07T10:00:00.123456Z","projectId":"22222222-2222-4222-8222-222222222222",
            "projectName":"Entrega","taskId":"33333333-3333-4333-8333-333333333333","taskTitle":"Publicar",
            "details":{"id":"11111111-1111-4111-8111-111111111111","fromStatus":"pending","toStatus":"completed",
            "occurredAt":"2026-09-07T10:00:00.123456Z"}}],"nextCursor":null}
            """,
                    true));
  }

  @Test
  void s6_emptyHistoryPreservesClosedPageAndPrivateCachePolicy() throws Exception {
    var filters = new HistoryFilters(null, null, null, null, null);
    when(history.list("owner", filters, null)).thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("{\"items\":[],\"nextCursor\":null}", true))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(header().doesNotExist("Location"));
    verify(history).list("owner", filters, null);
    verifyNoMoreInteractions(history);
  }
}
