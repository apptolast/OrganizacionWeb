package com.apptolast.organization.application;

/** Stable, non-field rejection of a webhook operation; the HTTP adapter maps each code. */
public final class WebhookOperationException extends RuntimeException {
  public enum Code {
    URL_BLOCKED,
    URL_UNRESOLVABLE,
    LIMIT,
    NOT_FOUND,
    DISABLED,
    DELIVERY_PENDING,
    CONNECTORS_DISABLED
  }

  private final Code code;

  public WebhookOperationException(Code code) {
    super(code.name());
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
