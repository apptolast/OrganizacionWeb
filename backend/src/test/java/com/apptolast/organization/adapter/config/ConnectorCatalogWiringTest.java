package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.ApiCredentialPage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @s1 el orden de las seis filas lo fija el cableado y no el azar del contenedor; @s3 sin clave las
 *     cuatro que cifran salen deshabilitadas y a sus fuentes no se las llega a preguntar, que es lo
 *     que impide que el catálogo entero se caiga por un descifrado imposible.
 */
class ConnectorCatalogWiringTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
  private static final String LOOPBACK = "http://127.0.0.1:18098/api/v4";
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T09:00:00Z"), ZoneOffset.UTC);

  /** El orden del contrato, escrito una sola vez y comparado entero, nunca por posiciones. */
  private static final List<String> ORDER =
      List.of(
          "api_credentials", "webhooks", "ics_calendar", "github", "external_calendar", "gitlab");

  private final ConnectorConfiguration configuration = new ConnectorConfiguration();
  private final GitlabConnectionStore gitlabConnections = mock(GitlabConnectionStore.class);
  private final IssueImportReceiptStore receipts = mock(IssueImportReceiptStore.class);
  private final ApiCredentialQueries credentials = mock(ApiCredentialQueries.class);

  private ReadConnectorCatalogUseCase catalogWith(SecretCipher cipher) {
    when(credentials.list(anyString(), any())).thenReturn(new ApiCredentialPage(List.of(), null));
    return configuration.readConnectorCatalog(
        credentials,
        mock(WebhookEndpoints.class),
        mock(WebhookDeliveries.class),
        mock(CalendarFeedTokens.class),
        mock(ConnectorConnectionStore.class),
        new ExternalCalendarStatusSource(mock(ExternalCalendarStore.class)),
        configuration.readGitlabConnection(
            gitlabConnections, receipts, configuration.gitlabApiBase(LOOPBACK), cipher),
        receipts,
        cipher,
        CLOCK);
  }

  @Test
  void s1_thewiredCatalogAnswersTheSixRowsInTheOrderOfTheContract() {
    var catalog = catalogWith(configuration.secretCipher(KEY, null)).execute("owner");

    assertThat(catalog.connectors()).extracting(ConnectorRow::id).containsExactlyElementsOf(ORDER);
  }

  @Test
  void s1_withoutAnyIntegrationTheSixWiredRowsAreNotConnected() {
    var catalog = catalogWith(configuration.secretCipher(KEY, null)).execute("owner");

    assertThat(catalog.connectors())
        .allSatisfy(row -> assertThat(row.status()).isEqualTo("not_connected"));
  }

  @Test
  void s3_withoutAkeyTheFourEncryptingRowsAreDisabledAndTheGitlabReadIsNeverReached() {
    var catalog = catalogWith(configuration.secretCipher(null, null)).execute("owner");

    assertThat(catalog.connectors()).extracting(ConnectorRow::id).containsExactlyElementsOf(ORDER);
    for (var id : List.of("webhooks", "github", "external_calendar", "gitlab"))
      assertThat(catalog.row(id).status()).describedAs(id).isEqualTo("disabled");
    for (var id : List.of("api_credentials", "ics_calendar"))
      assertThat(catalog.row(id).status()).describedAs(id).isEqualTo("not_connected");
    verifyNoInteractions(gitlabConnections);
  }
}
