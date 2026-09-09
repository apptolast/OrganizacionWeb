package com.apptolast.organization.application;

/**
 * Traduce el fallo del gestor externo al lenguaje de cada operación. Un token rechazado significa
 * cosas distintas según el momento: al conectar es un token que no sirve, y durante una importación
 * es una conexión que ha dejado de valer y hay que marcar como tal.
 */
final class ConnectorFailures {
  private ConnectorFailures() {}

  static RuntimeException whileConnecting(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED -> new GithubTokenRejectedException();
      case REPOSITORY_UNAVAILABLE -> new GithubRepositoryUnavailableException();
      case RATE_LIMITED -> new ConnectorRateLimitedException(error.retryAfterSeconds());
      case UNAVAILABLE -> new GithubUnavailableException();
    };
  }

  /** Código con el que la bitácora nombra un rechazo al conectar. */
  static String connectErrorCode(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED -> "GITHUB_TOKEN_REJECTED";
      case REPOSITORY_UNAVAILABLE -> "GITHUB_REPOSITORY_UNAVAILABLE";
      case RATE_LIMITED -> "RATE_LIMITED";
      case UNAVAILABLE -> "GITHUB_UNAVAILABLE";
    };
  }

  /**
   * GitLab no distingue entre «este token no vale» y «este proyecto no existe para este token»: sus
   * 401, 403 y 404 dicen lo mismo desde fuera, así que las tres se cuentan como conexión inválida.
   */
  static RuntimeException whileConnectingGitlab(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED, REPOSITORY_UNAVAILABLE -> new ConnectionInvalidException();
      case RATE_LIMITED -> new ConnectorRateLimitedException(error.retryAfterSeconds());
      case UNAVAILABLE -> new GitlabUnavailableException();
    };
  }

  /** Código estable con el que GitLab nombra un fallo, tanto en la bitácora como en el recibo. */
  static String gitlabErrorCode(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED, REPOSITORY_UNAVAILABLE -> "CONNECTION_INVALID";
      case RATE_LIMITED -> "RATE_LIMITED";
      case UNAVAILABLE -> "GITLAB_UNAVAILABLE";
    };
  }

  /** Código de recibo con el que se cierra una importación rota por el gestor externo. */
  static String importErrorCode(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED -> "CONNECTION_INVALID";
      case REPOSITORY_UNAVAILABLE -> "GITHUB_REPOSITORY_UNAVAILABLE";
      case RATE_LIMITED -> "RATE_LIMITED";
      case UNAVAILABLE -> "GITHUB_UNAVAILABLE";
    };
  }
}
