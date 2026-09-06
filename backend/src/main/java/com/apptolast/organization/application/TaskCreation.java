package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;

public record TaskCreation(Task task, TaskCreationEvent event) {}
