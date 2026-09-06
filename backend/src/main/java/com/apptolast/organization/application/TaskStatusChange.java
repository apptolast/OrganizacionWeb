package com.apptolast.organization.application;

import com.apptolast.organization.domain.TaskSnapshot;

public record TaskStatusChange(TaskSnapshot snapshot, TaskStatusChanged event) {}
