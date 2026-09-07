package com.apptolast.organization.application;

public final class ReadHistory implements ReadHistoryUseCase {
  private final HistoryQueries queries;

  public ReadHistory(HistoryQueries queries) {
    this.queries = queries;
  }

  public HistoryPage list(String owner, HistoryFilters filters, HistoryCursor cursor) {
    var rows = queries.list(owner, filters, cursor);
    var items = rows.stream().limit(20).toList();
    return new HistoryPage(
        items,
        rows.size() > 20
            ? new HistoryCursor(
                owner,
                filters,
                cursor == null ? position(items.getFirst()) : cursor.upper(),
                position(items.getLast()))
            : null);
  }

  private static HistoryPosition position(HistoryEntry<?> entry) {
    return new HistoryPosition(entry.occurredAt(), entry.type(), entry.id());
  }
}
