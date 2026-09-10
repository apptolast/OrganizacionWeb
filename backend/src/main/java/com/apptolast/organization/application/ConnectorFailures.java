package com.apptolast.organization.application;

/**
 * Traduce el fallo del gestor externo al lenguaje de cada operación. Un token rechazado significa
 * cosas distintas según el momento: al conectar es un token que no sirve, y durante una importación
 * es una conexión que ha dejado de valer y hay que marcar como tal.
 */
final class ConnectorFailures {
  private static final String RATE_LIMITED = "RATE_LIMITED";
  private static final String CONNECTION_INVALID = "CONNECTION_INVALID";

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
      case RATE_LIMITED -> RATE_LIMITED;
      case UNAVAILABLE -> "GITHUB_UNAVAILABLE";
    };
  }

  /** Código de recibo con el que se cierra una importación rota por el gestor externo. */
  static String importErrorCode(IssueSourceException error) {
    return switch (error.reason()) {
      case TOKEN_REJECTED -> CONNECTION_INVALID;
      case REPOSITORY_UNAVAILABLE -> "GITHUB_REPOSITORY_UNAVAILABLE";
      case RATE_LIMITED -> RATE_LIMITED;
      case UNAVAILABLE -> "GITHUB_UNAVAILABLE";
    };
  }
}
