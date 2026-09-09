package com.apptolast.organization.adapter.config;

import com.apptolast.organization.adapter.connectors.AesGcmSecretCipher;
import com.apptolast.organization.adapter.connectors.ConnectorKeyRing;
import com.apptolast.organization.adapter.connectors.GithubApiBase;
import com.apptolast.organization.adapter.connectors.HttpGithubIssueSource;
import com.apptolast.organization.adapter.http.GithubConnectorController;
import com.apptolast.organization.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cableado del conector de GitHub. Dos políticas deliberadamente distintas para la configuración:
 *
 * <ul>
 *   <li><b>Clave ausente</b>: el conector se deshabilita y sus rutas responden 503. Un despliegue
 *       que todavía no usa conectores no tiene por qué configurar una clave para arrancar.
 *   <li><b>Clave presente y mal formada</b>: la aplicación no arranca. Aquí sí hay intención de
 *       usar conectores, y arrancar con una clave rota significaría escribir secretos que después
 *       no se pueden leer.
 * </ul>
 *
 * <p>La base de la API se valida también al construir el bean: es configuración del servidor y no
 * puede venir de nada con alcance de petición, porque el PAT del usuario viaja hacia ella.
 */
@Configuration
public class ConnectorConfiguration {
  @Bean
  public SecretCipher secretCipher(
      @Value("${app.connectors.key:}") String key,
      @Value("${app.connectors.key-previous:}") String previous) {
    return new AesGcmSecretCipher(
        ConnectorKeyRing.of(configured(key), configured(previous)), new SecureRandom());
  }

  @Bean
  public GithubApiBase githubApiBase(
      @Value("${app.github.api-base:https://api.github.com}") String base) {
    return GithubApiBase.of(base);
  }

  @Bean
  ConnectorAudit connectorAudit() {
    return new com.apptolast.organization.adapter.logging.Slf4jConnectorAudit();
  }

  @Bean
  HttpGithubIssueSource githubIssueSource(GithubApiBase base, ObjectMapper json, Clock clock) {
    return new HttpGithubIssueSource(base, json, clock);
  }

  @Bean
  ConnectGithubUseCase connectGithub(
      ConnectorConnectionStore connections,
      IssueImportReceiptStore receipts,
      HttpGithubIssueSource source,
      SecretCipher cipher,
      ConnectorAudit audit,
      Clock clock) {
    return new ConnectGithub(connections, receipts, source, cipher, audit, clock);
  }

  @Bean
  ReadGithubConnectionUseCase readGithubConnection(
      ConnectorConnectionStore connections, IssueImportReceiptStore receipts, SecretCipher cipher) {
    return new ReadGithubConnection(connections, receipts, cipher);
  }

  @Bean
  DisconnectGithubUseCase disconnectGithub(
      ConnectorConnectionStore connections, SecretCipher cipher) {
    return new DisconnectGithub(connections, cipher);
  }

  @Bean
  ReadIssueImportUseCase readIssueImport(IssueImportReceiptStore receipts, SecretCipher cipher) {
    return new ReadIssueImport(receipts, cipher);
  }

  @Bean(GithubConnectorController.GITHUB_IMPORTS)
  ImportIssuesUseCase githubImportIssues(
      ConnectorConnectionStore connections,
      IssueImportReceiptStore receipts,
      ProjectQueries projects,
      HttpGithubIssueSource source,
      ImportedTaskCommit commit,
      SecretCipher cipher,
      ConnectorAudit audit,
      Clock clock) {
    return new ImportIssues(
        new GithubIssueConnections(connections),
        receipts,
        projects,
        source,
        commit,
        cipher,
        audit,
        clock);
  }

  /** Una variable de entorno sin definir llega como cadena vacía: eso es ausencia, no error. */
  private static String configured(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
