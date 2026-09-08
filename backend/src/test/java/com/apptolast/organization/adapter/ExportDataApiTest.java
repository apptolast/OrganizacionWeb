package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ExportDataController;
import com.apptolast.organization.application.ExportDataUseCase;
import com.apptolast.organization.application.PreparedExport;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ExportDataController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class ExportDataApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean ExportDataUseCase exports;

  @Test
  void s21_queryFailurePrecedesBodyAndNegotiation() throws Exception {
    mvc.perform(
            get("/api/v1/me/export")
                .with(user("owner"))
                .param("format", "json")
                .content("body")
                .header("Accept", "text/csv"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_EXPORT_QUERY"))
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verifyNoInteractions(exports);
  }

  @Test
  void s33_conditionalHeadersDoNotReplaceThePreparedDocument() throws Exception {
    var prepared = mock(PreparedExport.class);
    var bytes = "{\"owner\":\"owner\"}".getBytes(StandardCharsets.UTF_8);
    when(prepared.filename()).thenReturn("organizationweb-export-v1-20260908T010203123456Z.json");
    when(prepared.contentLength()).thenReturn((long) bytes.length);
    doAnswer(
            call -> {
              ((OutputStream) call.getArgument(0)).write(bytes);
              return null;
            })
        .when(prepared)
        .writeTo(any());
    when(exports.prepare("owner")).thenReturn(prepared);
    mvc.perform(get("/api/v1/me/export").with(user("owner")).header("If-None-Match", "*"))
        .andExpect(status().isOk())
        .andExpect(content().bytes(bytes))
        .andExpect(header().doesNotExist("ETag"));
    verify(exports).prepare("owner");
  }

  @Test
  void s18_originGuardStillPrecedesUnsupportedMethodHandling() throws Exception {
    mvc.perform(
            post("/api/v1/me/export")
                .with(user("owner"))
                .with(csrf())
                .header("Origin", "https://other.example"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"));
    verifyNoInteractions(exports);
  }

  @Test
  void s18_csrfStillPrecedesUnsupportedMethodHandling() throws Exception {
    mvc.perform(post("/api/v1/me/export").with(user("owner")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"));
    verifyNoInteractions(exports);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
  void s18_authenticatedPostWithCsrfIsNotAnExportCommand(String method) throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), "/api/v1/me/export")
                .with(user("owner"))
                .with(csrf()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verifyNoInteractions(exports);
  }

  @Test
  void s17_preparationFailureDoesNotExposePrivateDetailsOrDownloadHeaders() throws Exception {
    when(exports.prepare("owner"))
        .thenThrow(
            new com.apptolast.organization.application.StorageUnavailableException(
                new IllegalStateException("private row")));
    mvc.perform(get("/api/v1/me/export").with(user("owner")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private row"))))
        .andExpect(header().doesNotExist("Content-Disposition"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"));
    verify(exports).prepare("owner");
  }

  @Test
  void s15_sizeFailureIsAProblemWithoutDownloadHeaders() throws Exception {
    when(exports.prepare("owner"))
        .thenThrow(new com.apptolast.organization.application.ExportTooLargeException());
    mvc.perform(get("/api/v1/me/export").with(user("owner")))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("EXPORT_TOO_LARGE"))
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(header().doesNotExist("Content-Disposition"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"));
    verify(exports).prepare("owner");
  }

  @Test
  void s19_encodingDoesNotAcceptMediaTypeParametersOtherThanQuality() throws Exception {
    mvc.perform(
            get("/api/v1/me/export")
                .with(user("owner"))
                .header("Accept-Encoding", "identity; charset=utf-8"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_EXPORT_REQUEST"));
    verifyNoInteractions(exports);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"gzip, identity;q=0", "*;q=0"})
  void s19_identityExclusionRejectsBeforePreparing(String encoding) throws Exception {
    mvc.perform(get("/api/v1/me/export").with(user("owner")).header("Accept-Encoding", encoding))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code").value("EXPORT_FORMAT_NOT_ACCEPTABLE"));
    verifyNoInteractions(exports);
  }

  @Test
  void s19_malformedAcceptIsABadRequestRatherThanANegotiationFailure() throws Exception {
    mvc.perform(get("/api/v1/me/export").with(user("owner")).header("Accept", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_EXPORT_REQUEST"));
    verifyNoInteractions(exports);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"application/json;q=0, */*;q=1", "text/csv"})
  void s19_specificJsonExclusionOverridesAcceptedWildcard(String accept) throws Exception {
    mvc.perform(get("/api/v1/me/export").with(user("owner")).header("Accept", accept))
        .andExpect(status().isNotAcceptable())
        .andExpect(jsonPath("$.code").value("EXPORT_FORMAT_NOT_ACCEPTABLE"));
    verifyNoInteractions(exports);
  }

  @Test
  void s18_headDoesNotPrepareADiscardedExport() throws Exception {
    mvc.perform(head("/api/v1/me/export").with(user("owner")))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET"))
        .andExpect(content().string(""))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verifyNoInteractions(exports);
  }

  @Test
  void s18_rejectsANonemptyBodyBeforePreparing() throws Exception {
    mvc.perform(get("/api/v1/me/export").with(user("owner")).content(" "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_EXPORT_REQUEST"));
    verifyNoInteractions(exports);
  }

  @Test
  void s18_authenticationPrecedesQueryAndKeepsPrivateHeaders() throws Exception {
    mvc.perform(get("/api/v1/me/export").param("ownerId", "other"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().doesNotExist("Content-Disposition"));
    verifyNoInteractions(exports);
  }

  @Test
  void s18_rejectsEvenAnEmptyQueryParameterBeforePreparing() throws Exception {
    mvc.perform(get("/api/v1/me/export").with(user("owner")).param("cursor", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_EXPORT_QUERY"));
    verifyNoInteractions(exports);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "application/json, identity",
    "application/json;charset=us-ascii, identity",
    "application/*, gzip",
    "'application/json;q=0.5, */*;q=0', '*;q=0, identity;q=0.5'"
  })
  void s20_returnsPreparedBytesAndExactPrivateDownloadHeaders(String accept, String encoding)
      throws Exception {
    var bytes =
        "{\"owner\":\"dueño\",\"version\":\"9007199254740993\"}".getBytes(StandardCharsets.UTF_8);
    var filename = "organizationweb-export-v1-20260908T010203123456Z.json";
    when(exports.prepare("owner"))
        .thenReturn(
            new PreparedExport() {
              public String filename() {
                return filename;
              }

              public long contentLength() {
                return bytes.length;
              }

              public void writeTo(OutputStream output) throws IOException {
                output.write(bytes);
              }
            });
    mvc.perform(
            get("/api/v1/me/export")
                .with(user("owner"))
                .content(new byte[0])
                .header("Accept", accept)
                .header("Accept-Encoding", encoding))
        .andExpect(status().isOk())
        .andExpect(content().bytes(bytes))
        .andExpect(header().string("Content-Type", "application/json; charset=utf-8"))
        .andExpect(header().string("Content-Length", Integer.toString(bytes.length)))
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"" + filename + "\""))
        .andExpect(header().string("Cache-Control", "no-store, private, no-transform"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().doesNotExist("Content-Encoding"))
        .andExpect(header().doesNotExist("ETag"));
    verify(exports).prepare("owner");
    verifyNoMoreInteractions(exports);
  }
}
