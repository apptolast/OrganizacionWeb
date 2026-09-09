package com.apptolast.organization.adapter.connectors;

import com.apptolast.organization.application.IssuePage;
import com.apptolast.organization.application.IssueSource;
import com.apptolast.organization.application.IssueSourceException;
import com.apptolast.organization.application.RepositoryIdentity;
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

/**
 * Adaptador HTTP de GitHub. La base es configuración del servidor y ya viene validada, así que la
 * única parte que depende del usuario es el nombre del repositorio, que llega comprobado.
 *
 * <p>No sigue redirecciones: seguir una llevaría el PAT del usuario a un destino que GitHub elige.
 * Los plazos son cortos y explícitos, porque importar es una operación interactiva y quien espera
 * es una persona delante de la pantalla.
 */
public final class HttpGithubIssueSource implements IssueSource {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(4);
  private static final int DEFAULT_RETRY_SECONDS = 60;
  private static final int MINIMUM_RETRY_SECONDS = 1;
  private static final String PULL_REQUEST = "pull_request";

  private final GithubApiBase base;
  private final ObjectMapper json;
  private final Clock clock;
  private final HttpClient client;

  public HttpGithubIssueSource(GithubApiBase base, ObjectMapper json, Clock clock) {
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
  public RepositoryIdentity verify(String repository, String token) {
    var repositoryBody = body(get(base.repository(repository), token));
    var userBody = body(get(base.user(), token));
    return new RepositoryIdentity(text(repositoryBody, "full_name"), text(userBody, "login"));
  }

  @Override
  public IssuePage list(String repository, String token, int page) {
    var response = get(base.issues(repository, page), token);
    var array = body(response);
    if (!array.isArray()) throw IssueSourceException.unavailable();
    var issues = new ArrayList<ExternalIssue>();
    for (var element : array) {
      if (!element.isObject()) throw IssueSourceException.unavailable();
      if (element.has(PULL_REQUEST) && !element.get(PULL_REQUEST).isNull()) continue;
      issues.add(issueOf(element));
    }
    return new IssuePage(List.copyOf(issues), array.size(), announcesNextPage(response));
  }

  private static ExternalIssue issueOf(JsonNode element) {
    var id = element.get("id");
    if (id == null || !id.isIntegralNumber()) throw IssueSourceException.unavailable();
    var body = element.get("body");
    return new ExternalIssue(
        id.asText(),
        text(element, "title"),
        body == null || body.isNull() ? null : body.asText(),
        text(element, "html_url"));
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
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
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
   * Un 403 es cuota agotada cuando GitHub lo dice —sin peticiones restantes o con un plazo de
   * reintento—, y en cualquier otro caso significa que el repositorio no está a nuestro alcance.
   */
  private IssueSourceException classify(HttpResponse<String> response) {
    int status = response.statusCode();
    if (status == 401) return IssueSourceException.tokenRejected();
    if (status == 429) return IssueSourceException.rateLimited(retryAfter(response));
    if (status == 403)
      return (exhaustedQuota(response)
              ? IssueSourceException.rateLimited(retryAfter(response))
              : IssueSourceException.repositoryUnavailable())
          .answeredWith(status);
    if (status == 404) return IssueSourceException.repositoryUnavailable();
    return IssueSourceException.unavailable().answeredWith(status);
  }

  private static boolean exhaustedQuota(HttpResponse<String> response) {
    return header(response, "x-ratelimit-remaining").map("0"::equals).orElse(false)
        || header(response, "retry-after").isPresent();
  }

  /** Retry-After manda sobre el instante de reinicio; sin ninguno de los dos, un minuto. */
  private int retryAfter(HttpResponse<String> response) {
    var explicit = seconds(response, "retry-after");
    if (explicit.isPresent()) return atLeastOneSecond(explicit.get());
    var reset = seconds(response, "x-ratelimit-reset");
    if (reset.isEmpty()) return DEFAULT_RETRY_SECONDS;
    return atLeastOneSecond(reset.get() - clock.instant().getEpochSecond());
  }

  private static int atLeastOneSecond(long seconds) {
    return seconds < MINIMUM_RETRY_SECONDS
        ? MINIMUM_RETRY_SECONDS
        : (int) Math.min(seconds, Integer.MAX_VALUE);
  }

  private static java.util.Optional<Long> seconds(HttpResponse<String> response, String name) {
    return header(response, name)
        .flatMap(
            value -> {
              try {
                return java.util.Optional.of(Long.parseLong(value.trim()));
              } catch (NumberFormatException error) {
                return java.util.Optional.<Long>empty();
              }
            });
  }

  private static java.util.Optional<String> header(HttpResponse<String> response, String name) {
    return response.headers().firstValue(name);
  }

  private static boolean announcesNextPage(HttpResponse<String> response) {
    return header(response, "link").map(value -> value.contains("rel=\"next\"")).orElse(false);
  }
}
