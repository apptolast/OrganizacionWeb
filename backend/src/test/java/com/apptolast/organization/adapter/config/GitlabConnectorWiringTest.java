package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.GitlabConnection;
import com.apptolast.organization.application.GitlabConnectionStore;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s11 @s29 el cableado del conector de GitLab: la base de la API se valida al construir el bean y
 *     los casos de uso reciben el valor configurado, nunca nada con alcance de petición.
 */
class GitlabConnectorWiringTest {
  private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
  private static final String LOOPBACK = "http://127.0.0.1:18098/api/v4";

  private final ConnectorConfiguration configuration = new ConnectorConfiguration();

  @Test
  void s11_theConfiguredApiBaseIsValidatedWhenTheBeanIsBuilt() {
    assertThat(configuration.gitlabApiBase("https://gitlab.com/api/v4").value())
        .isEqualTo("https://gitlab.com/api/v4");
    assertThat(configuration.gitlabApiBase(LOOPBACK).value()).isEqualTo(LOOPBACK);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "https://gitlab.example.com/api/v4",
        "http://gitlab.com/api/v4",
        "https://gitlab.com",
        ""
      })
  void s11_anApiBaseOutsideTheAllowedListStopsTheStartupNamingTheProperty(String value) {
    assertThatThrownBy(() -> configuration.gitlabApiBase(value))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("app.gitlab.api-base");
  }

  @Test
  void s8_theReadUseCaseAnswersTheApiBaseOfTheServerAndNotOneFromTheRow() {
    var view =
        configuration
            .readGitlabConnection(
                empty(),
                receipts(),
                configuration.gitlabApiBase(LOOPBACK),
                configuration.secretCipher(KEY, null))
            .execute("owner");

    assertThat(view.status()).isEqualTo("not_connected");
    assertThat(view.apiBase()).isNull();
  }

  @Test
  void s29_withoutAKeyTheGitlabUseCasesAreDisabledInsteadOfStoppingTheApplication() {
    var cipher = configuration.secretCipher(null, null);
    var base = configuration.gitlabApiBase(LOOPBACK);

    assertThatThrownBy(
            () ->
                configuration.readGitlabConnection(empty(), receipts(), base, cipher).execute("o"))
        .isInstanceOf(com.apptolast.organization.application.ConnectorsDisabledException.class);
    assertThatThrownBy(
            () ->
                configuration
                    .disconnectGitlab(empty(), receipts(), cipher, Clock.systemUTC())
                    .execute("o"))
        .isInstanceOf(com.apptolast.organization.application.ConnectorsDisabledException.class);
  }

  private static GitlabConnectionStore empty() {
    return new GitlabConnectionStore() {
      @Override
      public Optional<GitlabConnection> find(String ownerId) {
        return Optional.empty();
      }

      @Override
      public void save(String ownerId, GitlabConnection connection) {
        throw new UnsupportedOperationException("El cableado no escribe");
      }

      @Override
      public boolean delete(String ownerId) {
        return false;
      }
    };
  }

  private static com.apptolast.organization.application.IssueImportReceiptStore receipts() {
    return new com.apptolast.organization.application.IssueImportReceiptStore() {
      @Override
      public com.apptolast.organization.domain.IssueImportReceipt begin(
          String ownerId,
          java.util.UUID projectId,
          String source,
          String projectPath,
          java.time.Instant startedAt,
          java.time.Instant staleBefore) {
        throw new UnsupportedOperationException("El cableado no importa");
      }

      @Override
      public void progress(
          String ownerId, java.util.UUID importId, int created, int skipped, int failed) {
        throw new UnsupportedOperationException("El cableado no importa");
      }

      @Override
      public com.apptolast.organization.domain.IssueImportReceipt finish(
          String ownerId,
          java.util.UUID importId,
          String status,
          String errorCode,
          boolean truncated,
          java.time.Instant finishedAt) {
        throw new UnsupportedOperationException("El cableado no importa");
      }

      @Override
      public Optional<com.apptolast.organization.domain.IssueImportReceipt> find(
          String ownerId, java.util.UUID importId) {
        return Optional.empty();
      }

      @Override
      public Optional<com.apptolast.organization.domain.IssueImportReceipt> latest(
          String ownerId, String source) {
        return Optional.empty();
      }

      @Override
      public boolean importing(String ownerId, java.time.Instant staleBefore) {
        return false;
      }
    };
  }
}
