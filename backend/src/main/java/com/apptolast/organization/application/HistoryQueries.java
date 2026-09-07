package com.apptolast.organization.application;

import java.util.List;

public interface HistoryQueries {
  List<HistoryEntry<?>> list(String owner, HistoryFilters filters, HistoryCursor cursor);
}
