package com.apptolast.organization.adapter.connectors;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Base de la API de GitLab, hermana de {@link GithubApiBase} y con la misma política: el PAT del
 * usuario viaja hacia ella, así que sólo se admite la instancia oficial o un bucle local para las
 * pruebas, y se valida al arrancar. Nunca se toma de nada con alcance de petición.
 *
 * <p>A diferencia de GitHub, la base de GitLab lleva ruta —{@code /api/v4}— y se exige exactamente
 * esa: cualquier otra ruta, consulta, fragmento o credencial pegada a la URL la descarta.
 */
public final class GitlabApiBase {
  private static final String PROPERTY = "app.gitlab.api-base";
  private static final String OFFICIAL_HOST = "gitlab.com";
  private static final String VERSIONED_PATH = "/api/v4";
  private static final String HTTPS = "https";
  private static final String HTTP = "http";
  private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");
  private static final String ISSUES_QUERY = "state=opened&per_page=100";

  private final String value;

  private GitlabApiBase(String value) {
    this.value = value;
  }

  public static GitlabApiBase of(String raw) {
    if (raw == null) throw rejected("no está definida");
    var normalized = withoutTrailingSlash(raw.trim());
    var uri = parse(normalized);
    if (!isOfficial(uri) && !isLoopback(uri)) throw rejected("no apunta a GitLab ni a loopback");
    if (!VERSIONED_PATH.equals(uri.getRawPath())) throw rejected("no cuelga de " + VERSIONED_PATH);
    if (hasExtraParts(uri)) throw rejected("no admite consulta, fragmento ni credenciales");
    return new GitlabApiBase(normalized);
  }

  public String value() {
    return value;
  }

  /**
   * La ruta del proyecto va codificada entera, barras incluidas, que es como GitLab identifica un
   * proyecto por nombre. Codificarla es lo que impide que un valor con barras salte del recurso.
   */
  public String project(String projectPath) {
    return value + "/projects/" + encoded(projectPath);
  }

  public String issues(String projectReference, int page) {
    return value
        + "/projects/"
        + encoded(projectReference)
        + "/issues?"
        + ISSUES_QUERY
        + "&page="
        + page;
  }

  private static String encoded(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("*", "%2A");
  }

  private static boolean isOfficial(URI uri) {
    return HTTPS.equals(uri.getScheme())
        && OFFICIAL_HOST.equals(uri.getHost())
        && uri.getPort() == -1;
  }

  private static boolean isLoopback(URI uri) {
    return (HTTP.equals(uri.getScheme()) || HTTPS.equals(uri.getScheme()))
        && LOOPBACK_HOSTS.contains(String.valueOf(uri.getHost()));
  }

  private static boolean hasExtraParts(URI uri) {
    return uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || uri.getRawUserInfo() != null;
  }

  private static URI parse(String normalized) {
    try {
      return new URI(normalized);
    } catch (URISyntaxException error) {
      throw rejected("no es una URI absoluta");
    }
  }

  private static String withoutTrailingSlash(String text) {
    return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
  }

  private static IllegalArgumentException rejected(String reason) {
    return new IllegalArgumentException(
        PROPERTY
            + " "
            + reason
            + "; sólo se admite https://gitlab.com/api/v4 o un loopback (127.0.0.1, localhost,"
            + " ::1) bajo /api/v4");
  }
}
