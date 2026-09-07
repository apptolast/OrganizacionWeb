package com.apptolast.organization.domain;

public final class WorkSessionTransitionException extends RuntimeException {
  private final String code;

  public WorkSessionTransitionException(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
