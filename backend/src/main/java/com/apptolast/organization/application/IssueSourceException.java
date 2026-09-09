package com.apptolast.organization.application;

/**
 * Fallo del gestor externo, ya clasificado por el adaptador. El mensaje jamás incorpora el token ni
 * el cuerpo de la respuesta: sólo la categoría, que es lo único que el caso de uso necesita.
 */
public final class IssueSourceException extends RuntimeException {
  /** Categorías que el adaptador sabe distinguir a partir de la respuesta del gestor. */
  public enum Reason {
    TOKEN_REJECTED,
    REPOSITORY_UNAVAILABLE,
    RATE_LIMITED,
    UNAVAILABLE
  }

  private static final int DEFAULT_RETRY_SECONDS = 60;

  /** No hubo respuesta: caída de red, plazo agotado o cuerpo ilegible. */
  private static final int NO_ANSWER = 0;

  private final Reason reason;
  private final int retryAfterSeconds;
  private final int providerStatus;

  private IssueSourceException(Reason reason, int retryAfterSeconds, int providerStatus) {
    super("El gestor externo respondió " + reason);
    this.reason = reason;
    this.retryAfterSeconds = retryAfterSeconds;
    this.providerStatus = providerStatus;
  }

  public static IssueSourceException tokenRejected() {
    return new IssueSourceException(Reason.TOKEN_REJECTED, 0, 401);
  }

  public static IssueSourceException repositoryUnavailable() {
    return new IssueSourceException(Reason.REPOSITORY_UNAVAILABLE, 0, 404);
  }

  public static IssueSourceException unavailable() {
    return new IssueSourceException(Reason.UNAVAILABLE, 0, NO_ANSWER);
  }

  public static IssueSourceException rateLimited(int retryAfterSeconds) {
    return new IssueSourceException(
        Reason.RATE_LIMITED,
        retryAfterSeconds < 1 ? DEFAULT_RETRY_SECONDS : retryAfterSeconds,
        429);
  }

  /** El mismo fallo, anotando qué código respondió de verdad el gestor, para la bitácora. */
  public IssueSourceException answeredWith(int providerStatus) {
    return new IssueSourceException(reason, retryAfterSeconds, providerStatus);
  }

  /** Código HTTP del gestor, o cero cuando ni siquiera llegó a contestar. */
  public int providerStatus() {
    return providerStatus;
  }

  public Reason reason() {
    return reason;
  }

  public int retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
