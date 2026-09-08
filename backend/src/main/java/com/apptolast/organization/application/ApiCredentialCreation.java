package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;

public record ApiCredentialCreation(ApiCredential credential, String secret) {
  @Override
  public String toString() {
    return "ApiCredentialCreation[credential=" + credential + ", secret=REDACTED]";
  }
}
