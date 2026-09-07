package com.apptolast.organization.application;

public record HistoryCursor(
    String owner, HistoryFilters filters, HistoryPosition upper, HistoryPosition after) {}
