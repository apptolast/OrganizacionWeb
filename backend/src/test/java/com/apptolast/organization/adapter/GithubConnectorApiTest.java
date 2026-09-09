package com.apptolast.organization.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.GithubConnectorController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.IssueImportReceipt;
import com.apptolast.organization.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @s1 @s3 @s5 @s6 @s10 @s11 @s12 @s20 @s23 @s29 @s30 @s31 @s32 @s34 @s35 la frontera HTTP del
 *     conector: cabeceras, validación estricta del cuerpo, códigos de problema y ausencia del
 *     token.
 */
@WebMvcTest(
    controllers = GithubConnectorController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class GithubConnectorApiTest {
  private static final String CONNECTION = "/api/v1/me/connectors/github";
  private static final String IMPORTS = CONNECTION + "/imports";
  private static final String TOKEN = "ghp_secreto123";
  private static final UUID PROJECT = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID IMPORT = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
  private static final Instant STARTED = Instant.parse("2026-09-09T12:00:00Z");

  private static final String[] RECEIPT_FIELDS = {
    "id",
    "projectId",
    "repository",
    "status",
    "created",
    "skipped",
    "failed",
    "truncated",
    "errorCode",
    "startedAt",
    "finishedAt"
  };

  @Autowired MockMvc mvc;
  @Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;
  @MockitoBean ReadGithubConnectionUseCase read;
  @MockitoBean ConnectGithubUseCase connect;
  @MockitoBean DisconnectGithubUseCase disconnect;
  @MockitoBean ImportGithubIssuesUseCase importIssues;
  @MockitoBean ReadIssueImportUseCase readImport;
  @MockitoBean AuthenticateApiCredentialUseCase authenticateCredential;
  @MockitoBean ConsumeApiQuotaUseCase quota;

  /** Claves realmente serializadas, incluidas las que valen null. */
  private java.util.Set<String> keysOf(String body, String... path) throws Exception {
    var node = mapper.readTree(body);
    for (var step : path) node = node.get(step);
    var keys = new java.util.LinkedHashSet<String>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  private static ConnectionView view(IssueImportReceipt lastImport) {
    return new ConnectionView(
        "octocat/Hello-World",
        "octocat",
        "valid",
        Instant.parse("2026-09-09T10:00:00Z"),
        lastImport);
  }

  private static IssueImportReceipt completed() {
    return new IssueImportReceipt(
        IMPORT,
        PROJECT,
        "octocat/Hello-World",
        "completed",
        3,
        1,
        0,
        false,
        null,
        STARTED,
        STARTED.plusSeconds(4));
  }

  private static IssueImportReceipt failedWith(String errorCode, int created) {
    return new IssueImportReceipt(
        IMPORT,
        PROJECT,
        "octocat/Hello-World",
        "failed",
        created,
        0,
        0,
        false,
        errorCode,
        STARTED,
        STARTED.plusSeconds(2));
  }

  private static String connectBody() {
    return "{\"repository\":\"octocat/Hello-World\",\"token\":\"" + TOKEN + "\"}";
  }

  // ------------------------------------------------------------------ @s1 @s10 conexión

  @Test
  void s1_theConnectionAnswerHasExactlyFiveFieldsAndNoTokenAnywhere() throws Exception {
    when(connect.execute("owner", "octocat/Hello-World", TOKEN)).thenReturn(view(null));

    var body =
        mvc.perform(
                put(CONNECTION)
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(connectBody()))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.repository").value("octocat/Hello-World"))
            .andExpect(jsonPath("$.login").value("octocat"))
            .andExpect(jsonPath("$.status").value("valid"))
            .andExpect(jsonPath("$.connectedAt").value("2026-09-09T10:00:00Z"))
            .andExpect(jsonPath("$.lastImport").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(content().json("{\"lastImport\":null}"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN);
    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder("repository", "login", "status", "connectedAt", "lastImport");
  }

  @Test
  void s10_theConnectionCarriesItsLastReceiptWithTheElevenFields() throws Exception {
    when(read.execute("owner")).thenReturn(view(completed()));

    mvc.perform(get(CONNECTION).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.lastImport.id").value(IMPORT.toString()))
        .andExpect(jsonPath("$.lastImport.projectId").value(PROJECT.toString()))
        .andExpect(jsonPath("$.lastImport.repository").value("octocat/Hello-World"))
        .andExpect(jsonPath("$.lastImport.status").value("completed"))
        .andExpect(jsonPath("$.lastImport.created").value(3))
        .andExpect(jsonPath("$.lastImport.skipped").value(1))
        .andExpect(jsonPath("$.lastImport.failed").value(0))
        .andExpect(jsonPath("$.lastImport.truncated").value(false))
        .andExpect(jsonPath("$.lastImport.errorCode").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.lastImport.startedAt").value("2026-09-09T12:00:00Z"))
        .andExpect(jsonPath("$.lastImport.finishedAt").value("2026-09-09T12:00:04Z"))
        .andReturn();

    var body =
        mvc.perform(get(CONNECTION).with(user("owner")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder("repository", "login", "status", "connectedAt", "lastImport");
    org.assertj.core.api.Assertions.assertThat(keysOf(body, "lastImport"))
        .containsExactlyInAnyOrder(RECEIPT_FIELDS);
  }

  @Test
  void s10_aRunningReceiptKeepsErrorCodeAndFinishedAtAsNull() throws Exception {
    var running =
        new IssueImportReceipt(
            IMPORT, PROJECT, "octocat/Hello-World", "running", 2, 0, 0, false, null, STARTED, null);
    when(read.execute("owner")).thenReturn(view(running));

    mvc.perform(get(CONNECTION).with(user("owner")))
        .andExpect(jsonPath("$.lastImport.status").value("running"))
        .andExpect(jsonPath("$.lastImport.errorCode").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.lastImport.finishedAt").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(content().json("{\"lastImport\":{\"errorCode\":null,\"finishedAt\":null}}"));
  }

  @Test
  void s10_withoutAConnectionTheAnswerIsConnectionNotFound() throws Exception {
    when(read.execute("owner")).thenThrow(new ConnectionNotFoundException());

    mvc.perform(get(CONNECTION).with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
  }

  @Test
  void s11_disconnectingAnswersTwoHundredFourWithoutABody() throws Exception {
    mvc.perform(delete(CONNECTION).with(user("owner")).with(csrf().asHeader()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(disconnect).execute("owner");
  }

  // --------------------------------------------------------------- @s5 @s6 cuerpo estricto

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"token\":\"ghp_x\"}                                      | repository | REQUIRED",
        "{\"repository\":null,\"token\":\"ghp_x\"}                  | repository | REQUIRED",
        "{\"repository\":7,\"token\":\"ghp_x\"}                     | repository | INVALID_TYPE",
        "{\"repository\":\"octocat/x\"}                             | token      | REQUIRED",
        "{\"repository\":\"octocat/x\",\"token\":null}              | token      | REQUIRED",
        "{\"repository\":\"octocat/x\",\"token\":7}                 | token      | INVALID_TYPE",
        "{\"repository\":\"octocat/x\",\"token\":\"a\",\"apiBase\":\"https://atacante.test\"} | apiBase | UNKNOWN_FIELD"
      })
  void s5_s6_s35_theBodyIsRejectedFieldByFieldWithoutTouchingTheUseCase(
      String body, String field, String code) throws Exception {
    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));

    verifyNoInteractions(connect);
  }

  @Test
  void s6_aQueryStringOnTheConnectionIsRejectedWithoutNamingAField() throws Exception {
    mvc.perform(
            put(CONNECTION + "?repository=x")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasSize(0)));

    verifyNoInteractions(connect);
  }

  @Test
  void s6_truncatedJsonIsMalformedAndAnotherMediaTypeIsUnsupported() throws Exception {
    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"repository\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content(connectBody()))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

    verifyNoInteractions(connect);
  }

  @Test
  void s5_theValidationOfTheUseCaseSurfacesAsAFieldError() throws Exception {
    when(connect.execute(any(), any(), any()))
        .thenThrow(
            new ValidationException(
                List.of(new FieldError("repository", "INVALID_FORMAT", "Revisa el repositorio."))));

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("repository"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
  }

  // -------------------------------------------------------- @s7 @s8 @s21 errores al conectar

  @Test
  void s7_aRejectedTokenIsAConflictWhoseBodyNeverEchoesTheToken() throws Exception {
    when(connect.execute(any(), any(), any())).thenThrow(new GithubTokenRejectedException());

    var body =
        mvc.perform(
                put(CONNECTION)
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(connectBody()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("GITHUB_TOKEN_REJECTED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN);
  }

  @Test
  void s8_anUnavailableRepositoryIsAConflict() throws Exception {
    when(connect.execute(any(), any(), any()))
        .thenThrow(new GithubRepositoryUnavailableException());

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("GITHUB_REPOSITORY_UNAVAILABLE"));
  }

  @Test
  void s21_anExhaustedQuotaCarriesTheDelayInTheBodyAndInTheHeader() throws Exception {
    when(connect.execute(any(), any(), any())).thenThrow(new ConnectorRateLimitedException(45));

    var body =
        mvc.perform(
                put(CONNECTION)
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(connectBody()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.retryAfterSeconds").value(45))
            .andExpect(header().string("Retry-After", "45"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN);
  }

  // ---------------------------------------------------------------------- @s12 importar

  @Test
  void s12_aFinishedImportAnswersCreatedWithItsLocationAndElevenFields() throws Exception {
    when(importIssues.execute("owner", PROJECT)).thenReturn(completed());

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", IMPORTS + "/" + IMPORT))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.id").value(IMPORT.toString()))
        .andExpect(jsonPath("$.status").value("completed"))
        .andExpect(jsonPath("$.created").value(3))
        .andExpect(jsonPath("$.truncated").value(false))
        .andExpect(
            result ->
                org.assertj.core.api.Assertions.assertThat(
                        keysOf(result.getResponse().getContentAsString()))
                    .containsExactlyInAnyOrder(RECEIPT_FIELDS));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "{\"projectId\":null}",
        "{\"projectId\":\"no-es-uuid\"}",
        "{\"projectId\":\"11111111-2222-3333-4444-555555555555\",\"extra\":1}",
        "{\"projectId\":7}"
      })
  void s23_s29_anInvalidBodyIsRejectedBeforeTheUseCase(String body) throws Exception {
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(importIssues);
  }

  @Test
  void s23_anUppercaseUuidIsNotAValidProjectId() throws Exception {
    var withLetters = "1111aaaa-2222-bbbb-3333-cccccccccccc";
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    "{\"projectId\":\"" + withLetters.toUpperCase(java.util.Locale.ROOT) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("projectId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));

    verifyNoInteractions(importIssues);
  }

  @Test
  void s23_anUnknownAndAForeignProjectShareTheSameProblem() throws Exception {
    when(importIssues.execute(any(), any())).thenThrow(new ResourceNotFoundException());

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  // ------------------------------------------------- @s13 @s19 @s20 @s22 @s24 errores al importar

  @ParameterizedTest
  @CsvSource({
    "GITHUB_UNAVAILABLE,503",
    "STORAGE_UNAVAILABLE,503",
    "CONNECTION_INVALID,409",
    "GITHUB_REPOSITORY_UNAVAILABLE,409",
    "PROJECT_COMPLETED,409"
  })
  void s13_s19_s22_s24_aFailedImportKeepsItsIdentifierAndItsPartialCounters(
      String errorCode, int status) throws Exception {
    when(importIssues.execute(any(), any()))
        .thenThrow(new IssueImportFailedException(failedWith(errorCode, 2), 0));

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.code").value(errorCode))
        .andExpect(jsonPath("$.importId").value(IMPORT.toString()))
        .andExpect(jsonPath("$.created").value(2))
        .andExpect(header().doesNotExist("Retry-After"));
  }

  @Test
  void s20_anExhaustedQuotaWhileImportingCarriesTheDelayAndTheIdentifier() throws Exception {
    when(importIssues.execute(any(), any()))
        .thenThrow(new IssueImportFailedException(failedWith("RATE_LIMITED", 0), 120));

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(120))
        .andExpect(jsonPath("$.importId").value(IMPORT.toString()))
        .andExpect(header().string("Retry-After", "120"));
  }

  @Test
  void s25_anImportAlreadyRunningIsAConflictWithoutAnIdentifier() throws Exception {
    when(importIssues.execute(any(), any())).thenThrow(new IssueImportInProgressException());

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IMPORT_IN_PROGRESS"))
        .andExpect(jsonPath("$.importId").doesNotExist());
  }

  @Test
  void s29_anInvalidConnectionIsAConflictAndACompletedProjectToo() throws Exception {
    when(importIssues.execute(any(), any())).thenThrow(new ConnectionInvalidException());
    mvc.perform(importRequest())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONNECTION_INVALID"));

    reset(importIssues);
    when(importIssues.execute(any(), any())).thenThrow(new ProjectCompletedException());
    mvc.perform(importRequest())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PROJECT_COMPLETED"));

    reset(importIssues);
    when(importIssues.execute(any(), any())).thenThrow(new ConnectionNotFoundException());
    mvc.perform(importRequest())
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
  }

  private org.springframework.test.web.servlet.RequestBuilder importRequest() {
    return post(IMPORTS)
        .with(user("owner"))
        .with(csrf().asHeader())
        .contentType("application/json")
        .content("{\"projectId\":\"" + PROJECT + "\"}");
  }

  // ------------------------------------------------------------------------ @s30 recibos

  @Test
  void s30_theOwnReceiptComesBackWithItsElevenFields() throws Exception {
    when(readImport.execute("owner", IMPORT)).thenReturn(completed());

    mvc.perform(get(IMPORTS + "/" + IMPORT).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.id").value(IMPORT.toString()))
        .andExpect(
            result ->
                org.assertj.core.api.Assertions.assertThat(
                        keysOf(result.getResponse().getContentAsString()))
                    .containsExactlyInAnyOrder(RECEIPT_FIELDS));
  }

  @ParameterizedTest
  @ValueSource(strings = {"no-es-uuid", "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE", "1"})
  void s30_anIdentifierThatIsNotAUuidIsAlsoAMissingReceipt(String id) throws Exception {
    mvc.perform(get(IMPORTS + "/" + id).with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMPORT_NOT_FOUND"));

    verifyNoInteractions(readImport);
  }

  @Test
  void s30_aForeignReceiptIsIndistinguishableFromAMissingOne() throws Exception {
    when(readImport.execute(any(), any())).thenThrow(new IssueImportNotFoundException());

    mvc.perform(get(IMPORTS + "/" + IMPORT).with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("IMPORT_NOT_FOUND"));
  }

  // ---------------------------------------------------------------- @s3 conector deshabilitado

  @Test
  void s3_everyConnectorRouteAnswersServiceUnavailableWithoutTheKey() throws Exception {
    when(read.execute(any())).thenThrow(new ConnectorsDisabledException());
    when(connect.execute(any(), any(), any())).thenThrow(new ConnectorsDisabledException());
    doThrow(new ConnectorsDisabledException()).when(disconnect).execute(any());
    when(importIssues.execute(any(), any())).thenThrow(new ConnectorsDisabledException());
    when(readImport.execute(any(), any())).thenThrow(new ConnectorsDisabledException());

    for (var request :
        List.of(
            get(CONNECTION).with(user("owner")),
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()),
            delete(CONNECTION).with(user("owner")).with(csrf().asHeader()),
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"),
            get(IMPORTS + "/" + IMPORT).with(user("owner")))) {
      mvc.perform(request)
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("CONNECTORS_DISABLED"))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }
  }

  // ------------------------------------------------------------------ @s31 @s32 seguridad

  @Test
  void s31_withoutASessionEveryConnectorRouteIsUnauthenticated() throws Exception {
    mvc.perform(get(CONNECTION)).andExpect(status().isUnauthorized());
    mvc.perform(get(IMPORTS + "/" + IMPORT)).andExpect(status().isUnauthorized());
    mvc.perform(delete(CONNECTION).with(csrf().asHeader())).andExpect(status().isUnauthorized());
    mvc.perform(
            put(CONNECTION)
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(read, connect, disconnect, importIssues, readImport);
  }

  /**
   * Desvío documentado respecto a @s31: el filtro Bearer de la feature 24 responde 403
   * API_SCOPE_DENIED, no 401, cuando la ruta no está en su lista blanca. La propiedad que @s31
   * persigue —que una credencial de integraciones no abra el conector ni escriba nada— sí se
   * cumple, y la lista blanca es de otro carril. Ver progress/tdd_github_connector.md.
   */
  @Test
  void s31_aBearerCredentialOfTheIntegrationChannelDoesNotOpenTheConnector() throws Exception {
    when(authenticateCredential.authenticate("una-credencial-valida"))
        .thenReturn(
            new ApiCredentialAccess(
                UUID.randomUUID(),
                "owner",
                List.of("projects:read", "projects:write", "tasks:read", "tasks:write")));

    for (var request :
        List.of(
            put(CONNECTION)
                .header("Authorization", "Bearer una-credencial-valida")
                .contentType("application/json")
                .content(connectBody()),
            post(IMPORTS)
                .header("Authorization", "Bearer una-credencial-valida")
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))) {
      mvc.perform(request)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("API_SCOPE_DENIED"));
    }

    verifyNoInteractions(connect, importIssues, quota);
  }

  @Test
  void s32_writingWithoutACsrfTokenIsForbidden() throws Exception {
    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isForbidden());
    mvc.perform(delete(CONNECTION).with(user("owner"))).andExpect(status().isForbidden());
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isForbidden());

    verifyNoInteractions(connect, disconnect, importIssues);
  }
}
