package com.apptolast.organization.adapter.connectors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Base de la API de GitHub (hallazgo B11 de {@code progress/security_review_connectors.md}). El PAT
 * del usuario viaja como {@code Authorization: Bearer} hacia esta base, así que sólo se admite la
 * base oficial o un host de loopback para las pruebas. Es configuración del servidor: se valida al
 * arrancar y nunca se toma de nada con alcance de petición.
 */
public final class GithubApiBase {
  private static final String PROPERTY = "app.github.api-base";
  private static final String OFFICIAL_HOST = "api.github.com";
  private static final String HTTPS = "https";
  private static final String HTTP = "http";
  private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");
  private static final String ISSUES_QUERY = "state=open&per_page=100&sort=created&direction=desc";

  private final String value;

  private GithubApiBase(String value) {
    this.value = value;
  }

  public static GithubApiBase of(String raw) {
    if (raw == null) throw rejected("no está definida");
    var normalized = withoutTrailingSlash(raw.trim());
    var uri = parse(normalized);
    if (!isOfficial(uri) && !isLoopback(uri)) throw rejected("no apunta a GitHub ni a loopback");
    if (hasExtraParts(uri)) throw rejected("no admite ruta, consulta, fragmento ni credenciales");
    return new GithubApiBase(normalized);
  }

  public String value() {
    return value;
  }

  public String user() {
    return value + "/user";
  }

  public String repository(String repository) {
    return value + "/repos/" + repository;
  }

  public String issues(String repository, int page) {
    return repository(repository) + "/issues?" + ISSUES_QUERY + "&page=" + page;
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
    return !uri.getRawPath().isEmpty()
        || uri.getRawQuery() != null
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
            + "; sólo se admite https://api.github.com o un loopback (127.0.0.1, localhost, ::1)");
  }
}
