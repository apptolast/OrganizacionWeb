package com.apptolast.organization.domain;

/**
 * The outcome of one outgoing attempt, already classified. A transport failure carries no HTTP
 * status; a response outside 2xx carries its code and never its body.
 */
public record WebhookAttempt(Integer httpStatus, String errorClass, int latencyMs) {
  public static final String HTTP_ERROR = "HTTP_ERROR";
  public static final String REDIRECT = "REDIRECT";
  private static final int FIRST_SUCCESS = 200;
  private static final int LAST_SUCCESS = 299;
  private static final int FIRST_REDIRECT = 300;
  private static final int LAST_REDIRECT = 399;

  public WebhookAttempt {
    if (latencyMs < 0) throw new IllegalArgumentException("Latency cannot be negative");
  }

  /** Classifies a response that arrived: 2xx succeeds, 3xx is a refused redirect, else an error. */
  public static WebhookAttempt http(int status, int latencyMs) {
    return new WebhookAttempt(status, classify(status), latencyMs);
  }

  /** A failure with no response at all: TIMEOUT, CONNECTION, TLS, DNS or BLOCKED_ADDRESS. */
  public static WebhookAttempt transport(String errorClass, int latencyMs) {
    return new WebhookAttempt(null, errorClass, latencyMs);
  }

  public boolean succeeded() {
    return errorClass == null;
  }

  private static String classify(int status) {
    if (status >= FIRST_SUCCESS && status <= LAST_SUCCESS) return null;
    return status >= FIRST_REDIRECT && status <= LAST_REDIRECT ? REDIRECT : HTTP_ERROR;
  }
}
