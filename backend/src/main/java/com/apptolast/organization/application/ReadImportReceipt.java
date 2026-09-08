package com.apptolast.organization.application;

import java.util.Optional;
import java.util.UUID;

public final class ReadImportReceipt implements ReadImportReceiptUseCase {
  private final ImportReceiptQueries queries;

  public ReadImportReceipt(ImportReceiptQueries queries) {
    this.queries = queries;
  }

  public Optional<ImportReceipt> find(String owner, UUID key) {
    return queries.find(owner, key);
  }
}
