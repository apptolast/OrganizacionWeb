package com.apptolast.organization.application;

import com.apptolast.organization.domain.ProjectSnapshot;

public record ProjectStatusChange(ProjectSnapshot snapshot, ProjectStatusChanged event) {}
