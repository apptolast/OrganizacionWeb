package com.apptolast.organization.adapter.connectors;

import com.apptolast.organization.application.GitlabProject;
import com.apptolast.organization.application.GitlabProjectDirectory;
import com.apptolast.organization.application.IssuePage;
import com.apptolast.organization.application.IssueSource;
import com.apptolast.organization.application.IssueSourceException;
import com.apptolast.organization.domain.ExternalIssue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador HTTP de GitLab. La base es configuración del servidor y ya viene validada, así que lo
 * único que depende del usuario es la ruta del proyecto, que llega comprobada y viaja codificada.
 *
 * <p>No sigue redirecciones: seguir una llevaría el PAT del usuario a un destino que elige GitLab.
 * El token viaja únicamente en la cabecera {@code PRIVATE-TOKEN}, nunca en la URL ni en el cuerpo.
 * Los plazos son cortos y explícitos, porque importar es interactivo y quien espera es una persona.
 */
public final class HttpGitlabIssueSource implements IssueSource, GitlabProjectDirectory {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);
  private static final int DEFAULT_RETRY_SECONDS = 60;
  private static final int MINIMUM_RETRY_SECONDS = 1;
  private static final String PLAIN_ISSUE = "issue";
  private static final String TOKEN_HEADER = "PRIVATE-TOKEN";

  private final GitlabApiBase base;
  private final ObjectMapper json;
  private final Clock clock;
  private final HttpClient client;

  public HttpGitlabIssueSource(GitlabApiBase base, ObjectMapper json, Clock clock) {
    this.base = base;
    this.json = json;
    this.clock = clock;
    this.client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
  }

  @Override
  public GitlabProject verify(String projectPath, String token) {
    var body = body(get(base.project(projectPath), token));
    if (!body.isObject()) throw IssueSourceException.unavailable();
    return new GitlabProject(integer(body, "id"), text(body, "path_with_namespace"));
  }

  @Override
  public IssuePage list(String projectReference, String token, int page) {
    var response = get(base.issues(projectReference, page), token);
    if (quotaExhausted(response)) throw IssueSourceException.rateLimited(retryAfter(response));
    var array = body(response);
    if (!array.isArray()) throw IssueSourceException.unavailable();
    var issues = new ArrayList<ExternalIssue>();
    int excluded = 0;
    for (var element : array) {
      if (!element.isObject()) throw IssueSourceException.unavailable();
      if (importable(element)) issues.add(issueOf(element));
      else excluded++;
    }
    return new IssuePage(List.copyOf(issues), array.size(), excluded, announcesNextPage(response));
  }

  /**
   * Sólo se importa lo que es trabajo planificable: incidentes, casos de prueba y las «tasks» de
   * GitLab no lo son, y una issue movida a otro proyecto ya vive en su destino.
   */
  private static boolean importable(JsonNode element) {
    var type = element.get("issue_type");
    var moved = element.get("moved_to_id");
    return type != null
        && PLAIN_ISSUE.equals(type.textValue())
        && (moved == null || moved.isNull());
  }

  private ExternalIssue issueOf(JsonNode element) {
    var description = element.get("description");
    return new ExternalIssue(
        externalId(element),
        text(element, "title"),
        description == null || description.isNull() ? null : description.asText(),
        text(element, "web_url"));
  }

  /**
   * El identificador global de la issue basta dentro de una instancia; el host delante evita que
   * cambiar de instancia haga colisionar dos issues distintas bajo el mismo enlace.
   */
  private String externalId(JsonNode element) {
    return URI.create(base.value()).getHost() + ":" + integer(element, "id");
  }

  private static long integer(JsonNode node, String field) {
    var value = node.get(field);
    if (value == null || !value.isIntegralNumber()) throw IssueSourceException.unavailable();
    return value.asLong();
  }

  private static String text(JsonNode node, String field) {
    var value = node.get(field);
    if (value == null || !value.isTextual()) throw IssueSourceException.unavailable();
    return value.textValue();
  }

  private HttpResponse<String> get(String url, String token) {
    var request =
        HttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(REQUEST_TIMEOUT)
            .header(TOKEN_HEADER, token)
            .header("Accept", "application/json")
            .header("User-Agent", "OrganizationWeb")
            .build();
    HttpResponse<String> response;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw IssueSourceException.unavailable();
    } catch (java.io.IOException error) {
      // El mensaje de la excepción de red puede llevar la URL, nunca la cabecera con el token.
      throw IssueSourceException.unavailable();
    }
    if (response.statusCode() != 200) throw classify(response);
    return response;
  }

  private JsonNode body(HttpResponse<String> response) {
    try {
      return json.readTree(response.body());
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw IssueSourceException.unavailable();
    }
  }

  /**
   * GitLab responde 401 con un token que no vale y 403 con uno que vale pero no alcanza; para el
   * producto son lo mismo, una conexión que hay que rehacer. El 404 se distingue porque describe el
   * proyecto y no la credencial, aunque acabe en el mismo código de la API.
   */
  private IssueSourceException classify(HttpResponse<String> response) {
    int status = response.statusCode();
    if (status == 401 || status == 403)
      return IssueSourceException.tokenRejected().answeredWith(status);
    if (status == 429) return IssueSourceException.rateLimited(retryAfter(response));
    if (status == 404) return IssueSourceException.repositoryUnavailable();
    return IssueSourceException.unavailable().answeredWith(status);
  }

  /** Una respuesta correcta con la cuota a cero es una negativa disfrazada de éxito. */
  private static boolean quotaExhausted(HttpResponse<String> response) {
    return header(response, "ratelimit-remaining").map("0"::equals).orElse(false);
  }

  private int retryAfter(HttpResponse<String> response) {
    var explicit = seconds(response, "retry-after");
    if (explicit.isPresent()) return atLeastOneSecond(explicit.get());
    var reset = seconds(response, "ratelimit-reset");
    if (reset.isEmpty()) return DEFAULT_RETRY_SECONDS;
    return atLeastOneSecond(reset.get() - clock.instant().getEpochSecond());
  }

  private static int atLeastOneSecond(long seconds) {
    return seconds < MINIMUM_RETRY_SECONDS
        ? MINIMUM_RETRY_SECONDS
        : (int) Math.min(seconds, Integer.MAX_VALUE);
  }

  private static Optional<Long> seconds(HttpResponse<String> response, String name) {
    return header(response, name)
        .flatMap(
            value -> {
              try {
                return Optional.of(Long.parseLong(value.trim()));
              } catch (NumberFormatException error) {
                return Optional.<Long>empty();
              }
            });
  }

  private static Optional<String> header(HttpResponse<String> response, String name) {
    return response.headers().firstValue(name);
  }

  /** GitLab anuncia la página siguiente en {@code X-Next-Page}, vacía cuando no hay más. */
  private static boolean announcesNextPage(HttpResponse<String> response) {
    return header(response, "x-next-page").map(value -> !value.isBlank()).orElse(false);
  }
}
