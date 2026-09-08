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
    long taskCustomFieldValues) {
  public ImportCounts minus(ImportCounts other) {
    return new ImportCounts(
        projects - other.projects,
        tasks - other.tasks,
        taskStatusHistory - other.taskStatusHistory,
        availability - other.availability,
        plannedBlocks - other.plannedBlocks,
        blockProjections - other.blockProjections,
        blockChanges - other.blockChanges,
        workSessions - other.workSessions,
        workSessionIntervals - other.workSessionIntervals,
        workSessionChanges - other.workSessionChanges,
        appearance - other.appearance,
        customization - other.customization,
        projectCustomFieldValues - other.projectCustomFieldValues,
        taskCustomFieldValues - other.taskCustomFieldValues);
  }
}
