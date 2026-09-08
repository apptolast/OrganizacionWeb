package com.apptolast.organization.application;

public record ImportCounts(
    long projects,
    long tasks,
    long taskStatusHistory,
    long availability,
    long plannedBlocks,
    long blockProjections,
    long blockChanges,
    long workSessions,
    long workSessionIntervals,
    long workSessionChanges,
    long appearance,
    long customization,
    long projectCustomFieldValues,
    long taskCustomFieldValues) {}
