package com.apptolast.organization.application;

import java.time.LocalDate;
import java.util.UUID;

public record HistoryFilters(
    String category, UUID projectId, UUID taskId, LocalDate from, LocalDate to) {}
