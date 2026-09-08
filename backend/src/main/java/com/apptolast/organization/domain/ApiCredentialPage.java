package com.apptolast.organization.domain;

import java.util.List;

public record ApiCredentialPage(List<ApiCredential> items, String nextCursor) {
  public ApiCredentialPage {
    items = List.copyOf(items);
  }
}
