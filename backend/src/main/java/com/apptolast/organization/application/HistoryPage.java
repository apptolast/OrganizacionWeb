package com.apptolast.organization.application;

import java.util.List;

public record HistoryPage(List<HistoryEntry<?>> items, HistoryCursor next) {}
