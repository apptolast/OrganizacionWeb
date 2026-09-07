package com.apptolast.organization.application;

public final class ReadHistory implements ReadHistoryUseCase {
  private final HistoryQueries queries;

  public ReadHistory(HistoryQueries queries) {
    this.queries = queries;
  }

  public HistoryPage list(String owner, HistoryFilters filters, HistoryCursor cursor) {
    return new HistoryPage(queries.list(owner, filters, cursor), null);
  }
}
