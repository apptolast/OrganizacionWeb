package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ImportDataController;
import com.apptolast.organization.application.ImportCounts;
import com.apptolast.organization.application.ImportDataUseCase;
import com.apptolast.organization.application.ImportPreview;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ImportDataController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class ImportDataApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean ImportDataUseCase imports;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "application/json; charset=ISO-8859-1,identity",
    "application/json,gzip"
  })
  void s29_mediaRejectionPrecedesUnsupportedQuery(String media, String encoding) throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .param("extra", "value")
                .contentType(media)
                .header("Content-Encoding", encoding)
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    verifyNoInteractions(imports);
  }

  @Test
  void s14_legacyNullRunningSinceAndUtf8IdentityArePreserved() throws Exception {
    var zeros = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    var inserted = new ImportCounts(1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0);
    var sessionId = java.util.UUID.fromString("fb621c69-d909-4721-b904-3d43eccd6b88");
    when(imports.preview(eq("owner"), any()))
        .thenReturn(
            new ImportPreview(
                "a".repeat(64),
                2,
                "owner",
                Instant.parse("2026-09-08T00:00:00Z"),
                inserted,
                inserted,
                zeros,
                List.of(new ImportPreview.RunningSession(sessionId, null))));
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("Application/JSON; charset=uTf-8")
                .header("Content-Encoding", "identity")
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.runningSessions[0].sessionId").value(sessionId.toString()))
        .andExpect(
            jsonPath("$.runningSessions[0].runningSince").value(org.hamcrest.Matchers.nullValue()));
    verify(imports).preview(eq("owner"), any());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"GET", "PUT", "PATCH", "DELETE", "OPTIONS"})
  void s29_otherMethodsNeverPrepareAPreview(String method) throws Exception {
    mvc.perform(
            request(
                    org.springframework.http.HttpMethod.valueOf(method),
                    "/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "POST"));
    verifyNoInteractions(imports);
  }

  @Test
  void s29_headDoesNotPrepareAPreview() throws Exception {
    mvc.perform(head("/api/v1/me/import/preview").with(user("owner")))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(content().string(""));
    verifyNoInteractions(imports);
  }

  @Test
  void s28_untrustedOriginIsRejectedBeforeImport() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://other.example")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(imports);
  }

  @Test
  void s28_csrfFailurePrecedesMediaAndTheImport() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .contentType("text/plain")
                .content("private"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    verifyNoInteractions(imports);
  }

  @Test
  void s28_anonymousRequestIsRejectedBeforeMediaOrImport() throws Exception {
    mvc.perform(post("/api/v1/me/import/preview").contentType("text/plain").content("private"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(imports);
  }

  @Test
  void s29_compressedBodyIsRejectedBeforeThePort() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Content-Encoding", "gzip")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    verifyNoInteractions(imports);
  }

  @Test
  void s29_nonUtf8CharsetIsRejectedBeforeReadingTheFile() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json; charset=ISO-8859-1")
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    verifyNoInteractions(imports);
  }

  @Test
  void s29_nonJsonMediaIsRejectedBeforeThePort() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(imports);
  }

  @Test
  void s29_queryIsRejectedBeforeReadingTheImport() throws Exception {
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .param("owner", "other-owner")
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("IMPORT_INVALID_REQUEST"));
    verifyNoInteractions(imports);
  }

  @Test
  void s16_exceededLimitIs413WithoutPretendingToHavePreparedAResponse() throws Exception {
    when(imports.preview(eq("owner"), any()))
        .thenThrow(new com.apptolast.organization.application.ImportTooLargeException());
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("IMPORT_TOO_LARGE"))
        .andExpect(jsonPath("$.counts").doesNotExist());
  }

  @Test
  void s8_invalidFileUsesTheImportProblemWithoutPrivateBodyDetails() throws Exception {
    when(imports.preview(eq("owner"), any()))
        .thenThrow(new com.apptolast.organization.application.ImportInvalidFileException());
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("private-invalid-file"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("IMPORT_INVALID_FILE"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private-invalid-file"))));
  }

  @Test
  void s14_runningSessionAndDistinctCountsRetainTheirPreparedValues() throws Exception {
    var counts = new ImportCounts(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
    var inserted = new ImportCounts(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
    var identical = new ImportCounts(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    var sessionId = java.util.UUID.fromString("fb621c69-d909-4721-b904-3d43eccd6b88");
    when(imports.preview(eq("other-owner"), any()))
        .thenReturn(
            new ImportPreview(
                "a".repeat(64),
                1234,
                "other-owner",
                Instant.parse("2026-09-08T01:02:03.123456Z"),
                counts,
                inserted,
                identical,
                List.of(
                    new ImportPreview.RunningSession(
                        sessionId, Instant.parse("2026-09-07T00:00:00Z")))));
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("other-owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.owner").value("other-owner"))
        .andExpect(jsonPath("$.byteLength").value(1234))
        .andExpect(jsonPath("$.exportedAt").value("2026-09-08T01:02:03.123456Z"))
        .andExpect(jsonPath("$.counts.taskCustomFieldValues").value(14))
        .andExpect(jsonPath("$.insertCounts.taskCustomFieldValues").value(1))
        .andExpect(jsonPath("$.identicalCounts.taskCustomFieldValues").value(13))
        .andExpect(jsonPath("$.runningSessions[0].sessionId").value(sessionId.toString()))
        .andExpect(
            jsonPath("$.runningSessions[0].runningSince").value("2026-09-07T00:00:00.000000Z"));
    verify(imports).preview(eq("other-owner"), any());
  }

  @Test
  void s18_emptyPreviewReturnsTheClosedProjectionFromTheAuthenticatedStream() throws Exception {
    var counts =
        """
        {"projects":0,"tasks":0,"taskStatusHistory":0,"availability":0,
        "plannedBlocks":0,"blockProjections":0,"blockChanges":0,"workSessions":0,
        "workSessionIntervals":0,"workSessionChanges":0,"appearance":0,"customization":0,
        "projectCustomFieldValues":0,"taskCustomFieldValues":0}
        """;
    var body =
        ("""
        {"format":"organizationweb-export","schemaVersion":1,"owner":"owner",
        "exportedAt":"2026-09-08T01:02:03.000000Z","counts":%s,"data":{
        "projects":[],"tasks":[],"taskStatusHistory":[],"availability":[],"plannedBlocks":[],
        "blockProjections":[],"blockChanges":[],"workSessions":[],"workSessionIntervals":[],
        "workSessionChanges":[],"appearance":[],"customization":[],
        "projectCustomFieldValues":[],"taskCustomFieldValues":[]}}
        """)
            .formatted(counts)
            .getBytes(StandardCharsets.UTF_8);
    var hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
    var zeros = new ImportCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    when(imports.preview(eq("owner"), any()))
        .thenAnswer(
            call -> {
              assertArrayEquals(body, ((InputStream) call.getArgument(1)).readAllBytes());
              return new ImportPreview(
                  hash,
                  body.length,
                  "owner",
                  Instant.parse("2026-09-08T01:02:03Z"),
                  zeros,
                  zeros,
                  zeros,
                  List.of());
            });
    mvc.perform(
            post("/api/v1/me/import/preview")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(
            content()
                .json(
                    ("""
          {"format":"organizationweb-import-preview","schemaVersion":1,"fileSha256":"%s",
          "byteLength":%d,"owner":"owner","exportedAt":"2026-09-08T01:02:03.000000Z",
          "counts":%s,"insertCounts":%s,"identicalCounts":%s,"runningSessions":[]}
          """)
                        .formatted(hash, body.length, counts, counts, counts),
                    true));
    verify(imports).preview(eq("owner"), any());
    verifyNoMoreInteractions(imports);
  }
}
