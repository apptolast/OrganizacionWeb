package com.apptolast.organization.application;

import java.io.InputStream;

public final class PreviewImportData implements ImportDataUseCase {
  private final ImportDataQueries queries;

  public PreviewImportData(ImportDataQueries queries) {
    this.queries = queries;
  }

  public ImportPreview preview(String owner, InputStream body) {
    return queries.preview(owner, body);
  }
}
