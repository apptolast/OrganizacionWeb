package com.apptolast.organization.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ApiErrors;
import com.apptolast.organization.adapter.http.GitlabConnectorController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.IssueImportReceipt;
import com.apptolast.organization.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @s8 @s10 @s12 @s22 @s23 @s24 @s29 @s30 @s31 @s32 la frontera HTTP del conector de GitLab:
 *     cabeceras, validación estricta del cuerpo, códigos de problema y ausencia del token.
 */
@WebMvcTest(
    controllers = GitlabConnectorController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import({SecurityConfiguration.class, ApiErrors.class})
class GitlabConnectorApiTest {
  private static final String CONNECTION = "/api/v1/me/connectors/gitlab";
  private static final String IMPORTS = CONNECTION + "/imports";
  private static final String TOKEN = "glpat-SECRETOSECRETO1234";
  private static final String API_BASE = "https://gitlab.example.com/api/v4";
  private static final UUID PROJECT = UUID.fromString("11111111-2222-3333-4444-555555555555");
  private static final UUID IMPORT = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
  private static final Instant STARTED = Instant.parse("2026-09-09T12:00:00Z");

  private static final String[] CONNECTION_FIELDS = {
    "status",
    "apiBase",
    "projectPath",
    "projectId",
    "tokenHint",
    "lastActivityAt",
    "lastError",
    "version"
  };

  private static final String[] RECEIPT_FIELDS = {
    "id",
    "source",
    "projectId",
    "projectPath",
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
  @MockitoBean ReadGitlabConnectionUseCase read;
  @MockitoBean ConnectGitlabUseCase connect;
  @MockitoBean DisconnectGitlabUseCase disconnect;

  @MockitoBean(name = GitlabConnectorController.GITLAB_IMPORTS)
  ImportIssuesUseCase importIssues;

  @MockitoBean ReadIssueImportUseCase readImport;
  @MockitoBean AuthenticateApiCredentialUseCase authenticateCredential;
  @MockitoBean ConsumeApiQuotaUseCase quota;

  private Set<String> keysOf(String body, String... path) throws Exception {
    var node = mapper.readTree(body);
    for (var step : path) node = node.get(step);
    var keys = new java.util.LinkedHashSet<String>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  private static GitlabConnectionView connected() {
    return new GitlabConnectionView(
        "connected",
        API_BASE,
        "grupo/proyecto",
        4821L,
        "1234",
        Instant.parse("2026-09-09T10:00:00Z"),
        null,
        1L);
  }

  private static IssueImportReceipt completed() {
    return new IssueImportReceipt(
        IMPORT,
        "gitlab",
        PROJECT,
        "grupo/proyecto",
        "completed",
        2,
        0,
        0,
        false,
        null,
        STARTED,
        STARTED.plusSeconds(4));
  }

  private static String connectBody() {
    return "{\"token\":\"" + TOKEN + "\",\"projectPath\":\"grupo/proyecto\"}";
  }

  // ------------------------------------------------------------------------- @s8 leer

  @Test
  void s8_theConnectionAnswerHasExactlyEightFieldsAndNoTokenAnywhere() throws Exception {
    when(read.execute("owner")).thenReturn(connected());

    var body =
        mvc.perform(get(CONNECTION).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.status").value("connected"))
            .andExpect(jsonPath("$.apiBase").value(API_BASE))
            .andExpect(jsonPath("$.projectPath").value("grupo/proyecto"))
            .andExpect(jsonPath("$.projectId").value(4821))
            .andExpect(jsonPath("$.tokenHint").value("1234"))
            .andExpect(jsonPath("$.version").value(1))
            .andExpect(content().json("{\"lastError\":null}"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder(CONNECTION_FIELDS);
    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN);
  }

  @Test
  void s8_withoutConnectionTheAnswerIsNotConnectedAndNotAFourOhFour() throws Exception {
    when(read.execute("owner")).thenReturn(GitlabConnectionView.notConnected());

    var body =
        mvc.perform(get(CONNECTION).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("not_connected"))
            .andExpect(
                content()
                    .json(
                        "{\"apiBase\":null,\"projectPath\":null,\"projectId\":null,"
                            + "\"tokenHint\":null,\"lastActivityAt\":null,\"lastError\":null,"
                            + "\"version\":null}"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder(CONNECTION_FIELDS);
  }

  @Test
  void s5_theLastErrorIsExactlyACodeAndAnInstantWithNoFreeText() throws Exception {
    when(read.execute("owner"))
        .thenReturn(
            new GitlabConnectionView(
                "error",
                API_BASE,
                "grupo/proyecto",
                4821L,
                "1234",
                STARTED,
                new ConnectorError("CONNECTION_INVALID", STARTED),
                2L));

    var body =
        mvc.perform(get(CONNECTION).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastError.code").value("CONNECTION_INVALID"))
            .andExpect(jsonPath("$.lastError.at").value("2026-09-09T12:00:00Z"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(keysOf(body, "lastError"))
        .containsExactlyInAnyOrder("code", "at");
    org.assertj.core.api.Assertions.assertThat(body)
        .doesNotContain("glpat")
        .doesNotContain("1234x");
  }

  // ---------------------------------------------------------------------- @s9 @s10 conectar

  @Test
  void s9_connectingAnswersTheConnectionAndNeverEchoesTheToken() throws Exception {
    when(connect.execute("owner", TOKEN, "grupo/proyecto")).thenReturn(connected());

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
            .andExpect(jsonPath("$.tokenHint").value("1234"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN);
  }

  @ParameterizedTest
  @CsvSource({
    "'{\"projectPath\":\"grupo/proyecto\"}', token, REQUIRED",
    "'{\"token\":null,\"projectPath\":\"grupo/proyecto\"}', token, REQUIRED",
    "'{\"token\":7,\"projectPath\":\"grupo/proyecto\"}', token, INVALID_TYPE",
    "'{\"token\":\"glpat-x\"}', projectPath, REQUIRED",
    "'{\"token\":\"glpat-x\",\"projectPath\":7}', projectPath, INVALID_TYPE",
    "'{\"token\":\"glpat-x\",\"projectPath\":\"g/p\",\"apiBase\":\"https://gitlab.com/api/v4\"}',"
        + " apiBase, UNKNOWN_FIELD",
    "'{\"token\":\"glpat-x\",\"projectPath\":\"g/p\",\"extra\":1}', extra, UNKNOWN_FIELD"
  })
  void s10_abadBodyIsRefusedWithoutReachingTheUseCase(String body, String field, String code)
      throws Exception {
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
  void s10_thedomainRefusalOfTheProjectPathTravelsAsAValidationError() throws Exception {
    when(connect.execute(any(), any(), any()))
        .thenThrow(
            new ValidationException(
                List.of(new FieldError("projectPath", "INVALID_FORMAT", "Revisa la ruta."))));

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"token\":\"glpat-x\",\"projectPath\":\"soloproyecto\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("projectPath"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
  }

  // ----------------------------------------------------------------- @s12 @s22 @s23 errores

  @ParameterizedTest
  @CsvSource({
    "CONNECTION_INVALID, 409",
    "CONNECTORS_DISABLED, 503",
    "GITLAB_UNAVAILABLE, 503",
    "IMPORT_IN_PROGRESS, 409"
  })
  void s12_everyRefusalHasItsStableCodeAndStatus(String code, int status) throws Exception {
    when(connect.execute(any(), any(), any())).thenThrow(exceptionFor(code));

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value(code));
  }

  private static RuntimeException exceptionFor(String code) {
    return switch (code) {
      case "CONNECTION_INVALID" -> new ConnectionInvalidException();
      case "CONNECTORS_DISABLED" -> new ConnectorsDisabledException();
      case "GITLAB_UNAVAILABLE" -> new GitlabUnavailableException();
      default -> new IssueImportInProgressException();
    };
  }

  @Test
  void s12_anExhaustedQuotaAnswersRetryAfterInTheHeaderAndInTheBody() throws Exception {
    when(connect.execute(any(), any(), any())).thenThrow(new ConnectorRateLimitedException(20));

    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().is(503))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(20))
        .andExpect(header().string("Retry-After", "20"));
  }

  @Test
  void s22_importingWithoutConnectionIsAMissingConnection() throws Exception {
    when(importIssues.execute("owner", PROJECT)).thenThrow(new ConnectionNotFoundException());

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
  }

  @ParameterizedTest
  @CsvSource({
    "'{\"projectId\":\"no-uuid\"}'",
    "'{\"projectId\":\"" + "11111111-2222-3333-4444-555555555555" + "\",\"projectPath\":\"g/p\"}'"
  })
  void s22_abadImportBodyNeverReachesTheUseCase(String body) throws Exception {
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
  void s23_afailedImportCarriesItsReceiptIdentifierWithTheProblem() throws Exception {
    var failed =
        new IssueImportReceipt(
            IMPORT,
            "gitlab",
            PROJECT,
            "grupo/proyecto",
            "failed",
            0,
            0,
            0,
            false,
            "CONNECTION_INVALID",
            STARTED,
            STARTED.plusSeconds(2));
    when(importIssues.execute("owner", PROJECT))
        .thenThrow(new IssueImportFailedException(failed, 0));

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONNECTION_INVALID"))
        .andExpect(jsonPath("$.importId").value(IMPORT.toString()))
        .andExpect(jsonPath("$.created").value(0));
  }

  // ---------------------------------------------------------------- @s15 @s30 importaciones

  @Test
  void s15_afinishedImportAnswersCreatedWithItsLocationAndTwelveFields() throws Exception {
    when(importIssues.execute("owner", PROJECT)).thenReturn(completed());

    var body =
        mvc.perform(
                post(IMPORTS)
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{\"projectId\":\"" + PROJECT + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", IMPORTS + "/" + IMPORT))
            // La fila 185 del contrato, valor a valor: un conjunto de claves no distingue un campo
            // presente con su dato de un campo presente con null.
            .andExpect(jsonPath("$.id").value(IMPORT.toString()))
            .andExpect(jsonPath("$.source").value("gitlab"))
            .andExpect(jsonPath("$.projectId").value(PROJECT.toString()))
            .andExpect(jsonPath("$.projectPath").value("grupo/proyecto"))
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.skipped").value(0))
            .andExpect(jsonPath("$.failed").value(0))
            .andExpect(jsonPath("$.truncated").value(false))
            .andExpect(jsonPath("$.startedAt").value("2026-09-09T12:00:00Z"))
            .andExpect(jsonPath("$.finishedAt").value("2026-09-09T12:00:04Z"))
            .andExpect(content().json("{\"errorCode\":null}"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder(RECEIPT_FIELDS);
  }

  @ParameterizedTest
  @CsvSource({
    "no-uuid, 400, VALIDATION_ERROR",
    "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE, 400, VALIDATION_ERROR",
    "99999999-9999-4999-8999-999999999999, 404, IMPORT_NOT_FOUND"
  })
  void s30_amalformedOrUnknownReceiptIdentifierNeverLeaksWhatExists(
      String id, int status, String code) throws Exception {
    when(readImport.execute(any(), any())).thenThrow(new IssueImportNotFoundException());

    mvc.perform(get(IMPORTS + "/" + id).with(user("owner")))
        .andExpect(status().is(status))
        .andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void s30_areceiptOfTheOwnerComesBackWholeThroughItsIdentifier() throws Exception {
    when(readImport.execute("owner", IMPORT)).thenReturn(completed());

    var body =
        mvc.perform(get(IMPORTS + "/" + IMPORT).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(IMPORT.toString()))
            .andExpect(jsonPath("$.projectId").value(PROJECT.toString()))
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.created").value(2))
            .andExpect(jsonPath("$.startedAt").value("2026-09-09T12:00:00Z"))
            .andExpect(jsonPath("$.finishedAt").value("2026-09-09T12:00:04Z"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(keysOf(body))
        .containsExactlyInAnyOrder(RECEIPT_FIELDS);
  }

  /**
   * Contadores y banderas del recibo, fila a fila del contrato: @s16 (la cuarta fila del outline,
   * doscientas creadas y truncated true), @s17 (la excluida cuenta como omitida), @s21 (la del
   * título en blanco falla sola) y @s26 (la página 2 caída deja errorCode GITLAB_UNAVAILABLE). Sin
   * afirmar el valor, sustituir cualquiera de estos accesores por null, 0 o "" pasa inadvertido.
   */
  @ParameterizedTest
  @CsvSource(
      nullValues = "nulo",
      value = {
        "completed, 200, 0, 0, true, nulo",
        "completed, 2, 1, 0, false, nulo",
        "completed, 0, 0, 1, false, nulo",
        "failed, 100, 0, 0, false, GITLAB_UNAVAILABLE"
      })
  void s16_s17_s21_s26_everyCounterAndFlagOfTheReceiptTravelsWithItsValue(
      String status, int created, int skipped, int failed, boolean truncated, String errorCode)
      throws Exception {
    when(readImport.execute("owner", IMPORT))
        .thenReturn(
            new IssueImportReceipt(
                IMPORT,
                "gitlab",
                PROJECT,
                "grupo/proyecto",
                status,
                created,
                skipped,
                failed,
                truncated,
                errorCode,
                STARTED,
                STARTED.plusSeconds(4)));

    mvc.perform(get(IMPORTS + "/" + IMPORT).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.created").value(created))
        .andExpect(jsonPath("$.skipped").value(skipped))
        .andExpect(jsonPath("$.failed").value(failed))
        .andExpect(jsonPath("$.truncated").value(truncated))
        .andExpect(
            content()
                .json(
                    "{\"errorCode\":"
                        + (errorCode == null ? "null" : "\"" + errorCode + "\"")
                        + "}"));
  }

  // ------------------------------------------- @s12 los tres manejadores sin cobertura

  @ParameterizedTest
  @CsvSource({"'{'", "'no soy json'", "''"})
  void s12_abodyThatIsNotReadableJsonIsAMalformedBodyAndNotAValidationError(String raw)
      throws Exception {
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(raw))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

    verifyNoInteractions(importIssues);
  }

  /**
   * @s24: una importación que muere por cuota lleva el problema de RATE_LIMITED <em>y</em> los
   *     contadores parciales de su recibo, para que la pantalla no tenga que volver a preguntar.
   */
  @Test
  void s24_animportKilledByTheQuotaCarriesRetryAfterAndItsPartialCounters() throws Exception {
    var exhausted =
        new IssueImportReceipt(
            IMPORT,
            "gitlab",
            PROJECT,
            "grupo/proyecto",
            "failed",
            7,
            1,
            0,
            false,
            "RATE_LIMITED",
            STARTED,
            STARTED.plusSeconds(3));
    when(importIssues.execute("owner", PROJECT))
        .thenThrow(new IssueImportFailedException(exhausted, 60));

    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().is(503))
        .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(60))
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.importId").value(IMPORT.toString()))
        .andExpect(jsonPath("$.created").value(7))
        .andExpect(jsonPath("$.skipped").value(1))
        .andExpect(jsonPath("$.failed").value(0));
  }

  // ------------------------------------------------------------------- @s14 @s29 @s31 resto

  @Test
  void s14_disconnectingAnswersNoContentWithoutABody() throws Exception {
    mvc.perform(delete(CONNECTION).with(user("owner")).with(csrf().asHeader()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(disconnect).execute("owner");
  }

  @Test
  void s29_withoutTheConnectorKeyEveryGitlabRouteAnswersConnectorsDisabled() throws Exception {
    when(read.execute(any())).thenThrow(new ConnectorsDisabledException());
    doThrow(new ConnectorsDisabledException()).when(disconnect).execute(any());
    when(importIssues.execute(any(), any())).thenThrow(new ConnectorsDisabledException());

    mvc.perform(get(CONNECTION).with(user("owner")))
        .andExpect(status().is(503))
        .andExpect(jsonPath("$.code").value("CONNECTORS_DISABLED"));
    mvc.perform(delete(CONNECTION).with(user("owner")).with(csrf().asHeader()))
        .andExpect(status().is(503))
        .andExpect(jsonPath("$.code").value("CONNECTORS_DISABLED"));
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().is(503))
        .andExpect(jsonPath("$.code").value("CONNECTORS_DISABLED"));
  }

  /**
   * @s12: un texto cifrado que ninguna clave del llavero abre —tras rotar APP_CONNECTOR_KEY sin
   *     conservar la anterior— es un fallo de configuración del servidor con código estable, no un
   *     500 mudo. El gemelo de GitHub ya lo respondía; esta ruta lo dejaba caer en el catch-all.
   */
  @Test
  void s12_anUndecipherableStoredTokenIsAnExplicitProblemAndNotAGenericFailure() throws Exception {
    when(importIssues.execute(any(), any())).thenThrow(new SecretUndecipherableException());

    var body =
        mvc.perform(
                post(IMPORTS)
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{\"projectId\":\"" + PROJECT + "\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("CONNECTOR_KEY_MISMATCH"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body).doesNotContain(TOKEN).doesNotContain("glpat");
  }

  /**
   * El estado por sí solo no dice cuál de las ocho filas de @s31 se está midiendo: el código sí.
   */
  @Test
  void s31_withoutASessionNoGitlabRouteAnswersAnything() throws Exception {
    mvc.perform(get(CONNECTION))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    mvc.perform(get(IMPORTS + "/" + IMPORT))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

    verifyNoInteractions(read, connect, disconnect, importIssues, readImport);
  }

  /**
   * Las tres filas de CSRF_INVALID y la de UNTRUSTED_ORIGIN devuelven el mismo 403: sin afirmar el
   * código, cualquiera de las dos causas pasaría por la otra y la prueba seguiría verde.
   */
  @Test
  void s31_withoutACsrfTokenNothingIsWritten() throws Exception {
    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(delete(CONNECTION).with(user("owner")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(
            post(IMPORTS)
                .with(user("owner"))
                .contentType("application/json")
                .content("{\"projectId\":\"" + PROJECT + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

    verifyNoInteractions(connect, disconnect, importIssues);
  }

  @Test
  void s31_aforeignOriginIsRefusedEvenWithAValidCsrfToken() throws Exception {
    mvc.perform(
            put(CONNECTION)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://atacante.test")
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));

    verifyNoInteractions(connect);
  }

  @Test
  void s31_aqueryStringOnAWriteRouteIsRefusedBeforeReachingTheUseCase() throws Exception {
    mvc.perform(
            put(CONNECTION + "?projectPath=otro")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(connectBody()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(connect);
  }
}
