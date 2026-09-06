package com.apptolast.organization.domain;

import java.time.LocalDate;

public record BudgetDay(
    LocalDate date,
    int budgetMinutes,
    long plannedSeconds,
    long requestedSeconds,
    long excessSeconds) {}
