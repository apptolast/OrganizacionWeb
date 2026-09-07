package com.apptolast.organization.application;

public interface ReadHistoryUseCase {
  HistoryPage list(String owner, HistoryFilters filters, HistoryCursor cursor);
}
