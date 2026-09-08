package com.apptolast.organization.application;

import com.apptolast.organization.domain.ApiCredential;

public record ApiCredentialIssuance(ApiCredential credential, String secret, byte[] verifier) {
  @Override
  public String toString() {
    return "ApiCredentialIssuance[credential="
        + credential
        + ", secret=REDACTED, verifier=REDACTED]";
  }
}
